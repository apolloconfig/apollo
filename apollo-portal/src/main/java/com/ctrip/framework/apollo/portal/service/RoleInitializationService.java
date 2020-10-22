package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.entity.App;

public interface RoleInitializationService {

  void initAppRoles(App app);

  void initNamespaceRoles(String appId, String namespaceName, String operator);

  void initNamespaceEnvRoles(String appId, String namespaceName, String operator);

  void initNamespaceSpecificEnvRoles(String appId, String namespaceName, String env,
      String operator);

  void initCreateAppRole();

  void initManageAppMasterRole(String appId, String operator);

}
