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
package com.ctrip.framework.apollo.openapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.SkipAuthorizationConfiguration;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.model.OpenConsumerRoleDTO;
import com.ctrip.framework.apollo.portal.PortalApplication;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies consumer role aggregation and scoped revocation against the Portal H2 database.
 */
@SpringBootTest(classes = {PortalApplication.class, SkipAuthorizationConfiguration.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
// Portal 启动时会校验必须配置认证方式，因此这里显式启用 auth，保证默认的 mvn test 能启动上下文
@ActiveProfiles({"test", "auth"})
@Sql(scripts = "/sql/openapi/ConsumerRoleManagementIntegrationTest.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ConsumerRoleManagementIntegrationTest {

  @Autowired
  private ConsumerService consumerService;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PlatformTransactionManager transactionManager;

  @Test
  void aggregatesNamespacePairsWithoutMergingDifferentEnvironments() {
    List<OpenConsumerRoleDTO> roles = consumerService.findConsumerRoleDetailsByConsumerId(1000L);

    assertEquals(7, roles.size());
    assertTrue(roles.contains(new OpenConsumerRoleDTO().type("AppRole").appId("role-test-app")
        .namespaceName(null).env(null)));
    for (String env : new String[] {null, "DEV", "PRO", "FAT", "FWS"}) {
      assertTrue(roles.contains(new OpenConsumerRoleDTO().type("NamespaceRole")
          .appId("role-test-app").namespaceName("application").env(env)));
    }
  }

  @Test
  void revokesBothNamespaceRolesOnlyForTheTargetConsumerAndEnvironment() {
    consumerService.revokeConsumerRole(1000L, "NamespaceRole", "role-test-app", "application",
        "dev", "operator");

    assertEquals(2,
        count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
            + "AND RoleId IN (1003, 1004) AND IsDeleted = TRUE AND DeletedAt > 0 "
            + "AND DataChange_LastModifiedBy = 'operator'"));
    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1001 "
        + "AND RoleId IN (1003, 1004) AND IsDeleted = FALSE"));
    assertEquals(14, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND IsDeleted = FALSE"));
    assertEquals(16, count("SELECT COUNT(*) FROM \"Role\" WHERE IsDeleted = FALSE"));
    assertEquals(1, count("SELECT COUNT(*) FROM \"RolePermission\" WHERE IsDeleted = FALSE"));
    assertEquals(1, count("SELECT COUNT(*) FROM \"UserRole\" WHERE IsDeleted = FALSE"));

    // 绑定已被撤销，再次撤销同一范围应当报错而不是静默成功
    assertThrows(BadRequestException.class, () -> consumerService.revokeConsumerRole(1000L,
        "NamespaceRole", "role-test-app", "application", "DEV", "operator"));
  }

  @Test
  void revokingAllEnvironmentGrantDoesNotRemoveEnvironmentSpecificGrants() {
    consumerService.revokeConsumerRole(1000L, "NamespaceRole", "role-test-app", "application", null,
        "operator");

    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1001, 1002) AND IsDeleted = TRUE"));
    assertEquals(4, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1003, 1004, 1005, 1006) AND IsDeleted = FALSE"));
  }

  @Test
  void revokingAppRoleDoesNotRemoveNamespaceOrSystemGrants() {
    consumerService.revokeConsumerRole(1000L, "AppRole", "role-test-app", null, null, "operator");

    assertEquals(1, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId = 1000 AND IsDeleted = TRUE"));
    assertEquals(15, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND IsDeleted = FALSE"));
  }

  @Test
  void namespacePairRevocationRollsBackWithTheTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
      consumerService.revokeConsumerRole(1000L, "NamespaceRole", "role-test-app", "application",
          "DEV", "operator");
      throw new IllegalStateException("force rollback");
    }));

    assertEquals(2,
        count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
            + "AND RoleId IN (1003, 1004) AND IsDeleted = FALSE AND DeletedAt = 0 "
            + "AND DataChange_LastModifiedBy = 'apollo'"));
  }

  @Test
  void revokesRemainingBindingWhenOnlyOneNamespaceRoleIsBound() {
    jdbcTemplate.update("UPDATE \"ConsumerRole\" SET IsDeleted = TRUE, DeletedAt = 1 "
        + "WHERE ConsumerId = 1000 AND RoleId = 1004");

    consumerService.revokeConsumerRole(1000L, "NamespaceRole", "role-test-app", "application",
        "DEV", "operator");

    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1003, 1004) AND IsDeleted = TRUE"));
  }

  @Test
  void revokingFwsDoesNotRemoveFatBindings() {
    // FWS 与 FAT 是不同的环境，撤销 FWS 时不能被 Env 的 FWS->FAT 别名规则改写到 FAT
    consumerService.revokeConsumerRole(1000L, "NamespaceRole", "role-test-app", "application",
        "FWS", "operator");

    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1011, 1012) AND IsDeleted = TRUE"));
    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1009, 1010) AND IsDeleted = FALSE"));
  }

  @Test
  void revokingListedScopeClearsNamespaceNameWithLeadingSpace() {
    // 列表返回的是未 trim 的原始 namespace 名，撤销必须用同一个值才能命中
    List<OpenConsumerRoleDTO> roles = consumerService.findConsumerRoleDetailsByConsumerId(1000L);
    assertTrue(roles.contains(new OpenConsumerRoleDTO().type("NamespaceRole").appId("role-test-app")
        .namespaceName(" application").env(null)));

    consumerService.revokeConsumerRole(1000L, "NamespaceRole", "role-test-app", " application",
        null, "operator");

    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1013, 1014) AND IsDeleted = TRUE"));
    assertEquals(0, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1013, 1014) AND IsDeleted = FALSE"));
  }

  @Test
  void revokingAllEnvironmentGrantDoesNotMatchNamespaceNameContainingEnvSegment() {
    // namespaceName="application+DEV" 且 env=null 表示“所有环境”的那条授权，
    // 不能被拼成 ModifyNamespace+app+application+DEV 而误删 DEV 的绑定
    assertThrows(BadRequestException.class, () -> consumerService.revokeConsumerRole(1000L,
        "NamespaceRole", "role-test-app", "application+DEV", null, "operator"));

    assertEquals(2, count("SELECT COUNT(*) FROM \"ConsumerRole\" WHERE ConsumerId = 1000 "
        + "AND RoleId IN (1003, 1004) AND IsDeleted = FALSE"));
  }

  private int count(String sql) {
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }
}
