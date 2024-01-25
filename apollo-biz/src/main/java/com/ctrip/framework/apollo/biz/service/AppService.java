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

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.biz.entity.Audit;
import com.ctrip.framework.apollo.biz.repository.AppRepository;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class AppService {

  private final AppRepository appRepository;
  private final AuditService auditService;

  public AppService(final AppRepository appRepository, final AuditService auditService) {
    this.appRepository = appRepository;
    this.auditService = auditService;
  }

  public boolean isAppIdUnique(String appId) {
    Objects.requireNonNull(appId, "AppId must not be null");
    return Objects.isNull(appRepository.findByAppId(appId));
  }

  @Transactional
  @ApolloAuditLog(type = OpType.DELETE, name = "App.delete")
  public void delete(long id, String operator) {

    App app = appRepository.findById(id).orElse(null);
    if (app == null) {
      return;
    }
    app.setDeleted(true);
    app.setDataChangeLastModifiedBy(operator);
    appRepository.save(app);
    auditService.audit(App.class.getSimpleName(), id, Audit.OP.DELETE, operator);
  }

  public List<App> findAll(Pageable pageable) {
    Page<App> page = appRepository.findAll(pageable);
    return page.getContent();
  }

  public List<App> findByName(String name) {
    return appRepository.findByName(name);
  }

  public App findOne(String appId) {
    return appRepository.findByAppId(appId);
  }

  @Transactional
  @ApolloAuditLog(type = OpType.CREATE, name = "App.create")
  public App save(App entity) {
    if (!isAppIdUnique(entity.getAppId())) {
      throw new ServiceException("appId not unique");
    }
    entity.setId(0);//protection
    App app = appRepository.save(entity);

    auditService.audit(App.class.getSimpleName(), app.getId(), Audit.OP.INSERT,
        app.getDataChangeCreatedBy());

    return app;
  }

  @Transactional
  @ApolloAuditLog(type = OpType.UPDATE, name = "App.update")
  public void update(App app) {
    String appId = app.getAppId();

    App managedApp = appRepository.findByAppId(appId);
    if (managedApp == null) {
      throw BadRequestException.appNotExists(appId);
    }

    managedApp.setName(app.getName());
    managedApp.setOrgId(app.getOrgId());
    managedApp.setOrgName(app.getOrgName());
    managedApp.setOwnerName(app.getOwnerName());
    managedApp.setOwnerEmail(app.getOwnerEmail());
    managedApp.setDataChangeLastModifiedBy(app.getDataChangeLastModifiedBy());

    managedApp = appRepository.save(managedApp);
    auditService.audit(App.class.getSimpleName(), managedApp.getId(), Audit.OP.UPDATE,
        managedApp.getDataChangeLastModifiedBy());

  }
}
