/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
        logger.error("Notify webHook server failed, env: {}, webHook server url:{}", env, url, e);
      }
    }
  }
}
