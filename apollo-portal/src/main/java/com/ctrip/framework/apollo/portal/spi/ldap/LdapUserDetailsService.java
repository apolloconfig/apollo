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
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;


public class LdapUserDetailsService implements UserDetailsService {

    private final LdapUserSearch ldapUserSearch;

    private final UserDetailsContextMapper userDetailsContextMapper;

    private final LdapAuthoritiesPopulator authoritiesPopulator;

    private final LdapExtendProperties properties;

    public LdapUserDetailsService(LdapUserSearch ldapUserSearch, UserDetailsContextMapper userDetailsContextMapper, LdapAuthoritiesPopulator authoritiesPopulator, LdapExtendProperties properties) {
        this.ldapUserSearch = ldapUserSearch;
        this.userDetailsContextMapper = userDetailsContextMapper;
        this.authoritiesPopulator = authoritiesPopulator;
        this.properties = properties;
    }

    public UserDetails loadUserDetailsByUserId(String userId) throws UsernameNotFoundException {
        // 1. Search for LDAP entry by userId
        DirContextOperations userData = ldapUserSearch.searchForUser(userId);

        // 2. Extract the actual loginId from the entry
        String loginId = userData.getStringAttribute(
                properties.getMapping().getLoginId());
        // 3. Construct UserDetails
        return userDetailsContextMapper.mapUserFromContext(
                userData,
                loginId,
                authoritiesPopulator.getGrantedAuthorities(userData, loginId));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.loadUserDetailsByUserId(username);
    }

}