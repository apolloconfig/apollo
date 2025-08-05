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

package com.ctrip.framework.apollo.portal.spi.ldap;

import com.ctrip.framework.apollo.portal.spi.configuration.LdapExtendProperties;
import com.ctrip.framework.apollo.portal.spi.configuration.LdapProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;

@Configuration
@Profile("ldap")
public class LdapSearchConfiguration {

    private final LdapProperties ldapProperties;
    private final LdapContextSource ldapContextSource;
    private final LdapExtendProperties ldapExtendProperties;

    public LdapSearchConfiguration(LdapProperties ldapProperties,
                                   LdapContextSource ldapContextSource,
                                   LdapExtendProperties ldapExtendProperties) {
        this.ldapProperties = ldapProperties;
        this.ldapContextSource = ldapContextSource;
        this.ldapExtendProperties = ldapExtendProperties;
    }

    @Bean
    public LdapUserSearch userSearch() {
        if (ldapExtendProperties.getGroup() == null ||
                StringUtils.isBlank(ldapExtendProperties.getGroup().getGroupSearch())) {

            FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch(
                    "", ldapProperties.getSearchFilter(), ldapContextSource);
            search.setSearchSubtree(true);
            return search;
        }

        FilterLdapByGroupUserSearch search = new FilterLdapByGroupUserSearch(
                ldapProperties.getBase(),
                ldapProperties.getSearchFilter(),
                ldapExtendProperties.getGroup().getGroupBase(),
                ldapContextSource,
                ldapExtendProperties.getGroup().getGroupSearch(),
                ldapExtendProperties.getMapping().getRdnKey(),
                ldapExtendProperties.getGroup().getGroupMembership(),
                ldapExtendProperties.getMapping().getLoginId());
        search.setSearchSubtree(true);
        return search;
    }
}