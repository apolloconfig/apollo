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
package com.ctrip.framework.apollo.biz.service;

import static org.junit.Assert.assertNotNull;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author nisiyong
 */
public class AccessKeyServiceTest extends AbstractIntegrationTest {

  @Autowired
  private AccessKeyService accessKeyService;

  @Test
  public void testCreate() {
    String appId = "someAppId";
    String secret = "someSecret";
    AccessKey entity = assembleAccessKey(appId, secret);

    AccessKey accessKey = accessKeyService.create(appId, entity);

    assertNotNull(accessKey);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateWithException() {
    String appId = "someAppId";
    String secret = "someSecret";
    int maxCount = 5;

    for (int i = 0; i <= maxCount; i++) {
      AccessKey entity = assembleAccessKey(appId, secret);
      accessKeyService.create(appId, entity);
    }
  }

  private AccessKey assembleAccessKey(String appId, String secret) {
    AccessKey accessKey = new AccessKey();
    accessKey.setAppId(appId);
    accessKey.setSecret(secret);
    return accessKey;
  }
}