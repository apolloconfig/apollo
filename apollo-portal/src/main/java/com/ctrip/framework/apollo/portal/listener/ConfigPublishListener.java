package com.ctrip.framework.apollo.portal.listener;

import com.ctrip.framework.apollo.common.constants.ReleaseOperation;
import com.ctrip.framework.apollo.portal.component.ConfigReleaseWebhookNotifier;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.component.emailbuilder.GrayPublishEmailBuilder;
import com.ctrip.framework.apollo.portal.component.emailbuilder.MergeEmailBuilder;
import com.ctrip.framework.apollo.portal.component.emailbuilder.NormalPublishEmailBuilder;
import com.ctrip.framework.apollo.portal.component.emailbuilder.RollbackEmailBuilder;
import com.ctrip.framework.apollo.portal.entity.bo.Email;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import com.ctrip.framework.apollo.portal.spi.EmailService;
import com.ctrip.framework.apollo.portal.spi.MQService;
import com.ctrip.framework.apollo.tracer.Tracer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ConfigPublishListener {

  private final ReleaseHistoryService releaseHistoryService;
  private final EmailService emailService;
  private final NormalPublishEmailBuilder normalPublishEmailBuilder;
  private final GrayPublishEmailBuilder grayPublishEmailBuilder;
  private final RollbackEmailBuilder rollbackEmailBuilder;
  private final MergeEmailBuilder mergeEmailBuilder;
  private final PortalConfig portalConfig;
  private final MQService mqService;
  private final ConfigReleaseWebhookNotifier configReleaseWebhookNotifier;

  private ExecutorService executorService;

  public ConfigPublishListener(
      final ReleaseHistoryService releaseHistoryService,
      final EmailService emailService,
      final NormalPublishEmailBuilder normalPublishEmailBuilder,
      final GrayPublishEmailBuilder grayPublishEmailBuilder,
      final RollbackEmailBuilder rollbackEmailBuilder,
      final MergeEmailBuilder mergeEmailBuilder,
      final PortalConfig portalConfig,
      final MQService mqService,
      final ConfigReleaseWebhookNotifier configReleaseWebhookNotifier) {
    this.releaseHistoryService = releaseHistoryService;
    this.emailService = emailService;
    this.normalPublishEmailBuilder = normalPublishEmailBuilder;
    this.grayPublishEmailBuilder = grayPublishEmailBuilder;
    this.rollbackEmailBuilder = rollbackEmailBuilder;
    this.mergeEmailBuilder = mergeEmailBuilder;
    this.portalConfig = portalConfig;
    this.mqService = mqService;
    this.configReleaseWebhookNotifier = configReleaseWebhookNotifier;
  }

  @PostConstruct
  public void init() {
    executorService = Executors.newSingleThreadExecutor(ApolloThreadFactory.create("ConfigPublishNotify", true));
  }

  @EventListener
  public void onConfigPublish(ConfigPublishEvent event) {
    executorService.submit(new ConfigPublishNotifyTask(event.getConfigPublishInfo()));
  }


  private class ConfigPublishNotifyTask implements Runnable {

    private ConfigPublishEvent.ConfigPublishInfo publishInfo;

    ConfigPublishNotifyTask(ConfigPublishEvent.ConfigPublishInfo publishInfo) {
      this.publishInfo = publishInfo;
    }

    @Override
    public void run() {
      ReleaseHistoryBO releaseHistory = getReleaseHistory();
      if (releaseHistory == null) {
        Tracer.logError("Load release history failed", null);
        return;
      }

      this.sendPublishWebHook(releaseHistory);

      sendPublishEmail(releaseHistory);

      sendPublishMsg(releaseHistory);
    }

    private ReleaseHistoryBO getReleaseHistory() {
      Env env = publishInfo.getEnv();

      int operation = publishInfo.isMergeEvent() ? ReleaseOperation.GRAY_RELEASE_MERGE_TO_MASTER :
                      publishInfo.isRollbackEvent() ? ReleaseOperation.ROLLBACK :
                      publishInfo.isNormalPublishEvent() ? ReleaseOperation.NORMAL_RELEASE :
                      publishInfo.isGrayPublishEvent() ? ReleaseOperation.GRAY_RELEASE : -1;

      if (operation == -1) {
        return null;
      }

      if (publishInfo.isRollbackEvent()) {
        return releaseHistoryService
            .findLatestByPreviousReleaseIdAndOperation(env, publishInfo.getPreviousReleaseId(), operation);
      }
      return releaseHistoryService.findLatestByReleaseIdAndOperation(env, publishInfo.getReleaseId(), operation);

    }

    /**
    * webhook send
    *
    * @param releaseHistory
    */
    private void sendPublishWebHook(ReleaseHistoryBO releaseHistory) {
      Env env = publishInfo.getEnv();

      String[] webHookUrls = portalConfig.webHookUrls();
      if (!portalConfig.webHookSupportedEnvs().contains(env) || webHookUrls == null) {
        return;
      }

      configReleaseWebhookNotifier.notify(webHookUrls, env, releaseHistory);
    }

    private void sendPublishEmail(ReleaseHistoryBO releaseHistory) {
      Env env = publishInfo.getEnv();

      if (!portalConfig.emailSupportedEnvs().contains(env)) {
        return;
      }

      int realOperation = releaseHistory.getOperation();

      Email email = null;
      try {
        email = buildEmail(env, releaseHistory, realOperation);
      } catch (Throwable e) {
        Tracer.logError("build email failed.", e);
      }

      if (email != null) {
        emailService.send(email);
      }
    }

    private void sendPublishMsg(ReleaseHistoryBO releaseHistory) {
      mqService.sendPublishMsg(publishInfo.getEnv(), releaseHistory);
    }

    private Email buildEmail(Env env, ReleaseHistoryBO releaseHistory, int operation) {
      switch (operation) {
        case ReleaseOperation.GRAY_RELEASE: {
          return grayPublishEmailBuilder.build(env, releaseHistory);
        }
        case ReleaseOperation.NORMAL_RELEASE: {
          return normalPublishEmailBuilder.build(env, releaseHistory);
        }
        case ReleaseOperation.ROLLBACK: {
          return rollbackEmailBuilder.build(env, releaseHistory);
        }
        case ReleaseOperation.GRAY_RELEASE_MERGE_TO_MASTER: {
          return mergeEmailBuilder.build(env, releaseHistory);
        }
        default:
          return null;
      }
    }
  }

}
