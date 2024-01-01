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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.repository.AppRepository;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.client.HttpClientErrorException;
import static org.hamcrest.Matchers.containsString;

public class AppControllerTest extends AbstractControllerTest {

  @Autowired
  AppRepository appRepository;

  private String getBaseAppUrl() {
    return url("/apps/");
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCheckIfAppIdUnique() {
    AppDTO dto = generateSampleDTOData();
    ResponseEntity<AppDTO> response =
        restTemplate.postForEntity(getBaseAppUrl(), dto, AppDTO.class);
    AppDTO result = response.getBody();
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assert.assertEquals(dto.getAppId(), result.getAppId());
    Assert.assertTrue(result.getId() > 0);

    Boolean falseUnique =
        restTemplate.getForObject(getBaseAppUrl() + dto.getAppId() + "/unique", Boolean.class);
    Assert.assertFalse(falseUnique);
    Boolean trueUnique = restTemplate
        .getForObject(getBaseAppUrl() + dto.getAppId() + "true" + "/unique", Boolean.class);
    Assert.assertTrue(trueUnique);
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreate() {
    AppDTO dto = generateSampleDTOData();
    ResponseEntity<AppDTO> response =
        restTemplate.postForEntity(getBaseAppUrl(), dto, AppDTO.class);
    AppDTO result = response.getBody();
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assert.assertEquals(dto.getAppId(), result.getAppId());
    Assert.assertTrue(result.getId() > 0);

    App savedApp = appRepository.findById(result.getId()).orElse(null);
    Assert.assertEquals(dto.getAppId(), savedApp.getAppId());
    Assert.assertNotNull(savedApp.getDataChangeCreatedTime());
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateTwice() {
    AppDTO dto = generateSampleDTOData();
    ResponseEntity<AppDTO> response =
        restTemplate.postForEntity(getBaseAppUrl(), dto, AppDTO.class);
    AppDTO first = response.getBody();
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assert.assertEquals(dto.getAppId(), first.getAppId());
    Assert.assertTrue(first.getId() > 0);

    App savedApp = appRepository.findById(first.getId()).orElse(null);
    Assert.assertEquals(dto.getAppId(), savedApp.getAppId());
    Assert.assertNotNull(savedApp.getDataChangeCreatedTime());

    try {
      restTemplate.postForEntity(getBaseAppUrl(), dto, AppDTO.class);
    }catch (HttpClientErrorException e){
      Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testFind() {
    AppDTO dto = generateSampleDTOData();
    App app = BeanUtils.transform(App.class, dto);
    app = appRepository.save(app);

    AppDTO result = restTemplate.getForObject(getBaseAppUrl() + dto.getAppId(), AppDTO.class);
    Assert.assertEquals(dto.getAppId(), result.getAppId());
    Assert.assertEquals(dto.getName(), result.getName());
  }

  @Test(expected = HttpClientErrorException.class)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindNotExist() {
    restTemplate.getForEntity(getBaseAppUrl() + "notExists", AppDTO.class);
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testDelete() {
    AppDTO dto = generateSampleDTOData();
    App app = BeanUtils.transform(App.class, dto);
    app = appRepository.save(app);

    restTemplate.delete(url("/apps/{appId}?operator={operator}"), app.getAppId(), "test");

    App deletedApp = appRepository.findById(app.getId()).orElse(null);
    Assert.assertNull(deletedApp);
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldFailedWhenAppIdIsInvalid() {
    AppDTO dto = generateSampleDTOData();
    dto.setAppId("invalid app id");
    try {
      restTemplate.postForEntity(getBaseAppUrl(), dto, String.class);
      Assert.fail("Should throw");
    } catch (HttpClientErrorException e) {
      Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Assert.assertThat(new String(e.getResponseBodyAsByteArray()), containsString(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
    }
  }

  private AppDTO generateSampleDTOData() {
    AppDTO dto = new AppDTO();
    dto.setAppId("someAppId");
    dto.setName("someName");
    dto.setOwnerName("someOwner");
    dto.setOwnerEmail("someOwner@ctrip.com");
    dto.setDataChangeCreatedBy("apollo");
    dto.setDataChangeLastModifiedBy("apollo");
    return dto;
  }
}
