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
package com.ctrip.framework.apollo.portal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.portal.component.AdminServiceAddressLocator;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.RetryableRestTemplate;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** Tests the encoded item key actually sent from Portal to AdminService. */
@ExtendWith(MockitoExtension.class)
class ItemAPITest {

  @Mock
  private RestTemplateFactory restTemplateFactory;
  @Mock
  private AdminServiceAddressLocator adminServiceAddressLocator;
  @Mock
  private PortalMetaDomainService portalMetaDomainService;
  @Mock
  private PortalConfig portalConfig;

  private AdminServiceAPI.ItemAPI itemAPI;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    RestTemplate http = new RestTemplate();
    server = MockRestServiceServer.bindTo(http).build();
    when(restTemplateFactory.getObject()).thenReturn(http);
    ServiceDTO adminService = new ServiceDTO();
    adminService.setHomepageUrl("http://admin.example.com");
    when(adminServiceAddressLocator.getServiceList(Env.DEV))
        .thenReturn(Collections.singletonList(adminService));

    RetryableRestTemplate retryable = new RetryableRestTemplate(restTemplateFactory,
        adminServiceAddressLocator, portalMetaDomainService, portalConfig);
    ReflectionTestUtils.invokeMethod(retryable, "postConstruct");
    itemAPI = new AdminServiceAPI.ItemAPI();
    itemAPI.restTemplate = retryable;
  }

  @ParameterizedTest
  @MethodSource("encodedKeys")
  void shouldSendUrlSafeUtf8KeyToAdminService(String key, String encodedKey) {
    ItemDTO expected = new ItemDTO(key, "value", "", 1);
    server
        .expect(requestTo("http://admin.example.com/apps/app/clusters/default/namespaces/"
            + "application/encodedItems/" + encodedKey))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(new Gson().toJson(expected), MediaType.APPLICATION_JSON));

    ItemDTO actual = itemAPI.loadItemByEncodeKey(Env.DEV, "app", "default", "application", key);

    assertThat(actual.getKey()).isEqualTo(key);
    assertThat(actual.getValue()).isEqualTo("value");
    server.verify();
  }

  private static Stream<Arguments> encodedKeys() {
    return Stream.of(Arguments.of("a/b", "YS9i"), Arguments.of("a/?", "YS8_"),
        Arguments.of("a/>", "YS8-"), Arguments.of("a/", "YS8"),
        Arguments.of("path\\name", "cGF0aFxuYW1l"),
        Arguments.of("path/中文\\name?", "cGF0aC_kuK3mlodcbmFtZT8"));
  }
}
