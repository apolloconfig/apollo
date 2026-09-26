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
package com.ctrip.framework.apollo.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.repository.AppNamespaceRepository;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Tests the database length boundary after namespace prefixes and suffixes are applied. */
@ExtendWith(MockitoExtension.class)
class AppNamespaceServiceValidationTest {

  @Mock
  private AppNamespaceRepository repository;
  @Mock
  private RoleInitializationService roleInitializationService;
  @Mock
  private AppService appService;
  @Mock
  private RolePermissionService rolePermissionService;

  private AppNamespaceService service;

  @BeforeEach
  void setUp() {
    service = new AppNamespaceService(repository, roleInitializationService, appService,
        rolePermissionService);
  }

  @ParameterizedTest
  @MethodSource("namespaceNames")
  void shouldAcceptFinalNameAtDatabaseLimit(boolean isPublic, boolean appendPrefix, String format,
      int baseLength, String prefix, String suffix) {
    stubApp();
    AppNamespace namespace = namespace("n".repeat(baseLength), isPublic, format);
    when(repository.save(namespace)).thenReturn(namespace);

    AppNamespace created = service.createAppNamespaceInLocal(namespace, appendPrefix, "operator");

    assertThat(created.getName()).isEqualTo(prefix + "n".repeat(baseLength) + suffix).hasSize(32);
    verify(repository).save(namespace);
    verify(roleInitializationService).initNamespaceRoles("app", created.getName(), "operator");
  }

  @ParameterizedTest
  @MethodSource("namespaceNames")
  void shouldRejectFinalNameOverDatabaseLimitBeforePersistence(boolean isPublic,
      boolean appendPrefix, String format, int baseLength, String prefix, String suffix) {
    stubApp();
    AppNamespace namespace = namespace("n".repeat(baseLength + 1), isPublic, format);

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.createAppNamespaceInLocal(namespace, appendPrefix, "operator"));

    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(error.getMessage()).contains("32", "prefix", "suffix");
    verify(repository, never()).save(any());
    verifyNoInteractions(roleInitializationService);
  }

  @Test
  void shouldRejectOversizedImportedNameBeforePersistence() {
    AppNamespace namespace = namespace("n".repeat(33), false, "properties");

    BadRequestException error =
        assertThrows(BadRequestException.class, () -> service.importAppNamespaceInLocal(namespace));

    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(repository, never()).save(any());
    verifyNoInteractions(roleInitializationService);
  }

  @Test
  void shouldImportNameAtDatabaseLimitWithoutAddingPrefixOrSuffix() {
    String name = "n".repeat(27) + ".json";
    AppNamespace namespace = namespace(name, true, "json");
    when(repository.save(namespace)).thenReturn(namespace);

    AppNamespace imported = service.importAppNamespaceInLocal(namespace);

    assertThat(imported.getName()).isEqualTo(name).hasSize(32);
    verify(repository).save(namespace);
    verifyNoInteractions(appService);
  }

  private void stubApp() {
    App app = new App();
    app.setAppId("app");
    app.setOrgId("TEST1");
    when(appService.load("app")).thenReturn(app);
  }

  private AppNamespace namespace(String name, boolean isPublic, String format) {
    AppNamespace namespace = new AppNamespace();
    namespace.setAppId("app");
    namespace.setName(name);
    namespace.setPublic(isPublic);
    namespace.setFormat(format);
    namespace.setDataChangeCreatedBy("operator");
    return namespace;
  }

  private static Stream<Arguments> namespaceNames() {
    return Stream.of(Arguments.of(false, true, "properties", 32, "", ""),
        Arguments.of(true, true, "properties", 26, "TEST1.", ""),
        Arguments.of(true, false, "properties", 32, "", ""),
        Arguments.of(false, true, "json", 27, "", ".json"),
        Arguments.of(true, true, "json", 21, "TEST1.", ".json"),
        Arguments.of(true, false, "json", 27, "", ".json"),
        Arguments.of(false, true, "yaml", 27, "", ".yaml"),
        Arguments.of(false, true, "yml", 28, "", ".yml"),
        Arguments.of(false, true, "xml", 28, "", ".xml"));
  }
}
