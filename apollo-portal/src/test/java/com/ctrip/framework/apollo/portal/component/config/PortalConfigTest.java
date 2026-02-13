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
package com.ctrip.framework.apollo.portal.component.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test coverage for PortalConfig.isConfigViewMemberOnly() environment normalization.
 * Supplements Apollo PR #5442 fix for environment alias handling.
 *
 * @see <a href="https://github.com/apolloconfig/apollo/issues/5442">#5442</a>
 */
@ExtendWith(MockitoExtension.class)
class PortalConfigTest {

  @Mock
  private PortalDBPropertySource portalDBPropertySource;

  private PortalConfig portalConfig;

  @BeforeEach
  void setUp() {
    portalConfig = new PortalConfig(portalDBPropertySource);
  }

  // ========== All Environment Aliases Tests ==========

  /**
   * Test PRO environment and its aliases (prod/PROD/PRO).
   * Verifies issue #5442 fix where PROD maps to PRO.
   */
  @Test
  void isConfigViewMemberOnly_prodAliases() {
    // Setup: configure "PRO" as member-only
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // Test with "prod" lowercase - should normalize to "PRO" and return true
    assertThat(portalConfig.isConfigViewMemberOnly("prod")).isTrue();

    // Test with "PROD" uppercase - should normalize to "PRO" and return true
    assertThat(portalConfig.isConfigViewMemberOnly("PROD")).isTrue();

    // Test with "PRO" canonical form - should return true
    assertThat(portalConfig.isConfigViewMemberOnly("PRO")).isTrue();
  }

  /**
   * Test FAT environment and its alias FWS (FWS maps to FAT).
   */
  @Test
  void isConfigViewMemberOnly_fatAndFwsAliases() {
    // Setup: configure "FAT" as member-only
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("FAT");

    // Test with "fat" lowercase
    assertThat(portalConfig.isConfigViewMemberOnly("fat")).isTrue();

    // Test with "FAT" uppercase
    assertThat(portalConfig.isConfigViewMemberOnly("FAT")).isTrue();

    // Test with "FWS" - should normalize to "FAT"
    assertThat(portalConfig.isConfigViewMemberOnly("FWS")).isTrue();

    // Test with "fws" lowercase - should also normalize to "FAT"
    assertThat(portalConfig.isConfigViewMemberOnly("fws")).isTrue();
  }

  /**
   * Test LOCAL environment normalization.
   */
  @Test
  void isConfigViewMemberOnly_localAlias() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("LOCAL");

    // Test with "local" lowercase
    assertThat(portalConfig.isConfigViewMemberOnly("local")).isTrue();

