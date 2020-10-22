package com.ctrip.framework.apollo.portal.component;


import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class PortalSettings {

  private static final Logger logger = LoggerFactory.getLogger(PortalSettings.class);
  private static final int HEALTH_CHECK_INTERVAL = 10 * 1000;

  private final ApplicationContext applicationContext;
  private final PortalConfig portalConfig;
  private final PortalMetaDomainService portalMetaDomainService;

  private List<Env> allEnvs = new ArrayList<>();

  //mark env up or down
  private Map<Env, Boolean> envStatusMark = new ConcurrentHashMap<>();

  public PortalSettings(
          final ApplicationContext applicationContext,
          final PortalConfig portalConfig,
          final PortalMetaDomainService portalMetaDomainService
  ) {
    this.applicationContext = applicationContext;
    this.portalConfig = portalConfig;
    this.portalMetaDomainService = portalMetaDomainService;
  }

  @PostConstruct
  private void postConstruct() {

    allEnvs = portalConfig.portalSupportedEnvs();

    for (Env env : allEnvs) {
      envStatusMark.put(env, true);
    }

    ScheduledExecutorService
        healthCheckService =
        Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("EnvHealthChecker", true));

    healthCheckService
        .scheduleWithFixedDelay(new HealthCheckTask(applicationContext), 1000, HEALTH_CHECK_INTERVAL,
                                TimeUnit.MILLISECONDS);

  }

  public List<Env> getAllEnvs() {
    return allEnvs;
  }

  public List<Env> getActiveEnvs() {
    List<Env> activeEnvs = new LinkedList<>();
    for (Env env : allEnvs) {
      if (envStatusMark.get(env)) {
        activeEnvs.add(env);
      }
    }
    return activeEnvs;
  }

  public boolean isEnvActive(Env env) {
    Boolean mark = envStatusMark.get(env);
    return mark != null && mark;
  }

  private class HealthCheckTask implements Runnable {

    private static final int ENV_DOWN_THRESHOLD = 2;

    private Map<Env, Integer> healthCheckFailedCounter = new HashMap<>();

    private AdminServiceAPI.HealthAPI healthAPI;

    public HealthCheckTask(ApplicationContext context) {
      healthAPI = context.getBean(AdminServiceAPI.HealthAPI.class);
      for (Env env : allEnvs) {
        healthCheckFailedCounter.put(env, 0);
      }
    }

    public void run() {

      for (Env env : allEnvs) {
        try {
          if (isUp(env)) {
            //revive
            if (!envStatusMark.get(env)) {
              envStatusMark.put(env, true);
              healthCheckFailedCounter.put(env, 0);
              logger.info("Env revived because env health check success. env: {}", env);
            }
          } else {
            logger.error("Env health check failed, maybe because of admin server down. env: {}, meta server address: {}", env,
                    portalMetaDomainService.getDomain(env));
            handleEnvDown(env);
          }

        } catch (Exception e) {
          logger.error("Env health check failed, maybe because of meta server down "
                       + "or configure wrong meta server address. env: {}, meta server address: {}", env,
                  portalMetaDomainService.getDomain(env), e);
          handleEnvDown(env);
        }
      }

    }

    private boolean isUp(Env env) {
      Health health = healthAPI.health(env);
      return "UP".equals(health.getStatus().getCode());
    }

    private void handleEnvDown(Env env) {
      int failedTimes = healthCheckFailedCounter.get(env);
      healthCheckFailedCounter.put(env, ++failedTimes);

      if (!envStatusMark.get(env)) {
        logger.error("Env is down. env: {}, failed times: {}, meta server address: {}", env, failedTimes,
                portalMetaDomainService.getDomain(env));
      } else {
        if (failedTimes >= ENV_DOWN_THRESHOLD) {
          envStatusMark.put(env, false);
          logger.error("Env is down because health check failed for {} times, "
                       + "which equals to down threshold. env: {}, meta server address: {}", ENV_DOWN_THRESHOLD, env,
                  portalMetaDomainService.getDomain(env));
        } else {
          logger.error(
              "Env health check failed for {} times which less than down threshold. down threshold:{}, env: {}, meta server address: {}",
              failedTimes, ENV_DOWN_THRESHOLD, env, portalMetaDomainService.getDomain(env));
        }
      }

    }

  }
}
