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
package com.ctrip.framework.apollo.portal.spi.configuration;

import com.ctrip.framework.apollo.portal.filter.UserTypeResolverFilter;
import com.ctrip.framework.apollo.openapi.filter.ConsumerAuthenticationFilter;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class AuthFilterConfigurationTest {

    private AuthFilterConfiguration authFilterConfiguration;
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authFilterConfiguration = new AuthFilterConfiguration();
    }

    @Test
    public void authTypeResolverFilter_ShouldReturnConfiguredFilterRegistrationBean() {
        // Call the method under test
        FilterRegistrationBean<UserTypeResolverFilter> filterRegistrationBean = authFilterConfiguration.authTypeResolverFilter();

        // Verify that FilterRegistrationBean is created correctly
        assertNotNull(filterRegistrationBean, "FilterRegistrationBean should not be null");

        // Verify that UserTypeResolverFilter is set correctly
        UserTypeResolverFilter filter = filterRegistrationBean.getFilter();
        assertNotNull(filter, "UserTypeResolverFilter should not be null");

        // Verify that URL pattern is added correctly
        assertEquals("/*", filterRegistrationBean.getUrlPatterns().iterator().next(), "URL pattern should be '/*'");
    }


    @Test
    public void openApiAuthenticationFilter_ShouldReturnConfiguredFilterRegistrationBean() {
        // Mock dependencies (assuming they are properly initialized)
        ConsumerAuthUtil mockConsumerAuthUtil = mock(ConsumerAuthUtil.class);
        ConsumerAuditUtil mockConsumerAuditUtil = mock(ConsumerAuditUtil.class);

        // Call the method under test to get FilterRegistrationBean
        FilterRegistrationBean<ConsumerAuthenticationFilter> result =
                authFilterConfiguration.openApiAuthenticationFilter(mockConsumerAuthUtil, mockConsumerAuditUtil);

        // Verify that URL pattern configuration is correct
        assertEquals(1, result.getUrlPatterns().size());
        assertEquals("/openapi/*", result.getUrlPatterns().iterator().next());


        // Verify that there are no unexpected interactions
        verifyNoMoreInteractions(mockConsumerAuthUtil, mockConsumerAuditUtil);
    }
}