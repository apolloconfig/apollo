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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Verifies migrated OpenAPI controller security annotations stay aligned with legacy Portal
 * behavior for UI routes, plus intentional audit coverage for role mutations.
 */
class OpenApiControllerAnnotationParityTest {

  private static final List<Class<?>> OPENAPI_CONTROLLER_CLASSES = List.of(
      AccessKeyController.class, AppController.class, ClusterController.class, ItemController.class,
      NamespaceBranchController.class, NamespaceController.class, PermissionController.class,
      PortalManagementController.class, PortalUserController.class, ReleaseController.class);

  @Test
  void appAuditAnnotationsShouldMatchLegacyController() throws Exception {
    assertAudit(AppController.class.getMethod("deleteApp", String.class, String.class), OpType.RPC,
        "App.delete");
  }

  @Test
  void appPermissionAnnotationsShouldMatchLegacyController() throws Exception {
    assertNoPreAuthorize(AppController.class.getMethod("createAppInEnv", String.class,
        OpenAppDTO.class, String.class));
    assertPreAuthorize(AppController.class.getMethod("deleteApp", String.class, String.class),
        "@unifiedPermissionValidator.isSuperAdmin()");
  }

  @Test
  void clusterAuditAnnotationsShouldMatchLegacyController() throws Exception {
    assertAudit(ClusterController.class.getMethod("createCluster", String.class, String.class,
        OpenClusterDTO.class), OpType.CREATE, "Cluster.create");
    assertAudit(ClusterController.class.getMethod("deleteCluster", String.class, String.class,
        String.class, String.class), OpType.DELETE, "Cluster.delete");
  }

  @Test
  void clusterPermissionAnnotationsShouldMatchLegacyController() throws Exception {
    assertPreAuthorize(
        ClusterController.class.getMethod("createCluster", String.class, String.class,
            OpenClusterDTO.class),
        "@unifiedPermissionValidator.hasCreateClusterPermission(#appId)");
    assertNoPreAuthorize(ClusterController.class.getMethod("deleteCluster", String.class,
        String.class, String.class, String.class));
  }

  @Test
  void namespaceBranchAuditAnnotationsShouldMatchLegacyController() throws Exception {
    assertAudit(NamespaceBranchController.class.getMethod("createBranch", String.class,
        String.class, String.class, String.class, String.class), OpType.CREATE,
        "NamespaceBranch.create");
    assertAudit(
        NamespaceBranchController.class.getMethod("deleteBranch", String.class, String.class,
            String.class, String.class, String.class, String.class),
        OpType.DELETE, "NamespaceBranch.delete");
    assertAudit(
        NamespaceBranchController.class.getMethod("merge", String.class, String.class, String.class,
            String.class, String.class, Boolean.class, NamespaceReleaseDTO.class),
        OpType.UPDATE, "NamespaceBranch.merge");
    assertAudit(NamespaceBranchController.class.getMethod("mergeBranch", String.class, String.class,
        String.class, String.class, String.class, Boolean.class, NamespaceReleaseDTO.class,
        String.class), OpType.UPDATE, "NamespaceBranch.merge");
    assertAudit(
        NamespaceBranchController.class.getMethod("updateBranchRules", String.class, String.class,
            String.class, String.class, String.class, OpenGrayReleaseRuleDTO.class, String.class),
        OpType.UPDATE, "NamespaceBranch.updateBranchRules");
  }

  @Test
  void permissionAnnotationsShouldMatchLegacyController() throws Exception {
    assertNoPreAuthorize(PermissionController.class.getMethod("initAppPermission", String.class,
        String.class, String.class));
    assertNoPreAuthorize(PermissionController.class.getMethod("initClusterNamespacePermission",
        String.class, String.class, String.class, String.class));
  }

  @Test
  void permissionAuditAnnotationsShouldCoverRoleAndInitializationMutations() throws Exception {
    assertAudit(
        PermissionController.class.getMethod("assignClusterNamespaceRoleToUser", String.class,
            String.class, String.class, String.class, String.class, String.class),
        OpType.CREATE, "Auth.assignClusterNamespaceRoleToUser");
    assertAudit(
        PermissionController.class.getMethod("removeClusterNamespaceRoleFromUser", String.class,
            String.class, String.class, String.class, String.class, String.class),
        OpType.DELETE, "Auth.removeClusterNamespaceRoleFromUser");
    assertAudit(PermissionController.class.getMethod("initAppPermission", String.class,
        String.class, String.class), OpType.CREATE, "Auth.initAppPermission");
    assertAudit(
        PermissionController.class.getMethod("initClusterNamespacePermission", String.class,
            String.class, String.class, String.class),
        OpType.CREATE, "Auth.initClusterNamespacePermission");
  }

  @Test
  void openApiPreAuthorizeExpressionsShouldNotEmbedAuthTypeBranching() {
    for (Class<?> controllerClass : OPENAPI_CONTROLLER_CLASSES) {
      for (Method method : controllerClass.getDeclaredMethods()) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        if (preAuthorize == null) {
          continue;
        }
        // Keep auth-type compatibility branches in Java helpers or method bodies. Embedding them
        // directly in SpEL is hard to compare against legacy WebAPI behavior and easy to regress.
        assertThat(preAuthorize.value()).doesNotContain("UserIdentityContextHolder")
            .doesNotContain("UserIdentityConstants").doesNotContain("T(").doesNotContain("&&")
            .doesNotContain("||");
      }
    }
  }

  private void assertAudit(Method method, OpType type, String name) {
    ApolloAuditLog auditLog = method.getAnnotation(ApolloAuditLog.class);
    assertThat(auditLog).isNotNull();
    assertThat(auditLog.type()).isEqualTo(type);
    assertThat(auditLog.name()).isEqualTo(name);
  }

  private void assertPreAuthorize(Method method, String value) {
    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
    assertThat(preAuthorize).isNotNull();
    assertThat(preAuthorize.value()).isEqualTo(value);
  }

  private void assertNoPreAuthorize(Method method) {
    assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
  }
}
