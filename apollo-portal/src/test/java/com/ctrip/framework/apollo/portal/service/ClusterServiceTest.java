package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class ClusterServiceTest extends com.ctrip.framework.apollo.portal.AbstractUnitTest {

  @Mock
  private AdminServiceAPI.ClusterAPI clusterAPI;
  @Mock
  private RoleInitializationService roleInitializationService;
  @Mock
  private RolePermissionService rolePermissionService;
  @Mock
  private UserInfoHolder userInfoHolder;

  @InjectMocks
  private ClusterService clusterService;

  private String appId = "clusterApp";
  private String clusterName = "default";
  private Env env = Env.DEV;

  @Before
  public void setUp() {
    UserInfo user = new UserInfo();
    user.setUserId("operator");
    when(userInfoHolder.getUser()).thenReturn(user);
  }

  @Test
  public void testDeleteClusterShouldCleanupRoles() {
    clusterService.deleteCluster(env, appId, clusterName);

    verify(rolePermissionService).deleteRolePermissionsByAppIdAndCluster(appId, env.getName(), clusterName, "operator");
    verify(clusterAPI).delete(env, appId, clusterName, "operator");
  }
}
