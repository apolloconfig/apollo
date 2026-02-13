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
package com.ctrip.framework.apollo.portal.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Supplemental test coverage for Apollo PR #5542 environment normalization fix.
 * Tests comprehensive coverage of env aliases, boundary values, and invalid env handling.
 *
 * @see <a href="https://github.com/apolloconfig/apollo/issues/5442">#5442</a>
 */
@ExtendWith(MockitoExtension.class)
class UserPermissionValidatorTestSupplement {

  private static final String USER_ID = "test-user";
  private static final String APP_ID = "test-app";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";

  @Mock
  private UserInfoHolder userInfoHolder;
  @Mock
  private RolePermissionService rolePermissionService;
  @Mock
  private PortalConfig portalConfig;
  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private SystemRoleManagerService systemRoleManagerService;
  @InjectMocks
  private UserPermissionValidator validator;

  @BeforeEach
  void setUp() {
    UserInfo stubUser = new UserInfo();
    stubUser.setUserId(USER_ID);
    stubUser.setName("test");
    lenient().when(userInfoHolder.getUser()).thenReturn(stubUser);
  }

  // ========== All Environment Aliases Tests ==========

  /**
   * Test FAT environment and its alias FWS (FWS maps to FAT).
   * Verifies issue #5442 fix for environment alias normalization.
   */
  @Test
  void shouldHideConfigToCurrentUser_envNormalization_fatAndFwsAliases() {
    when(portalConfig.isConfigViewMemberOnly("FAT")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // Test with "fat" lowercase - should normalize to "FAT"
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "fat", CLUSTER, NAMESPACE)).isTrue();

