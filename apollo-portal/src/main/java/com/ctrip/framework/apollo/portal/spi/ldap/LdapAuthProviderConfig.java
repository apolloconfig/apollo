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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

@Configuration
@Profile("ldap")
public class LdapAuthProviderConfig {

    @Bean
    @DependsOn("userSearch")
    public LdapAuthenticationProvider ldapAuthProvider(
            LdapContextSource ldapContextSource,
            LdapUserSearch userSearch,
            LdapAuthoritiesPopulator authoritiesPopulator,
            LdapExtendProperties ldapExtendProperties
    ) {
        BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
        bindAuthenticator.setUserSearch(userSearch);

        return new ApolloLdapAuthenticationProvider(
                bindAuthenticator,
                authoritiesPopulator,
                ldapExtendProperties
        );
    }

    @Bean
    public DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator(LdapContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextSource, null);
        populator.setIgnorePartialResultException(true);
        populator.setSearchSubtree(true);
        return populator;
    }
}