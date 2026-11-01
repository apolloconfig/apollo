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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.bo.AppNamespaceDeletionCheckBO;
import com.ctrip.framework.apollo.portal.entity.bo.AppNamespaceEnvDeletionStatusBO;
import com.ctrip.framework.apollo.portal.entity.bo.AppNamespaceEnvDeletionStatusBO.Status;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceDeletionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests that deleting, cleaning up and checking an AppNamespace name the environments where the
 * work is not complete, that a deletion or a cleanup stops when the AppNamespace is in the Portal
 * again, and that no environment deletes one recreated after it was listed.
 */
@ExtendWith(MockitoExtension.class)
class AppNamespaceDeletionServiceTest {

  private static final String APP_ID = "sample-app";
  private static final String NAMESPACE = "db-config";
  private static final String OPERATOR = "apollo";
  // the id of the AppNamespace that an environment lists, unless a test sets another one
  private static final long LISTED_ID = 7L;
  private static final long RECREATED_ID = 8L;
  private static final String UNRESPONSIVE = "Admin servers are unresponsive.";
  private static final String DELETION_INCOMPLETE = "Deleting AppNamespace sample-app:db-config "
      + "is incomplete, and some changes have already taken effect. It was removed from the Portal.";
  private static final String CHECK_AND_CLEAN_UP = " After these environments recover, check the "
      + "AppNamespace on the delete management page and clean up what remains.";
  private static final String RECREATED_DURING_DELETION = "Deleting AppNamespace "
      + "sample-app:db-config stopped because the AppNamespace was recreated in the Portal during "
      + "the deletion, and some changes have already taken effect. It was removed from the Portal.";
  private static final String CLEANUP_INCOMPLETE = "Cleaning up AppNamespace sample-app:db-config "
      + "is incomplete, and some changes may already have taken effect.";
  private static final String CHECK_AGAIN = " Check it again after these environments recover.";
  private static final String EXISTS_IN_PORTAL = "AppNamespace sample-app:db-config exists in the "
      + "Portal, so it is not cleaned up as a leftover. It may have been recreated. "
      + "Delete it normally, or check it again.";

  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private PortalSettings portalSettings;
  @Mock
  private AdminServiceAPI.NamespaceAPI namespaceAPI;
  @Mock
  private ApplicationEventPublisher publisher;

  private AppNamespaceDeletionService service;

  @BeforeEach
  void setUp() {
    service = new AppNamespaceDeletionService(appNamespaceService, portalSettings, namespaceAPI,
        publisher);
  }