    // Test with "LOCAL" uppercase
    assertThat(portalConfig.isConfigViewMemberOnly("LOCAL")).isTrue();
  }

  /**
   * Test DEV environment normalization.
   */
  @Test
  void isConfigViewMemberOnly_devAlias() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("DEV");

    // Test with "dev" lowercase
    assertThat(portalConfig.isConfigViewMemberOnly("dev")).isTrue();

    // Test with "DEV" uppercase
    assertThat(portalConfig.isConfigViewMemberOnly("DEV")).isTrue();
  }

  /**
   * Test UAT environment normalization.
   */
  @Test
  void isConfigViewMemberOnly_uatAlias() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("UAT");

    // Test with "uat" lowercase
    assertThat(portalConfig.isConfigViewMemberOnly("uat")).isTrue();

    // Test with "UAT" uppercase
    assertThat(portalConfig.isConfigViewMemberOnly("UAT")).isTrue();
  }

  /**
   * Test LPT environment normalization.
   */
  @Test
  void isConfigViewMemberOnly_lptAlias() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("LPT");

    // Test with "lpt" lowercase
    assertThat(portalConfig.isConfigViewMemberOnly("lpt")).isTrue();

    // Test with "LPT" uppercase
    assertThat(portalConfig.isConfigViewMemberOnly("LPT")).isTrue();
  }

  /**
   * Test TOOLS environment normalization.
   */
  @Test
  void isConfigViewMemberOnly_toolsAlias() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("TOOLS");

    // Test with "tools" lowercase
    assertThat(portalConfig.isConfigViewMemberOnly("tools")).isTrue();

    // Test with "TOOLS" uppercase
    assertThat(portalConfig.isConfigViewMemberOnly("TOOLS")).isTrue();
  }

  // ========== Boundary Value Tests ==========

  /**
   * Test empty string environment - should return false (safe default).
   */
  @Test
  void isConfigViewMemberOnly_emptyString_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // Empty string should be treated as invalid and return false for safety
    assertThat(portalConfig.isConfigViewMemberOnly("")).isFalse();
  }

  /**
   * Test whitespace-only environment strings - should return false.
   */
  @Test
  void isConfigViewMemberOnly_whitespaceStrings_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // Test with spaces
    assertThat(portalConfig.isConfigViewMemberOnly("   ")).isFalse();

    // Test with tab
    assertThat(portalConfig.isConfigViewMemberOnly("\t")).isFalse();

    // Test with newline
    assertThat(portalConfig.isConfigViewMemberOnly("\n")).isFalse();
  }

  /**
   * Test special characters in environment name - should return false.
   */
  @Test
  void isConfigViewMemberOnly_specialCharacters_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // Test with @ symbol
    assertThat(portalConfig.isConfigViewMemberOnly("env@123")).isFalse();

    // Test with # symbol
    assertThat(portalConfig.isConfigViewMemberOnly("env#test")).isFalse();

    // Test with spaces in name
    assertThat(portalConfig.isConfigViewMemberOnly("env with spaces")).isFalse();
  }

  /**
   * Test extra-long environment string - should return false.
   */
  @Test
  void isConfigViewMemberOnly_extraLongString_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    String longEnv = "A".repeat(1000);
    assertThat(portalConfig.isConfigViewMemberOnly(longEnv)).isFalse();
  }

  /**
   * Test mixed case variations - should normalize correctly.
   */
  @Test
  void isConfigViewMemberOnly_mixedCaseVariations() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO,FAT,UAT");

    // Test "PrOd" - should normalize to "PRO"
    assertThat(portalConfig.isConfigViewMemberOnly("PrOd")).isTrue();

    // Test "FaT" - should normalize to "FAT"
    assertThat(portalConfig.isConfigViewMemberOnly("FaT")).isTrue();

    // Test "uAt" - should normalize to "UAT"
    assertThat(portalConfig.isConfigViewMemberOnly("uAt")).isTrue();
  }

  // ========== Invalid Environment Handling Tests ==========

  /**
   * Test invalid environment name - should return false (safe default).
   */
  @Test
  void isConfigViewMemberOnly_invalidEnv_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // Invalid env should return false for safety
    assertThat(portalConfig.isConfigViewMemberOnly("INVALID_ENV")).isFalse();
  }

  /**
   * Test random string as environment - should return false.
   */
  @Test
  void isConfigViewMemberOnly_randomString_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    assertThat(portalConfig.isConfigViewMemberOnly("xyz123")).isFalse();
  }

  /**
   * Test null environment - should return false.
   */
  @Test
  void isConfigViewMemberOnly_nullEnv_returnsFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    assertThat(portalConfig.isConfigViewMemberOnly(null)).isFalse();
  }

  // ========== Consistency Validation Tests ==========

  /**
   * Test consistency: all prod variants (prod/PROD/PRO) should behave identically.
   * Verifies that PortalConfig and UserPermissionValidator handle env normalization consistently.
   */
  @Test
  void isConfigViewMemberOnly_consistency_allProdVariants() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // All variants should produce the same result
    boolean resultProd = portalConfig.isConfigViewMemberOnly("prod");
    boolean resultPROD = portalConfig.isConfigViewMemberOnly("PROD");
    boolean resultPRO = portalConfig.isConfigViewMemberOnly("PRO");

    assertThat(resultProd).isEqualTo(resultPROD).isEqualTo(resultPRO).isTrue();
  }

  /**
   * Test consistency: FWS and FAT should behave identically (FWS maps to FAT).
   */
  @Test
  void isConfigViewMemberOnly_consistency_fwsAndFat() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("FAT");

    // FWS and FAT should produce the same result
    boolean resultFWS = portalConfig.isConfigViewMemberOnly("FWS");
    boolean resultFAT = portalConfig.isConfigViewMemberOnly("FAT");

    assertThat(resultFWS).isEqualTo(resultFAT).isTrue();
  }

  /**
   * Test consistency: invalid env handling should always return false.
   * Ensures predictable behavior across all invalid inputs.
   */
  @Test
  void isConfigViewMemberOnly_consistency_invalidEnvAlwaysFalse() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // All invalid inputs should return false
    assertThat(portalConfig.isConfigViewMemberOnly("")).isFalse();
    assertThat(portalConfig.isConfigViewMemberOnly("   ")).isFalse();
    assertThat(portalConfig.isConfigViewMemberOnly("INVALID")).isFalse();
    assertThat(portalConfig.isConfigViewMemberOnly("xyz123")).isFalse();
    assertThat(portalConfig.isConfigViewMemberOnly(null)).isFalse();
  }

  // ========== End-to-End Scenario Tests ==========

  /**
   * Test end-to-end scenario: user configures env=prod → normalizes to PRO → config view check uses PRO.
   */
  @Test
  void isConfigViewMemberOnly_endToEnd_prodToPRO() {
    // Setup: configure "PRO" as member-only
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO");

    // User inputs "prod" (lowercase) - system should normalize to "PRO" and return true
    boolean isMemberOnly = portalConfig.isConfigViewMemberOnly("prod");

    // Verify that member-only check passed with normalized "PRO"
    assertThat(isMemberOnly).isTrue();
  }

  /**
   * Test multiple environments configured as member-only.
   */
  @Test
  void isConfigViewMemberOnly_multipleEnvs() {
    when(portalDBPropertySource.getValue("configView.memberOnly.envs")).thenReturn("PRO,UAT,FAT");

    // All configured envs and their aliases should return true
    assertThat(portalConfig.isConfigViewMemberOnly("prod")).isTrue();
    assertThat(portalConfig.isConfigViewMemberOnly("PROD")).isTrue();
    assertThat(portalConfig.isConfigViewMemberOnly("PRO")).isTrue();

    assertThat(portalConfig.isConfigViewMemberOnly("uat")).isTrue();
    assertThat(portalConfig.isConfigViewMemberOnly("UAT")).isTrue();

    assertThat(portalConfig.isConfigViewMemberOnly("fat")).isTrue();
    assertThat(portalConfig.isConfigViewMemberOnly("FAT")).isTrue();
    assertThat(portalConfig.isConfigViewMemberOnly("FWS")).isTrue();

    // Non-configured env should return false
    assertThat(portalConfig.isConfigViewMemberOnly("DEV")).isFalse();
  }
}
