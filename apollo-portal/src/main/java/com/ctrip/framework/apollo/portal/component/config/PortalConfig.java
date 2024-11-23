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
package com.ctrip.framework.apollo.portal.component.config;
import com.ctrip.framework.apollo.portal.entity.vo.Organization;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PortalConfig {

  @Autowired
  private final EmailConfig emailConfig;
  @Autowired
  private final ServerConfig serverConfig;
  @Autowired
  private final SecurityConfig securityConfig;
  @Autowired
  private final EnvConfig envConfig;

  public PortalConfig(final PortalDBPropertySource portalDBPropertySource) {
    this.emailConfig = new EmailConfig(portalDBPropertySource);
    this.securityConfig = new SecurityConfig(portalDBPropertySource);
    this.envConfig = new EnvConfig(portalDBPropertySource);
    this.serverConfig = new ServerConfig(portalDBPropertySource);
  }

  /***
   * Level: important
   **/
  public List<Env> portalSupportedEnvs() {
    return envConfig.portalSupportedEnvs();
  }

  public int getPerEnvSearchMaxResults() {
    return envConfig.getPerEnvSearchMaxResults();
  }

  /**
   * @return the relationship between environment and its meta server. empty if meet exception
   */
  public Map<String, String> getMetaServers() {
    return serverConfig.getMetaServers();
  }

  public List<String> superAdmins() {
    return serverConfig.superAdmins();
  }

  public Set<Env> emailSupportedEnvs() {
    return envConfig.emailSupportedEnvs();
  }

  public Set<Env> webHookSupportedEnvs() {
    return envConfig.webHookSupportedEnvs();
  }

  public boolean isConfigViewMemberOnly(String env) {
    return securityConfig.isConfigViewMemberOnly(env);
  }

  /***
   * Level: normal
   **/
  public int connectTimeout() {
    return serverConfig.connectTimeout();
  }

  public int readTimeout() {
    return serverConfig.readTimeout();
  }

  public int connectionTimeToLive() {
    return serverConfig.connectionTimeToLive();
  }

  public int connectPoolMaxTotal() {
    return serverConfig.connectPoolMaxTotal();
  }

  public int connectPoolMaxPerRoute() {
    return serverConfig.connectPoolMaxPerRoute();
  }

  public List<Organization> organizations() {
     return serverConfig.organizations();
  }

  public String portalAddress() {
    return serverConfig.portalAddress();
  }

  public int refreshAdminServerAddressTaskNormalIntervalSecond() {
    return serverConfig.refreshAdminServerAddressTaskNormalIntervalSecond();
  }

  public int refreshAdminServerAddressTaskOfflineIntervalSecond() {
    return serverConfig.refreshAdminServerAddressTaskOfflineIntervalSecond();
  }

  public boolean isEmergencyPublishAllowed(Env env) {
    return serverConfig.isEmergencyPublishAllowed(env);
  }

  /***
   * Level: low
   **/
  public Set<Env> publishTipsSupportedEnvs() {
    return envConfig.publishTipsSupportedEnvs();
  }

  public String consumerTokenSalt() {
    return serverConfig.consumerTokenSalt();
  }

  public boolean isEmailEnabled() {
    return emailConfig.isEmailEnabled();
  }

  public String emailConfigHost() {
    return emailConfig.emailConfigHost();
  }

  public String emailConfigUser() {
    return emailConfig.emailConfigUser();
  }

  public String emailConfigPassword() {
    return emailConfig.emailConfigPassword();
  }

  public String emailSender() {
    return emailConfig.emailSender();
  }

  public String emailTemplateFramework() {
    return emailConfig.emailTemplateFramework();
  }

  public String emailReleaseDiffModuleTemplate() {
    return emailConfig.emailReleaseDiffModuleTemplate();
  }

  public String emailRollbackDiffModuleTemplate() {
    return emailConfig.emailRollbackDiffModuleTemplate();
  }

  public String emailGrayRulesModuleTemplate() {
    return emailConfig.emailGrayRulesModuleTemplate();
  }

  public String wikiAddress() {
    return serverConfig.wikiAddress();
  }

  public boolean canAppAdminCreatePrivateNamespace() {
    return securityConfig.canAppAdminCreatePrivateNamespace();
  }

  public boolean isCreateApplicationPermissionEnabled() {
    return securityConfig.isCreateApplicationPermissionEnabled();
  }

  public boolean isManageAppMasterPermissionEnabled() {
    return securityConfig.isManageAppMasterPermissionEnabled();
  }

  public String getAdminServiceAccessTokens() {
    return serverConfig.getAdminServiceAccessTokens();
  }

  public String[] webHookUrls() {
    return serverConfig.webHookUrls();
  }

  public boolean supportSearchByItem() {
    return serverConfig.supportSearchByItem();
  }
  
  public List<String> getUserPasswordNotAllowList() {
    return securityConfig.getUserPasswordNotAllowList();
  }

}
