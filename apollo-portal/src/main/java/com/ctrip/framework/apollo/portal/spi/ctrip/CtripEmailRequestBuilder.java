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
import com.ctrip.framework.apollo.tracer.Tracer;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

public class CtripEmailRequestBuilder {

  private static final Logger logger = LoggerFactory.getLogger(CtripEmailRequestBuilder.class);

  private static Class sendEmailRequestClazz;
  private static Method setBodyContent;
  private static Method setRecipient;
  private static Method setSendCode;
  private static Method setBodyTemplateID;
  private static Method setSender;
  private static Method setSubject;
  private static Method setIsBodyHtml;
  private static Method setCharset;
  private static Method setExpiredTime;
  private static Method setAppID;

  @Autowired
  private PortalConfig portalConfig;


  @PostConstruct
  public void init() {
    try {
      sendEmailRequestClazz = Class.forName("com.ctrip.framework.apolloctripservice.emailservice.SendEmailRequest");

      setSendCode = sendEmailRequestClazz.getMethod("setSendCode", String.class);
      setBodyTemplateID = sendEmailRequestClazz.getMethod("setBodyTemplateID", Integer.class);
      setSender = sendEmailRequestClazz.getMethod("setSender", String.class);
      setBodyContent = sendEmailRequestClazz.getMethod("setBodyContent", String.class);
      setRecipient = sendEmailRequestClazz.getMethod("setRecipient", List.class);
      setSubject = sendEmailRequestClazz.getMethod("setSubject", String.class);
      setIsBodyHtml = sendEmailRequestClazz.getMethod("setIsBodyHtml", Boolean.class);
      setCharset = sendEmailRequestClazz.getMethod("setCharset", String.class);
      setExpiredTime = sendEmailRequestClazz.getMethod("setExpiredTime", Calendar.class);
      setAppID = sendEmailRequestClazz.getMethod("setAppID", Integer.class);

    } catch (Throwable e) {
      logger.error("init email request build failed", e);
      Tracer.logError("init email request build failed", e);
    }
  }


  public Object buildEmailRequest(Email email) throws Exception {

    Object emailRequest = createBasicEmailRequest();

    setSender.invoke(emailRequest, email.getSenderEmailAddress());
    setSubject.invoke(emailRequest, email.getSubject());
    String emailBodyBuilder = "<entry><content><![CDATA[<!DOCTYPE html>" +
            email.getBody() +
            "]]></content></entry>";
    setBodyContent.invoke(emailRequest, emailBodyBuilder);
    setRecipient.invoke(emailRequest, email.getRecipients());

    return emailRequest;
  }

  private Object createBasicEmailRequest() throws Exception {
    Object request = sendEmailRequestClazz.newInstance();

    setSendCode.invoke(request, portalConfig.sendCode());
    setBodyTemplateID.invoke(request, portalConfig.templateId());
    setIsBodyHtml.invoke(request, true);
    setCharset.invoke(request, "UTF-8");
    setExpiredTime.invoke(request, calExpiredTime());
    setAppID.invoke(request, portalConfig.appId());

    return request;
  }


  private Calendar calExpiredTime() {

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(DateUtils.addHours(new Date(), portalConfig.survivalDuration()));

    return calendar;
  }


}
