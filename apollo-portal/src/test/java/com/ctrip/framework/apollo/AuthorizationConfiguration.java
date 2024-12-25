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
public class AuthorizationConfiguration {

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
