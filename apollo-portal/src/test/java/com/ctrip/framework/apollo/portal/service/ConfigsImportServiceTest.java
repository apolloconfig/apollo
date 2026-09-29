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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.portal.environment.Env;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/** Tests import failure reporting and successful imports through the public service entry points. */
@ExtendWith(MockitoExtension.class)
class ConfigsImportServiceTest {

  private static final String APP = "qa3ui";
  private static final String CLUSTER = "default";
  private static final String NS = "application";
  private static final String OPERATOR = "apollo";
  private static final String FILE = "qa3ui+default+application.properties";

  @Mock
  private ItemService itemService;
  @Mock
  private AppService appService;
  @Mock
  private ClusterService clusterService;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private ApplicationEventPublisher publisher;
  @Mock
  private RoleInitializationService roleInitializationService;

  private ConfigsImportService service;

  @BeforeEach
  void setUp() {
    service = new ConfigsImportService(itemService, appService, clusterService, namespaceService,
        appNamespaceService, publisher, roleInitializationService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"load", "update", "create", "comment"})
  void namespaceImportMustReportFailedItemAtEachWriteStep(String step) {
    prepareNamespace();
    String contents = "[{\"key\":\"failed.key\",\"value\":\"private-value\"}]";
    switch (step) {
      case "load":
        when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "failed.key"))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        break;
      case "update":
        when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "failed.key"))
            .thenReturn(existingItem("failed.key"));
        doThrow(new BadRequestException("Rejected")).when(itemService).updateItem(eq(APP),
            eq(Env.DEV), eq(CLUSTER), eq(NS), any());
        break;
      case "create":
        when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "failed.key"))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        when(itemService.createItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), any()))
            .thenThrow(new BadRequestException("Rejected"));
        break;
      default:
        contents = "[{\"key\":\"\",\"comment\":\"private-value\",\"lineNum\":2}]";
        when(itemService.createCommentItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), any()))
            .thenThrow(new BadRequestException("Rejected"));
    }

    String input = contents;
    ServiceException failure = assertThrows(ServiceException.class, () -> importNamespace(input));

    assertThat(failure.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(failure.getMessage()).contains("Import failed for 1 resource(s)",
        step.equals("comment") ? "comment at line 2" : "failed.key",
        "Some changes may have been applied").doesNotContain("private-value");
    if (!step.equals("create")) {
      verify(itemService, never()).createItem(any(), any(), any(), any(), any());
    }
  }

  @Test
  void updateNotFoundMustNotFallBackToCreatingAnItem() {
    prepareNamespace();
    when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "removed"))
        .thenReturn(existingItem("removed"));
    doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(itemService)
        .updateItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), any());

    assertThrows(ServiceException.class,
        () -> importNamespace("[{\"key\":\"removed\",\"value\":\"new\"}]"));

    verify(itemService, never()).createItem(any(), any(), any(), any(), any());
  }

  @Test
  void partialImportReportsAllFailedKeysWhilePreservingSuccessfulWrites() {
    prepareNamespace();
    when(itemService.loadItem(eq(Env.DEV), eq(APP), eq(CLUSTER), eq(NS), anyString()))
        .thenAnswer(invocation -> {
          String key = invocation.getArgument(4);
          if (!key.equals("good")) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
          }
          return existingItem(key);
        });

    ServiceException failure = assertThrows(ServiceException.class,
        () -> importNamespace("[{\"key\":\"bad.one\"},{\"key\":\"good\",\"value\":\"saved\"},"
            + "{\"key\":\"bad.two\"}]"));

    assertThat(failure.getMessage()).contains("2 resource(s)", "bad.one", "bad.two")
        .doesNotContain("key 'good'");
    ArgumentCaptor<ItemDTO> saved = ArgumentCaptor.forClass(ItemDTO.class);
    verify(itemService).updateItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), saved.capture());
    assertThat(saved.getValue().getValue()).isEqualTo("saved");
  }

  @Test
  void successfulImportUpdatesExistingItemsAndCreatesOnlyMissingItems() {
    prepareNamespace();
    when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "existing"))
        .thenReturn(existingItem("existing"));
    when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "new"))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    importNamespace("[{\"key\":\"existing\",\"value\":\"99\",\"type\":1},"
        + "{\"key\":\"new\",\"value\":\"true\",\"type\":2},"
        + "{\"key\":\"\",\"comment\":\"note\"},{\"key\":\"\"}]");

    ArgumentCaptor<ItemDTO> updated = ArgumentCaptor.forClass(ItemDTO.class);
    verify(itemService).updateItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), updated.capture());
    assertThat(updated.getValue().getId()).isEqualTo(42L);
    assertThat(updated.getValue().getType()).isEqualTo(1);
    assertThat(updated.getValue().getNamespaceId()).isEqualTo(10L);
    assertThat(updated.getValue().getDataChangeLastModifiedBy()).isEqualTo(OPERATOR);
    ArgumentCaptor<ItemDTO> created = ArgumentCaptor.forClass(ItemDTO.class);
    verify(itemService).createItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), created.capture());
    assertThat(created.getValue().getKey()).isEqualTo("new");
    assertThat(created.getValue().getType()).isEqualTo(2);
    verify(itemService).createCommentItem(eq(APP), eq(Env.DEV), eq(CLUSTER), eq(NS), any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void zipImportsMustPropagateNamespaceFailureDetails(boolean allEnvironments) throws IOException {
    prepareNamespace();
    when(itemService.loadItem(Env.DEV, APP, CLUSTER, NS, "failed.key"))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
    String entry = (allEnvironments ? "apollo/" : "") + APP + "/DEV/" + FILE;
    try (ZipInputStream input =
        zip(Collections.singletonMap(entry, "[{\"key\":\"failed.key\",\"value\":\"ignored\"}]"))) {
      ServiceException failure;
      if (allEnvironments) {
        failure = assertThrows(ServiceException.class, () -> service
            .importDataFromZipFile(Collections.singletonList(Env.DEV), input, false, OPERATOR));
      } else {
        when(clusterService.loadCluster(APP, Env.DEV, CLUSTER)).thenReturn(new ClusterDTO());
        failure = assertThrows(ServiceException.class, () -> service.importAppConfigFromZipFile(APP,
            Env.DEV, CLUSTER, input, false, OPERATOR));
      }
      assertThat(failure.getMessage()).contains(FILE, "failed.key",
          "Some changes may have been applied");
    }
  }

  @Test
  void skipExistingNamespaceMustNotWriteItems() throws IOException {
    NamespaceDTO namespace = new NamespaceDTO();
    when(namespaceService.loadNamespaceBaseInfo(APP, Env.DEV, CLUSTER, NS)).thenReturn(namespace);
    when(itemService.findItems(APP, Env.DEV, CLUSTER, NS))
        .thenReturn(Collections.singletonList(existingItem("existing")));
    when(clusterService.loadCluster(APP, Env.DEV, CLUSTER)).thenReturn(new ClusterDTO());
    try (ZipInputStream input = zip(Collections.singletonMap(APP + "/DEV/" + FILE,
        "[{\"key\":\"existing\",\"value\":\"overwrite\"}]"))) {
      service.importAppConfigFromZipFile(APP, Env.DEV, CLUSTER, input, true, OPERATOR);
    }
    verify(itemService, never()).loadItem(any(), any(), any(), any(), any());
    verify(itemService, never()).updateItem(any(), any(), any(), any(), any());
    verify(itemService, never()).createItem(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"application", "AppNamespace", "cluster"})
  void metadataFailureMustNotBeReportedAsSuccessOrImportDependentNamespaces(String kind)
      throws IOException {
    Map<String, String> entries = new LinkedHashMap<>();
    switch (kind) {
      case "application":
        entries.put("apollo/qa3ui/app.metadata", "{\"appId\":\"qa3ui\"}");
        when(appService.importAppInLocal(any())).thenThrow(new BadRequestException("Rejected"));
        break;
      case "AppNamespace":
        entries.put("qa3ui+application.appnamespace.metadata",
            "{\"appId\":\"qa3ui\",\"name\":\"application\",\"format\":\"properties\"}");
        when(appNamespaceService.importAppNamespaceInLocal(any()))
            .thenThrow(new BadRequestException("Rejected"));
        break;
      default:
        entries.put("apollo/qa3ui/DEV/default.cluster.metadata",
            "{\"appId\":\"qa3ui\",\"name\":\"default\"}");
        when(clusterService.loadCluster(APP, Env.DEV, CLUSTER))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        when(clusterService.createCluster(eq(Env.DEV), any(), eq(OPERATOR)))
            .thenThrow(new BadRequestException("Rejected"));
    }
    entries.put("apollo/qa3ui/DEV/" + FILE, "[]");
    try (ZipInputStream input = zip(entries)) {
      ServiceException failure = assertThrows(ServiceException.class, () -> service
          .importDataFromZipFile(Collections.singletonList(Env.DEV), input, false, OPERATOR));
      assertThat(failure.getMessage()).contains(kind, "Some changes may have been applied");
    }
    verifyNoInteractions(namespaceService, itemService);
  }

  private void prepareNamespace() {
    NamespaceDTO namespace = new NamespaceDTO();
    namespace.setId(10L);
    when(namespaceService.loadNamespaceBaseInfo(APP, Env.DEV, CLUSTER, NS)).thenReturn(namespace);
  }

  private ItemDTO existingItem(String key) {
    ItemDTO item = new ItemDTO();
    item.setId(42L);
    item.setKey(key);
    return item;
  }

  private void importNamespace(String contents) {
    service.forceImportNamespaceFromFile(Env.DEV, FILE,
        new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), OPERATOR);
  }

  private ZipInputStream zip(Map<String, String> entries) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    }
    return new ZipInputStream(new ByteArrayInputStream(output.toByteArray()));
  }
}
