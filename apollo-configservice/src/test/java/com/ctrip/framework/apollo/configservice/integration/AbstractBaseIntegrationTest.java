package com.ctrip.framework.apollo.configservice.integration;

import com.ctrip.framework.apollo.biz.service.BizDBPropertySource;
import com.google.gson.Gson;

import com.ctrip.framework.apollo.ConfigServiceTestConfiguration;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.repository.ReleaseMessageRepository;
import com.ctrip.framework.apollo.biz.repository.ReleaseRepository;
import com.ctrip.framework.apollo.biz.utils.ReleaseKeyGenerator;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AbstractBaseIntegrationTest.TestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractBaseIntegrationTest {
  @Autowired
  private ReleaseMessageRepository releaseMessageRepository;
  @Autowired
  private ReleaseRepository releaseRepository;

  private static final Gson GSON = new Gson();

  protected RestTemplate restTemplate = (new TestRestTemplate()).getRestTemplate();

  @PostConstruct
  private void postConstruct() {
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
  }

  @Value("${local.server.port}")
  int port;

  protected String getHostUrl() {
    return "localhost:" + port;
  }

  @Configuration
  @Import(ConfigServiceTestConfiguration.class)
  protected static class TestConfiguration {
    @Bean
    public BizConfig bizConfig(final BizDBPropertySource bizDBPropertySource) {
      return new TestBizConfig(bizDBPropertySource);
    }
  }

  protected void sendReleaseMessage(String message) {
    ReleaseMessage releaseMessage = new ReleaseMessage(message);
    releaseMessageRepository.save(releaseMessage);
  }

  public Release buildRelease(String name, String comment, Namespace namespace,
                              Map<String, String> configurations, String owner) {
    Release release = new Release();
    release.setReleaseKey(ReleaseKeyGenerator.generateReleaseKey(namespace));
    release.setDataChangeCreatedTime(new Date());
    release.setDataChangeCreatedBy(owner);
    release.setDataChangeLastModifiedBy(owner);
    release.setName(name);
    release.setComment(comment);
    release.setAppId(namespace.getAppId());
    release.setClusterName(namespace.getClusterName());
    release.setNamespaceName(namespace.getNamespaceName());
    release.setConfigurations(GSON.toJson(configurations));
    release = releaseRepository.save(release);

    return release;
  }

  protected void periodicSendMessage(ExecutorService executorService, String message, AtomicBoolean stop) {
    executorService.submit(() -> {
      //wait for the request connected to server
      while (!stop.get() && !Thread.currentThread().isInterrupted()) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
        }

        //double check
        if (stop.get()) {
          break;
        }

        sendReleaseMessage(message);
      }
    });
  }

  private static class TestBizConfig extends BizConfig {
    public TestBizConfig(final BizDBPropertySource propertySource) {
      super(propertySource);
    }

    @Override
    public int appNamespaceCacheScanInterval() {
      //should be short enough to update the AppNamespace cache in time
      return 1;
    }

    @Override
    public TimeUnit appNamespaceCacheScanIntervalTimeUnit() {
      return TimeUnit.MILLISECONDS;
    }
  }
}
