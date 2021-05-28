/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.portal.spi.ctrip;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.Email;
import com.ctrip.framework.apollo.portal.spi.EmailService;
import com.ctrip.framework.apollo.tracer.Tracer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

import javax.annotation.PostConstruct;


public class CtripEmailService implements EmailService {

  private static final Logger logger = LoggerFactory.getLogger(CtripEmailService.class);

  private Object emailServiceClient;
  private Method sendEmailAsync;
  private Method sendEmail;

  @Autowired
  private CtripEmailRequestBuilder emailRequestBuilder;
  @Autowired
  private PortalConfig portalConfig;

  @PostConstruct
  public void init() {
    try {
      initServiceClientConfig();

      Class emailServiceClientClazz =
              Class.forName("com.ctrip.framework.apolloctripservice.emailservice.EmailServiceClient");

      Method getInstanceMethod = emailServiceClientClazz.getMethod("getInstance");
      emailServiceClient = getInstanceMethod.invoke(null);

      Class sendEmailRequestClazz =
              Class.forName("com.ctrip.framework.apolloctripservice.emailservice.SendEmailRequest");
      sendEmailAsync = emailServiceClientClazz.getMethod("sendEmailAsync", sendEmailRequestClazz);
      sendEmail = emailServiceClientClazz.getMethod("sendEmail", sendEmailRequestClazz);
    } catch (Throwable e) {
      logger.error("init ctrip email service failed", e);
      Tracer.logError("init ctrip email service failed", e);
    }
  }

  private void initServiceClientConfig() throws Exception {

    Class serviceClientConfigClazz = Class.forName("com.ctriposs.baiji.rpc.client.ServiceClientConfig");
    Object serviceClientConfig = serviceClientConfigClazz.newInstance();
    Method setFxConfigServiceUrlMethod = serviceClientConfigClazz.getMethod("setFxConfigServiceUrl", String.class);

    setFxConfigServiceUrlMethod.invoke(serviceClientConfig, portalConfig.soaServerAddress());

    Class serviceClientBaseClazz = Class.forName("com.ctriposs.baiji.rpc.client.ServiceClientBase");
    Method initializeMethod = serviceClientBaseClazz.getMethod("initialize", serviceClientConfigClazz);
    initializeMethod.invoke(null, serviceClientConfig);
  }

  @Override
  public void send(Email email) {

    try {
      Object emailRequest = emailRequestBuilder.buildEmailRequest(email);

      Object sendResponse = portalConfig.isSendEmailAsync() ?
              sendEmailAsync.invoke(emailServiceClient, emailRequest) :
              sendEmail.invoke(emailServiceClient, emailRequest);

      logger.info("Email server response: " + sendResponse);

    } catch (Throwable e) {
      logger.error("send email failed", e);
      Tracer.logError("send email failed", e);
    }


  }

}
