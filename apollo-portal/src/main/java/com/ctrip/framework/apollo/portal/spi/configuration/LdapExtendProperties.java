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
/*
 * Copyright (c) 2019 www.ceair.com Inc. All rights reserved.
 */

package com.ctrip.framework.apollo.portal.spi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * the LdapExtendProperties description.
 *
 * @author wuzishu
 */
@ConfigurationProperties(prefix = "ldap")
public class LdapExtendProperties {

  private LdapMappingProperties mapping;
  private LdapGroupProperties group;

  public LdapMappingProperties getMapping() {
    return mapping;
  }

  public void setMapping(LdapMappingProperties mapping) {
    this.mapping = mapping;
  }

  public LdapGroupProperties getGroup() {
    return group;
  }

  public void setGroup(LdapGroupProperties group) {
    this.group = group;
  }
}
