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

import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.common.config.RefreshablePropertySource;
import com.ctrip.framework.apollo.portal.entity.vo.Organization;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PortalConfig extends RefreshableConfig {

  private static final Logger logger = LoggerFactory.getLogger(PortalConfig.class);

  private static final int DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_NORMAL_INTERVAL_IN_SECOND = 5 * 60; //5min
  private static final int DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_OFFLINE_INTERVAL_IN_SECOND = 10; //10s

  private static final Gson GSON = new Gson();
  private static final Type ORGANIZATION = new TypeToken<List<Organization>>() {
  }.getType();

  private static final List<String> DEFAULT_USER_PASSWORD_NOT_ALLOW_LIST = Arrays.asList(
      "111", "222", "333", "444", "555", "666", "777", "888", "999", "000",
      "001122", "112233", "223344", "334455", "445566", "556677", "667788", "778899", "889900",
      "009988", "998877", "887766", "776655", "665544", "554433", "443322", "332211", "221100",
      "0123", "1234", "2345", "3456", "4567", "5678", "6789", "7890",
      "0987", "9876", "8765", "7654", "6543", "5432", "4321", "3210",
      "1q2w", "2w3e", "3e4r", "5t6y", "abcd", "qwer", "asdf", "zxcv"
  );

  /**
   * meta servers config in "PortalDB.ServerConfig"
   */
  private static final Type META_SERVERS = new TypeToken<Map<String, String>>(){}.getType();

  private final PortalDBPropertySource portalDBPropertySource;

  public PortalConfig(final PortalDBPropertySource portalDBPropertySource) {
    this.portalDBPropertySource = portalDBPropertySource;
  }

  @Override
  public List<RefreshablePropertySource> getRefreshablePropertySources() {
    return Collections.singletonList(portalDBPropertySource);
  }

  /***
   * Level: important
   **/
  public List<Env> portalSupportedEnvs() {
    String[] configurations = getArrayProperty("apollo.portal.envs", new String[]{"FAT", "UAT", "PRO"});
    List<Env> envs = Lists.newLinkedList();

    for (String env : configurations) {
      envs.add(Env.addEnvironment(env));
    }

    return envs;
  }

  public int getPerEnvSearchMaxResults() {return getIntProperty("apollo.portal.search.perEnvMaxResults", 200);}

  /**
   * @return the relationship between environment and its meta server. empty if meet exception
   */
  public Map<String, String> getMetaServers() {
    final String key = "apollo.portal.meta.servers";
    String jsonContent = getValue(key);
    if (null == jsonContent) {
      return Collections.emptyMap();
    }

    // watch out that the format of content may be wrong
    // that will cause exception
    Map<String, String> map = Collections.emptyMap();
    try {
      // try to parse
      map = GSON.fromJson(jsonContent, META_SERVERS);
    } catch (Exception e) {
      logger.error("Wrong format for: {}", key, e);
    }
    return map;
  }

  public List<String> superAdmins() {
    String superAdminConfig = getValue("superAdmin", "");
    if (Strings.isNullOrEmpty(superAdminConfig)) {
      return Collections.emptyList();
    }
    return splitter.splitToList(superAdminConfig);
  }

  public Set<Env> emailSupportedEnvs() {
    String[] configurations = getArrayProperty("email.supported.envs", null);

    Set<Env> result = Sets.newHashSet();
    if (configurations == null || configurations.length == 0) {
      return result;
    }

    for (String env : configurations) {
      result.add(Env.valueOf(env));
    }

    return result;
  }

  public Set<Env> webHookSupportedEnvs() {
    String[] configurations = getArrayProperty("webhook.supported.envs", null);

    Set<Env> result = Sets.newHashSet();
    if (configurations == null || configurations.length == 0) {
      return result;
    }

    for (String env : configurations) {
      result.add(Env.valueOf(env));
    }

    return result;
  }

  public boolean isConfigViewMemberOnly(String env) {
    String[] configViewMemberOnlyEnvs = getArrayProperty("configView.memberOnly.envs", new String[0]);

    for (String memberOnlyEnv : configViewMemberOnlyEnvs) {
      if (memberOnlyEnv.equalsIgnoreCase(env)) {
        return true;
      }
    }

    return false;
  }

  /***
   * Level: normal
   **/
  public int connectTimeout() {
    return getIntProperty("api.connectTimeout", 3000);
  }

  public int readTimeout() {
    return getIntProperty("api.readTimeout", 10000);
  }

  public int connectionTimeToLive() {
    return getIntProperty("api.connectionTimeToLive", -1);
  }

  public int connectPoolMaxTotal() {
    return getIntProperty("api.pool.max.total", 20);
  }

  public int connectPoolMaxPerRoute() {
    return getIntProperty("api.pool.max.per.route", 2);
  }

  public List<Organization> organizations() {

    String organizations = getValue("organizations");
    return organizations == null ? Collections.emptyList() : GSON.fromJson(organizations, ORGANIZATION);
  }

  public String portalAddress() {
    return getValue("apollo.portal.address");
  }

  public int refreshAdminServerAddressTaskNormalIntervalSecond() {
    int interval = getIntProperty("refresh.admin.server.address.task.normal.interval.second", DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_NORMAL_INTERVAL_IN_SECOND);
    return checkInt(interval, 5, Integer.MAX_VALUE, DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_NORMAL_INTERVAL_IN_SECOND);
  }

  public int refreshAdminServerAddressTaskOfflineIntervalSecond() {
    int interval = getIntProperty("refresh.admin.server.address.task.offline.interval.second", DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_OFFLINE_INTERVAL_IN_SECOND);
    return checkInt(interval, 5, Integer.MAX_VALUE, DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_OFFLINE_INTERVAL_IN_SECOND);
  }

  public boolean isEmergencyPublishAllowed(Env env) {
    String targetEnv = env.getName();

    String[] emergencyPublishSupportedEnvs = getArrayProperty("emergencyPublish.supported.envs", new String[0]);

    for (String supportedEnv : emergencyPublishSupportedEnvs) {
      if (Objects.equals(targetEnv, supportedEnv.toUpperCase().trim())) {
        return true;
      }
    }

    return false;
  }

  /***
   * Level: low
   **/
  public Set<Env> publishTipsSupportedEnvs() {
    String[] configurations = getArrayProperty("namespace.publish.tips.supported.envs", null);

    Set<Env> result = Sets.newHashSet();
    if (configurations == null || configurations.length == 0) {
      return result;
    }

    for (String env : configurations) {
      result.add(Env.valueOf(env));
    }

    return result;
  }

  public String consumerTokenSalt() {
    return getValue("consumer.token.salt", "apollo-portal");
  }

  public boolean isEmailEnabled() {
    return getBooleanProperty("email.enabled", false);
  }

  public String emailConfigHost() {
    return getValue("email.config.host", "");
  }

  public String emailConfigUser() {
    return getValue("email.config.user", "");
  }

  public String emailConfigPassword() {
    return getValue("email.config.password", "");
  }

  public String emailSender() {
    String value = getValue("email.sender", "");
    if (Strings.isNullOrEmpty(value)) {
      value = emailConfigUser();
    }
    return value;
  }

  public String emailTemplateFramework() {
    return getValue("email.template.framework", "");
  }

  public String emailReleaseDiffModuleTemplate() {
    return getValue("email.template.release.module.diff", "");
  }

  public String emailRollbackDiffModuleTemplate() {
    return getValue("email.template.rollback.module.diff", "");
  }

  public String emailGrayRulesModuleTemplate() {
    return getValue("email.template.release.module.rules", "");
  }

  public String wikiAddress() {
    return getValue("wiki.address", "https://www.apolloconfig.com");
  }

  public boolean canAppAdminCreatePrivateNamespace() {
    return getBooleanProperty("admin.createPrivateNamespace.switch", true);
  }

  public boolean isCreateApplicationPermissionEnabled() {
    return getBooleanProperty(SystemRoleManagerService.CREATE_APPLICATION_LIMIT_SWITCH_KEY, false);
  }

  public boolean isManageAppMasterPermissionEnabled() {
    return getBooleanProperty(SystemRoleManagerService.MANAGE_APP_MASTER_LIMIT_SWITCH_KEY, false);
  }

  public String getAdminServiceAccessTokens() {
    return getValue("admin-service.access.tokens");
  }

  public String[] webHookUrls() {
    return getArrayProperty("config.release.webhook.service.url", null);
  }

  public boolean supportSearchByItem() {
    return getBooleanProperty("searchByItem.switch", true);
  }
  
  public List<String> getUserPasswordNotAllowList() {
    String[] value = getArrayProperty("apollo.portal.auth.user-password-not-allow-list", null);
    if (value == null || value.length == 0) {
      return DEFAULT_USER_PASSWORD_NOT_ALLOW_LIST;
    }
    return Arrays.asList(value);
  }

  private int checkInt(int value, int min, int max, int defaultValue) {
    if (value >= min && value <= max) {
      return value;
    }
    logger.warn("Configuration value '{}' is out of bounds [{} - {}]. Using default value '{}'.", value, min, max, defaultValue);
    return defaultValue;
  }
}
