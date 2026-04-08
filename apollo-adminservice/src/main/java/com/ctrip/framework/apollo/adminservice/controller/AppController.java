/*
 * Copyright 2025 Apollo Authors
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

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import com.ctrip.framework.apollo.biz.service.AccessKeyService;
import com.ctrip.framework.apollo.biz.service.AdminService;
import com.ctrip.framework.apollo.biz.service.AppService;
import com.ctrip.framework.apollo.common.constants.AccessKeyMode;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.UniqueKeyGenerator;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;

@RestController
public class AppController {

  private static final Logger logger = LoggerFactory.getLogger(AppController.class);

  private final AppService appService;
  private final AdminService adminService;
  private final AccessKeyService accessKeyService;
  private final BizConfig bizConfig;

  public AppController(final AppService appService, final AdminService adminService,
      final AccessKeyService accessKeyService, final BizConfig bizConfig) {
    this.appService = appService;
    this.adminService = adminService;
    this.accessKeyService = accessKeyService;
    this.bizConfig = bizConfig;
  }

  @PostMapping("/apps")
  public AppDTO create(@Valid @RequestBody AppDTO dto) {
    App entity = BeanUtils.transform(App.class, dto);
    App managedEntity = appService.findOne(entity.getAppId());
    if (managedEntity != null) {
      throw BadRequestException.appAlreadyExists(entity.getAppId());
    }

    entity = adminService.createNewApp(entity);
    if (bizConfig.isAccessKeyAutoProvisionEnabled()) {
      try {
        accessKeyService.create(entity.getAppId(), createDefaultAccessKey(entity));
      } catch (Exception ex) {
        logger.warn("Failed to auto-provision access key for appId={}", entity.getAppId(), ex);
      }
    }

    return BeanUtils.transform(AppDTO.class, entity);
  }

  private AccessKey createDefaultAccessKey(App app) {
    String operator = app.getDataChangeCreatedBy();
    if (StringUtils.isBlank(operator)) {
      operator = app.getDataChangeLastModifiedBy();
    }
    if (StringUtils.isBlank(operator)) {
      operator = app.getOwnerName();
    }
    AccessKey accessKey = new AccessKey();
    accessKey.setAppId(app.getAppId());
    accessKey.setSecret(UniqueKeyGenerator.generateId());
    accessKey.setMode(AccessKeyMode.FILTER);
    accessKey.setEnabled(true);
    accessKey.setDataChangeCreatedBy(operator);
    return accessKey;
  }

  @DeleteMapping("/apps/{appId:.+}")
  public void delete(@PathVariable("appId") String appId, @RequestParam String operator) {
    App entity = appService.findOne(appId);
    if (entity == null) {
      throw NotFoundException.appNotFound(appId);
    }
    adminService.deleteApp(entity, operator);
  }

  @PutMapping("/apps/{appId:.+}")
  public void update(@PathVariable String appId, @RequestBody App app) {
    if (!Objects.equals(appId, app.getAppId())) {
      throw new BadRequestException("The App Id of path variable and request body is different");
    }

    appService.update(app);
  }

  @GetMapping("/apps")
  public List<AppDTO> find(@RequestParam(value = "name", required = false) String name,
      Pageable pageable) {
    List<App> app;
    if (StringUtils.isBlank(name)) {
      app = appService.findAll(pageable);
    } else {
      app = appService.findByName(name);
    }
    return BeanUtils.batchTransform(AppDTO.class, app);
  }

  @GetMapping("/apps/{appId:.+}")
  public AppDTO get(@PathVariable("appId") String appId) {
    App app = appService.findOne(appId);
    if (app == null) {
      throw NotFoundException.appNotFound(appId);
    }
    return BeanUtils.transform(AppDTO.class, app);
  }

  @GetMapping("/apps/{appId}/unique")
  public boolean isAppIdUnique(@PathVariable("appId") String appId) {
    return appService.isAppIdUnique(appId);
  }
}
