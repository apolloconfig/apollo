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
package com.ctrip.framework.apollo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ControllableAuthorizationConfiguration {

  @Primary
  @Bean
  public UserInfoHolder userInfoHolder() {
    return mock(UserInfoHolder.class);
  }

  @Primary
  @Bean
  public RolePermissionService rolePermissionService() {
    final RolePermissionService mock = mock(RolePermissionService.class);
    when(mock.userHasPermission(eq("luke"), any(), any())).thenReturn(true);
    return mock;
  }

  @Primary
  @Bean
  public ItemService itemService() {
    return mock(ItemService.class);
  }

}
