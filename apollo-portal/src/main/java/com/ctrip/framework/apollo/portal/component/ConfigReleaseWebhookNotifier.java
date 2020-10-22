package com.ctrip.framework.apollo.portal.component;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.environment.Env;

/**
 * publish webHook
 *
 * @author HuangSheng
 */
@Component
public class ConfigReleaseWebhookNotifier {

  private static final Logger logger = LoggerFactory.getLogger(ConfigReleaseWebhookNotifier.class);

  private final RestTemplateFactory restTemplateFactory;

  private RestTemplate restTemplate;

  public ConfigReleaseWebhookNotifier(RestTemplateFactory restTemplateFactory) {
    this.restTemplateFactory = restTemplateFactory;
  }

  @PostConstruct
  public void init() {
    // init restTemplate
    restTemplate = restTemplateFactory.getObject();
  }

  public void notify(String[] webHookUrls, Env env, ReleaseHistoryBO releaseHistory) {
    if (webHookUrls == null) {
      return;
    }

    for (String webHookUrl : webHookUrls) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
      HttpEntity entity = new HttpEntity(releaseHistory, headers);
      String url = webHookUrl + "?env={env}";
      try {
        restTemplate.postForObject(url, entity, String.class, env);
      } catch (Exception e) {
        logger.error("Notify webHook server failed. webHook server url:{}", env, url, e);
      }
    }
  }
}