    // Test with "FAT" uppercase - should remain "FAT"
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "FAT", CLUSTER, NAMESPACE)).isTrue();

    // Test with "FWS" - should normalize to "FAT" (special mapping)
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "FWS", CLUSTER, NAMESPACE)).isTrue();

    // Test with "fws" lowercase - should also normalize to "FAT"
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "fws", CLUSTER, NAMESPACE)).isTrue();
  }

  /**
   * Test LPT environment normalization.
   */
  @Test
  void shouldHideConfigToCurrentUser_envNormalization_lptAlias() {
    when(portalConfig.isConfigViewMemberOnly("LPT")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // Test with "lpt" lowercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "lpt", CLUSTER, NAMESPACE)).isTrue();

    // Test with "LPT" uppercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "LPT", CLUSTER, NAMESPACE)).isTrue();
  }

  /**
   * Test TOOLS environment normalization.
   */
  @Test
  void shouldHideConfigToCurrentUser_envNormalization_toolsAlias() {
    when(portalConfig.isConfigViewMemberOnly("TOOLS")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // Test with "tools" lowercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "tools", CLUSTER, NAMESPACE)).isTrue();

    // Test with "TOOLS" uppercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "TOOLS", CLUSTER, NAMESPACE)).isTrue();
  }

  /**
   * Test DEV environment normalization.
   */
  @Test
  void shouldHideConfigToCurrentUser_envNormalization_devAlias() {
    when(portalConfig.isConfigViewMemberOnly("DEV")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // Test with "dev" lowercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "dev", CLUSTER, NAMESPACE)).isTrue();

    // Test with "DEV" uppercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "DEV", CLUSTER, NAMESPACE)).isTrue();
  }

  /**
   * Test UAT environment normalization.
   */
  @Test
  void shouldHideConfigToCurrentUser_envNormalization_uatAlias() {
    when(portalConfig.isConfigViewMemberOnly("UAT")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // Test with "uat" lowercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "uat", CLUSTER, NAMESPACE)).isTrue();

    // Test with "UAT" uppercase
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "UAT", CLUSTER, NAMESPACE)).isTrue();
  }

  // ========== Boundary Value Tests ==========

  /**
   * Test empty string environment - should throw BadRequestException.
   */
  @Test
  void shouldHideConfigToCurrentUser_emptyString_throwsBadRequestException() {
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("Invalid env format");
  }

  /**
   * Test whitespace-only environment strings - should throw BadRequestException.
   */
  @Test
  void shouldHideConfigToCurrentUser_whitespaceStrings_throwsBadRequestException() {
    // Test with spaces
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "   ", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);

    // Test with tab
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "\t", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);

    // Test with newline
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "\n", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);
  }

  /**
   * Test special characters in environment name - should throw BadRequestException.
   */
  @Test
  void shouldHideConfigToCurrentUser_specialCharacters_throwsBadRequestException() {
    // Test with @ symbol
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "env@123", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);

    // Test with # symbol
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "env#test", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);

    // Test with spaces in name
    assertThatThrownBy(() -> validator.shouldHideConfigToCurrentUser(APP_ID, "env with spaces",
        CLUSTER, NAMESPACE)).isInstanceOf(BadRequestException.class);
  }

  /**
   * Test extra-long environment string - should throw BadRequestException.
   */
  @Test
  void shouldHideConfigToCurrentUser_extraLongString_throwsBadRequestException() {
    String longEnv = "A".repeat(1000);
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, longEnv, CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);
  }

  /**
   * Test mixed case variations - should normalize correctly.
   */
  @Test
  void shouldHideConfigToCurrentUser_mixedCaseVariations() {
    when(portalConfig.isConfigViewMemberOnly("PRO")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // Test "PrOd" - should normalize to "PRO"
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "PrOd", CLUSTER, NAMESPACE)).isTrue();

    when(portalConfig.isConfigViewMemberOnly("FAT")).thenReturn(true);
    // Test "FaT" - should normalize to "FAT"
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "FaT", CLUSTER, NAMESPACE)).isTrue();

    when(portalConfig.isConfigViewMemberOnly("UAT")).thenReturn(true);
    // Test "uAt" - should normalize to "UAT"
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "uAt", CLUSTER, NAMESPACE)).isTrue();
  }

  // ========== Invalid Environment Handling Tests ==========

  /**
   * Test invalid environment name - should throw BadRequestException.
   */
  @Test
  void shouldHideConfigToCurrentUser_invalidEnv_throwsBadRequestException() {
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "INVALID_ENV", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("Invalid env format");
  }

  /**
   * Test random string as environment - should throw BadRequestException.
   */
  @Test
  void shouldHideConfigToCurrentUser_randomString_throwsBadRequestException() {
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "xyz123", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);
  }

  // ========== End-to-End Scenario Tests ==========

  /**
   * Test end-to-end scenario: user configures env=prod → normalizes to PRO → permission check uses PRO.
   * This verifies the complete flow from user input to permission validation.
   */
  @Test
  void shouldHideConfigToCurrentUser_endToEnd_prodToPRO() {
    // Setup: config view is member-only for "PRO" (canonical form)
    when(portalConfig.isConfigViewMemberOnly("PRO")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // User inputs "prod" (lowercase) - system should normalize to "PRO" and apply permission check
    boolean shouldHide = validator.shouldHideConfigToCurrentUser(APP_ID, "prod", CLUSTER, NAMESPACE);

    // Verify that config is hidden (permission check passed with normalized "PRO")
    assertThat(shouldHide).isTrue();
  }

  /**
   * Test consistency: all prod variants (prod/PROD/PRO) should behave identically.
   */
  @Test
  void shouldHideConfigToCurrentUser_consistency_allProdVariants() {
    when(portalConfig.isConfigViewMemberOnly("PRO")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // All variants should produce the same result
    boolean resultProd = validator.shouldHideConfigToCurrentUser(APP_ID, "prod", CLUSTER, NAMESPACE);
    boolean resultPROD = validator.shouldHideConfigToCurrentUser(APP_ID, "PROD", CLUSTER, NAMESPACE);
    boolean resultPRO = validator.shouldHideConfigToCurrentUser(APP_ID, "PRO", CLUSTER, NAMESPACE);

    assertThat(resultProd).isEqualTo(resultPROD).isEqualTo(resultPRO).isTrue();
  }

  /**
   * Test consistency: FWS and FAT should behave identically (FWS maps to FAT).
   */
  @Test
  void shouldHideConfigToCurrentUser_consistency_fwsAndFat() {
    when(portalConfig.isConfigViewMemberOnly("FAT")).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    // FWS and FAT should produce the same result
    boolean resultFWS = validator.shouldHideConfigToCurrentUser(APP_ID, "FWS", CLUSTER, NAMESPACE);
    boolean resultFAT = validator.shouldHideConfigToCurrentUser(APP_ID, "FAT", CLUSTER, NAMESPACE);

    assertThat(resultFWS).isEqualTo(resultFAT).isTrue();
  }
}
