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
package com.ctrip.framework.apollo.portal.spi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xm.lin xm.lin@anxincloud.com
 * @Description
 * @date 18-8-9 下午4:36
 */
@ConfigurationProperties(prefix = "spring.ldap")
public class LdapProperties {

  private static final int DEFAULT_PORT = 389;

  /**
   * LDAP URLs of the server.
   */
  private String[] urls;

  /**
   * Base suffix from which all operations should originate.
   */
  private String base;

  /**
   * Login username of the server.
   */
  private String username;

  /**
   * Login password of the server.
   */
  private String password;

  /**
   * Whether read-only operations should use an anonymous environment.
   */
  private boolean anonymousReadOnly;

  /**
   * User search filter
   */
  private String searchFilter;

  /**
   * LDAP specification settings.
   */
  private final Map<String, String> baseEnvironment = new HashMap<>();

  public String[] getUrls() {
    return this.urls;
  }

  public void setUrls(String[] urls) {
    this.urls = urls;
  }

  public String getBase() {
    return this.base;
  }

  public void setBase(String base) {
    this.base = base;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean getAnonymousReadOnly() {
    return this.anonymousReadOnly;
  }

  public void setAnonymousReadOnly(boolean anonymousReadOnly) {
    this.anonymousReadOnly = anonymousReadOnly;
  }

  public String getSearchFilter() {
    return searchFilter;
  }

  public void setSearchFilter(String searchFilter) {
    this.searchFilter = searchFilter;
  }

  public Map<String, String> getBaseEnvironment() {
    return this.baseEnvironment;
  }

  public String[] determineUrls(Environment environment) {
    if (ObjectUtils.isEmpty(this.urls)) {
      return new String[]{"ldap://localhost:" + determinePort(environment)};
    }
    return this.urls;
  }

  private int determinePort(Environment environment) {
    Assert.notNull(environment, "Environment must not be null");
    String localPort = environment.getProperty("local.ldap.port");
    if (localPort != null) {
      return Integer.parseInt(localPort);
    }
    return DEFAULT_PORT;
  }
}