  @Test
  void shouldDeleteInPortalAndEveryEnvironment() {
    AppNamespace appNamespace = givenPortalDeletion();
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    givenListed(Env.DEV, NAMESPACE);
    givenListed(Env.PRO, NAMESPACE);

    service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    verify(appNamespaceService).deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR);
    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI).deleteAppNamespace(Env.PRO, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    assertThat(publishedAppNamespace()).isSameAs(appNamespace);
  }

  @Test
  void shouldNameEnvironmentWhoseAdminServiceFails() {
    AppNamespace appNamespace = givenPortalDeletion();
    givenEnvs(Env.DEV);
    givenActive(Env.DEV);
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.DEV))
        .thenThrow(new ServiceException(UNRESPONSIVE));

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage())
        .isEqualTo(DELETION_INCOMPLETE + " Failed in [DEV]." + CHECK_AND_CLEAN_UP);
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
    assertThat(publishedAppNamespace()).isSameAs(appNamespace);
  }

  @Test
  void shouldSkipAndNameEnvironmentMarkedDown() {
    givenPortalDeletion();
    givenEnvs(Env.DEV, Env.FAT);
    givenActive(Env.DEV);
    when(portalSettings.isEnvActive(Env.FAT)).thenReturn(false);
    givenListed(Env.DEV, NAMESPACE);

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(DELETION_INCOMPLETE + " Deleted in [DEV]."
        + " Skipped because the health check marks them down: [FAT]." + CHECK_AND_CLEAN_UP);
    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI, never()).getAppNamespaces(APP_ID, Env.FAT);
    verify(namespaceAPI, never()).deleteAppNamespace(eq(Env.FAT), anyString(), anyString(),
        anyLong(), anyString());
  }

  @Test
  void shouldNameDeletedFailedAndSkippedEnvironmentsAfterPartialDeletion() {
    givenPortalDeletion();
    givenEnvs(Env.DEV, Env.FAT, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    when(portalSettings.isEnvActive(Env.FAT)).thenReturn(false);
    givenListed(Env.DEV, NAMESPACE);
    givenListed(Env.PRO, NAMESPACE);
    doThrow(new ServiceException(UNRESPONSIVE)).when(namespaceAPI).deleteAppNamespace(Env.DEV,
        APP_ID, NAMESPACE, LISTED_ID, OPERATOR);

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(
        "Deleting AppNamespace sample-app:db-config is incomplete, and some changes have already "
            + "taken effect. It was removed from the Portal. Deleted in [PRO]. Failed in [DEV]. "
            + "Skipped because the health check marks them down: [FAT]. After these environments "
            + "recover, check the AppNamespace on the delete management page and clean up what "
            + "remains.");
    verify(namespaceAPI).deleteAppNamespace(Env.PRO, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
  }

  @Test
  void shouldRejectDeletionOfAppNamespaceMissingInPortalWithoutCallingEnvironments() {
    when(appNamespaceService.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR))
        .thenThrow(BadRequestException.appNamespaceNotExists(APP_ID, NAMESPACE));

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage())
        .isEqualTo("appNamespace not exist for appId:sample-app namespaceName:db-config");
    verifyNoInteractions(portalSettings, namespaceAPI, publisher);
  }

  @Test
  void shouldCleanUpEnvironmentThatWasDownDuringDeletionAfterAFreshCheck() {
    givenPortalDeletion();
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.DEV))
        .thenThrow(new ServiceException(UNRESPONSIVE)).thenReturn(listing(NAMESPACE));
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.PRO)).thenReturn(listing(NAMESPACE))
        .thenReturn(listing("application"));

    ServiceException deletionError = assertThrows(ServiceException.class,
        () -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));
    assertThat(deletionError.getMessage()).isEqualTo(
        DELETION_INCOMPLETE + " Deleted in [PRO]. Failed in [DEV]." + CHECK_AND_CLEAN_UP);

    // the Portal row is gone, so the lookup of the mocked AppNamespaceService answers null
    AppNamespaceDeletionCheckBO check = service.checkAppNamespaceDeletion(APP_ID, NAMESPACE);
    assertThat(check.isExistsInPortal()).isFalse();
    assertThat(check.getEnvironments())
        .extracting(AppNamespaceEnvDeletionStatusBO::getEnv,
            AppNamespaceEnvDeletionStatusBO::getStatus)
        .containsExactly(tuple(Env.DEV, Status.REMAINING), tuple(Env.PRO, Status.ABSENT));

    service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI, times(1)).deleteAppNamespace(Env.PRO, APP_ID, NAMESPACE, LISTED_ID,
        OPERATOR);
    verify(appNamespaceService, times(1)).deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR);
    verify(publisher, times(1)).publishEvent(any(AppNamespaceDeletionEvent.class));
  }

  @Test
  void cleanUpShouldRejectAppNamespaceMissingInPortalAndEveryEnvironment() {
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    givenListed(Env.DEV, "application");
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.PRO)).thenReturn(listing());

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage())
        .isEqualTo("appNamespace not exist for appId:sample-app namespaceName:db-config");
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
    verifyNoInteractions(publisher);
  }

  @Test
  void cleanUpShouldAnswerIncompleteInsteadOfMissingWhenAnEnvironmentIsSkipped() {
    givenEnvs(Env.DEV, Env.FAT);
    givenActive(Env.DEV);
    when(portalSettings.isEnvActive(Env.FAT)).thenReturn(false);
    givenListed(Env.DEV, "application");

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(CLEANUP_INCOMPLETE
        + " Skipped because the health check marks them down: [FAT]." + CHECK_AGAIN);
    verify(namespaceAPI, never()).getAppNamespaces(APP_ID, Env.FAT);
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
  }

  @Test
  void cleanUpShouldAnswerIncompleteInsteadOfMissingWhenAnEnvironmentIsUnreachable() {
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.DEV))
        .thenThrow(new ServiceException(UNRESPONSIVE));
    givenListed(Env.PRO, "application");

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage())
        .isEqualTo(CLEANUP_INCOMPLETE + " Failed in [DEV]." + CHECK_AGAIN);
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
  }

  @Test
  void cleanUpShouldNameDeletedFailedAndSkippedEnvironments() {
    givenEnvs(Env.DEV, Env.FAT, Env.UAT);
    givenActive(Env.DEV, Env.FAT);
    when(portalSettings.isEnvActive(Env.UAT)).thenReturn(false);
    givenListed(Env.DEV, NAMESPACE);
    givenListed(Env.FAT, NAMESPACE);
    doNothing().when(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID,
        OPERATOR);
    doThrow(new ServiceException(UNRESPONSIVE)).when(namespaceAPI).deleteAppNamespace(Env.FAT,
        APP_ID, NAMESPACE, LISTED_ID, OPERATOR);

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(
        "Cleaning up AppNamespace sample-app:db-config is incomplete, and some changes may already "
            + "have taken effect. Deleted in [DEV]. Failed in [FAT]. Skipped because the health "
            + "check marks them down: [UAT]. Check it again after these environments recover.");
    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verifyNoInteractions(publisher);
  }

  @Test
  void cleanUpShouldRejectAppNamespaceInPortalWithoutCallingEnvironments() {
    givenPortalRow();

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(EXISTS_IN_PORTAL);
    verifyNoInteractions(portalSettings, namespaceAPI, publisher);
    verify(appNamespaceService, never()).deleteAppNamespace(anyString(), anyString(), anyString());
  }

  @Test
  void cleanUpShouldStopWhenAppNamespaceIsRecreatedInPortalDuringTheCleanup() {
    givenEnvs(Env.DEV, Env.FAT, Env.PRO);
    givenActive(Env.DEV, Env.FAT);
    givenListed(Env.DEV, NAMESPACE);
    givenListed(Env.FAT, NAMESPACE);
    // absent at the start and before the DEV deletion, recreated before the FAT deletion
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null, null,
        portalRow());

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(
        "Cleaning up AppNamespace sample-app:db-config stopped because the AppNamespace was "
            + "recreated in the Portal during the cleanup, and some changes may already have "
            + "taken effect. Deleted in [DEV]. The remaining environments were not changed: "
            + "[FAT, PRO].");
    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI, never()).deleteAppNamespace(eq(Env.FAT), anyString(), anyString(),
        anyLong(), anyString());
    verify(portalSettings, never()).isEnvActive(Env.PRO);
    verify(namespaceAPI, never()).getAppNamespaces(APP_ID, Env.PRO);
    verifyNoInteractions(publisher);
  }

  @Test
  void cleanUpShouldRejectAppNamespaceRecreatedInPortalBeforeAnythingWasDeleted() {
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV);
    givenListed(Env.DEV, NAMESPACE);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null, portalRow());

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(EXISTS_IN_PORTAL);
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
    verify(namespaceAPI, never()).getAppNamespaces(APP_ID, Env.PRO);
  }

  @Test
  void cleanUpShouldCountEnvironmentCleanedUpConcurrentlyAsDone() {
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    givenListed(Env.DEV, NAMESPACE);
    // PRO still has it when the page checks, and another cleanup removes it before this one
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.PRO)).thenReturn(listing(NAMESPACE))
        .thenReturn(listing("application"));

    assertThat(service.checkAppNamespaceDeletion(APP_ID, NAMESPACE).getEnvironments())
        .extracting(AppNamespaceEnvDeletionStatusBO::getStatus)
        .containsExactly(Status.REMAINING, Status.REMAINING);

    service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI, never()).deleteAppNamespace(eq(Env.PRO), anyString(), anyString(),
        anyLong(), anyString());
    verifyNoInteractions(publisher);
  }

  @Test
  void checkShouldReportEachStatusWithoutChangingAnything() {
    givenEnvs(Env.DEV, Env.FAT, Env.UAT, Env.PRO);
    givenActive(Env.DEV, Env.FAT, Env.UAT);
    when(portalSettings.isEnvActive(Env.PRO)).thenReturn(false);
    givenListed(Env.DEV, NAMESPACE);
    givenListed(Env.FAT, "application");
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.UAT))
        .thenThrow(new ServiceException(UNRESPONSIVE));

    AppNamespaceDeletionCheckBO check = service.checkAppNamespaceDeletion(APP_ID, NAMESPACE);

    assertThat(check.getAppId()).isEqualTo(APP_ID);
    assertThat(check.getNamespaceName()).isEqualTo(NAMESPACE);
    assertThat(check.isExistsInPortal()).isFalse();
    assertThat(check.getEnvironments())
        .extracting(AppNamespaceEnvDeletionStatusBO::getEnv,
            AppNamespaceEnvDeletionStatusBO::getStatus, AppNamespaceEnvDeletionStatusBO::getMessage)
        .containsExactly(tuple(Env.DEV, Status.REMAINING, null),
            tuple(Env.FAT, Status.ABSENT, null), tuple(Env.UAT, Status.UNREACHABLE, UNRESPONSIVE),
            tuple(Env.PRO, Status.SKIPPED, null));
    verify(namespaceAPI, never()).getAppNamespaces(APP_ID, Env.PRO);
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
    verify(appNamespaceService, never()).deleteAppNamespace(anyString(), anyString(), anyString());
    verifyNoInteractions(publisher);
  }

  @Test
  void checkShouldListEnvironmentsWhenAppNamespaceExistsInPortal() {
    givenPortalRow();
    givenEnvs(Env.DEV);
    givenActive(Env.DEV);
    givenListed(Env.DEV, NAMESPACE);

    AppNamespaceDeletionCheckBO check = service.checkAppNamespaceDeletion(APP_ID, NAMESPACE);

    assertThat(check.isExistsInPortal()).isTrue();
    assertThat(check.getEnvironments())
        .extracting(AppNamespaceEnvDeletionStatusBO::getEnv,
            AppNamespaceEnvDeletionStatusBO::getStatus)
        .containsExactly(tuple(Env.DEV, Status.REMAINING));
    verify(namespaceAPI, never()).deleteAppNamespace(any(), anyString(), anyString(), anyLong(),
        anyString());
    verify(appNamespaceService, never()).deleteAppNamespace(anyString(), anyString(), anyString());
    verifyNoInteractions(publisher);
  }

  @Test
  void cleanUpShouldDeleteTheNameTheEnvironmentStoresIgnoringCase() {
    givenEnvs(Env.DEV);
    givenActive(Env.DEV);
    givenListed(Env.DEV, "DB-Config");

    service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, "DB-Config", LISTED_ID, OPERATOR);
  }

  @Test
  void cleanUpShouldCountEnvironmentEmptiedBetweenListingAndDeleteAsDone() {
    givenEnvs(Env.DEV);
    givenActive(Env.DEV);
    // another cleanup removes it after this one listed it, so the delete call fails
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.DEV)).thenReturn(listing(NAMESPACE))
        .thenReturn(listing());
    doThrow(new BadRequestException("appNamespace not exist")).when(namespaceAPI)
        .deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage())
        .isEqualTo("appNamespace not exist for appId:sample-app namespaceName:db-config");
  }

  @Test
  void cleanUpShouldNotDeleteAppNamespaceRecreatedAfterTheFinalLookup() {
    givenEnvs(Env.DEV);
    givenActive(Env.DEV);
    AtomicLong idInDev = givenDevRecreatesItWhenTheDeletionArrives();
    // absent at the start and before the DEV deletion, recreated while the deletion is on its way
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null, null,
        portalRow());

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(EXISTS_IN_PORTAL);
    assertThat(idInDev.get()).isEqualTo(RECREATED_ID);
    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verifyNoInteractions(publisher);
  }

  @Test
  void deletionShouldKeepAppNamespaceRecreatedInAnEnvironmentAfterTheFinalLookup() {
    AppNamespace appNamespace = givenPortalDeletion();
    givenEnvs(Env.DEV);
    givenActive(Env.DEV);
    AtomicLong idInDev = givenDevRecreatesItWhenTheDeletionArrives();
    // absent before the DEV deletion, recreated while the deletion is on its way
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null, portalRow());

    ServiceException error = assertThrows(ServiceException.class,
        () -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(error.getMessage()).isEqualTo(
        RECREATED_DURING_DELETION + " The remaining environments were not changed: [DEV].");
    assertThat(idInDev.get()).isEqualTo(RECREATED_ID);
    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    assertThat(publishedAppNamespace()).isSameAs(appNamespace);
  }

  @Test
  void deletionShouldStopWhenAppNamespaceIsRecreatedBeforeItReachesAnEnvironment() {
    AppNamespace appNamespace = givenPortalDeletion();
    givenEnvs(Env.DEV, Env.PRO);
    givenActive(Env.DEV, Env.PRO);
    givenListed(Env.DEV, NAMESPACE);
    AtomicLong idInPro = new AtomicLong(LISTED_ID);
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.PRO))
        .thenAnswer(invocation -> Collections.singletonList(listed(NAMESPACE, idInPro.get())));
    AtomicReference<AppNamespace> inPortal = new AtomicReference<>();
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE))
        .thenAnswer(invocation -> inPortal.get());
    // while this slow deletion is busy in DEV, another user cleans up PRO and recreates the
    // AppNamespace, in the Portal and in PRO with another id
    doAnswer(invocation -> {
      idInPro.set(RECREATED_ID);
      inPortal.set(portalRow());
      return null;
    }).when(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);

    Throwable error = catchThrowable(() -> service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    // PRO lists the recreated AppNamespace, and the deletion stops before deleting it
    verify(namespaceAPI, never()).deleteAppNamespace(eq(Env.PRO), anyString(), anyString(),
        anyLong(), anyString());
    assertThat(error).isInstanceOf(ServiceException.class).hasMessage(RECREATED_DURING_DELETION
        + " Deleted in [DEV]. The remaining environments were not changed: [PRO].");
    assertThat(publishedAppNamespace()).isSameAs(appNamespace);
  }

  @Test
  void cleanUpShouldAlsoCleanEnvironmentsThatRecoverAfterTheCheck() {
    givenEnvs(Env.DEV, Env.FAT, Env.PRO);
    givenActive(Env.DEV, Env.FAT);
    // FAT is unreachable and PRO is marked down when the page checks, and both recover before the
    // cleanup, which checks every environment again
    when(portalSettings.isEnvActive(Env.PRO)).thenReturn(false, true);
    givenListed(Env.DEV, NAMESPACE);
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.FAT))
        .thenThrow(new ServiceException(UNRESPONSIVE)).thenReturn(listing(NAMESPACE));
    givenListed(Env.PRO, NAMESPACE);

    assertThat(service.checkAppNamespaceDeletion(APP_ID, NAMESPACE).getEnvironments())
        .extracting(AppNamespaceEnvDeletionStatusBO::getEnv,
            AppNamespaceEnvDeletionStatusBO::getStatus)
        .containsExactly(tuple(Env.DEV, Status.REMAINING), tuple(Env.FAT, Status.UNREACHABLE),
            tuple(Env.PRO, Status.SKIPPED));

    service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    verify(namespaceAPI).deleteAppNamespace(Env.DEV, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI).deleteAppNamespace(Env.FAT, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verify(namespaceAPI).deleteAppNamespace(Env.PRO, APP_ID, NAMESPACE, LISTED_ID, OPERATOR);
    verifyNoInteractions(publisher);
  }

  private AppNamespace givenPortalDeletion() {
    AppNamespace appNamespace = portalRow();
    when(appNamespaceService.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR))
        .thenReturn(appNamespace);
    return appNamespace;
  }

  private void givenPortalRow() {
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(portalRow());
  }

  private AppNamespace portalRow() {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId(APP_ID);
    appNamespace.setName(NAMESPACE);
    return appNamespace;
  }

  private void givenEnvs(Env... envs) {
    when(portalSettings.getAllEnvs()).thenReturn(Arrays.asList(envs));
  }

  private void givenActive(Env... envs) {
    for (Env env : envs) {
      when(portalSettings.isEnvActive(env)).thenReturn(true);
    }
  }

  private void givenListed(Env env, String namespaceName) {
    when(namespaceAPI.getAppNamespaces(APP_ID, env)).thenReturn(listing(namespaceName));
  }

  /**
   * Makes DEV list the AppNamespace it has, and recreate it with RECREATED_ID when a deletion
   * arrives, which is after the caller's final lookup, as if another cleanup removed it and someone
   * created it again. DEV then deletes only the AppNamespace with the requested id, as the admin
   * service does. The returned value is the id that DEV has, 0 when it has none.
   */
  private AtomicLong givenDevRecreatesItWhenTheDeletionArrives() {
    AtomicLong idInDev = new AtomicLong(LISTED_ID);
    when(namespaceAPI.getAppNamespaces(APP_ID, Env.DEV))
        .thenAnswer(invocation -> Collections.singletonList(listed(NAMESPACE, idInDev.get())));
    doAnswer(invocation -> {
      idInDev.set(RECREATED_ID);
      long requestedId = invocation.getArgument(3);
      if (requestedId != idInDev.get()) {
        throw new BadRequestException(
            "appNamespace %s:%s has id %s instead of %s, it may have" + " been recreated", APP_ID,
            NAMESPACE, idInDev.get(), requestedId);
      }
      idInDev.set(0);
      return null;
    }).when(namespaceAPI).deleteAppNamespace(eq(Env.DEV), eq(APP_ID), eq(NAMESPACE), anyLong(),
        eq(OPERATOR));
    return idInDev;
  }

  private List<AppNamespaceDTO> listing(String... namespaceNames) {
    List<AppNamespaceDTO> appNamespaces = new ArrayList<>();
    for (String namespaceName : namespaceNames) {
      appNamespaces.add(listed(namespaceName, LISTED_ID));
    }
    return appNamespaces;
  }

  private AppNamespaceDTO listed(String namespaceName, long id) {
    AppNamespaceDTO appNamespace = new AppNamespaceDTO();
    appNamespace.setId(id);
    appNamespace.setAppId(APP_ID);
    appNamespace.setName(namespaceName);
    return appNamespace;
  }

  private AppNamespace publishedAppNamespace() {
    ArgumentCaptor<AppNamespaceDeletionEvent> event =
        ArgumentCaptor.forClass(AppNamespaceDeletionEvent.class);
    verify(publisher).publishEvent(event.capture());
    return event.getValue().getAppNamespace();
  }
}
