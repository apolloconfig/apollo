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
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.controller.GlobalDefaultExceptionHandler;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.AdminServiceAddressLocator;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.RetryableRestTemplate;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Tests a deletion and a cleanup while the adminservices run different versions. The requests that
 * the Portal sends go through its rest template to DEV, whose adminservice is a release up to 3.0.0
 * that cannot check the id of the AppNamespace it deletes, and to PRO, whose adminservice checks
 * it. Both databases compare names by case, as the default H2 schema does.
 */
@ExtendWith(MockitoExtension.class)
class AppNamespaceDeletionMixedVersionTest {

  private static final String APP_ID = "sample-app";
  private static final String NAMESPACE = "db-config";
  private static final String OTHER_NAMESPACE = "DB-Config";
  private static final String OPERATOR = "apollo";
  private static final long OTHER_ID = 6L;
  private static final long LISTED_ID = 7L;
  private static final long RECREATED_ID = 8L;
  private static final String ADMIN_SERVICE_PROFILE = "appnamespace-deletion-mixed-version-test";

  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private PortalSettings portalSettings;
  @Mock
  private ApplicationEventPublisher publisher;
  @Mock
  private RestTemplateFactory restTemplateFactory;
  @Mock
  private AdminServiceAddressLocator adminServiceAddressLocator;
  @Mock
  private PortalMetaDomainService portalMetaDomainService;
  @Mock
  private PortalConfig portalConfig;

  private final ReleasedAdminService dev = new ReleasedAdminService();
  private final UpgradedAdminService pro = new UpgradedAdminService();
  private AppNamespaceDeletionService service;

  @BeforeEach
  void setUp() {
    Map<String, ClientHttpRequestFactory> adminServices = new HashMap<>();
    adminServices.put("dev", adminService(dev));
    adminServices.put("pro", adminService(pro));
    when(restTemplateFactory.getObject()).thenReturn(new RestTemplate(
        (uri, method) -> adminServices.get(uri.getHost()).createRequest(uri, method)));
    RetryableRestTemplate restTemplate = new RetryableRestTemplate(restTemplateFactory,
        adminServiceAddressLocator, portalMetaDomainService, portalConfig);
    ReflectionTestUtils.invokeMethod(restTemplate, "postConstruct");
    AdminServiceAPI.NamespaceAPI namespaceAPI = new AdminServiceAPI.NamespaceAPI();
    ReflectionTestUtils.setField(namespaceAPI, "restTemplate", restTemplate);

    givenAdminService(Env.DEV, "http://dev");
    givenAdminService(Env.PRO, "http://pro");
    when(portalSettings.getAllEnvs()).thenReturn(Arrays.asList(Env.DEV, Env.PRO));
    service = new AppNamespaceDeletionService(appNamespaceService, portalSettings, namespaceAPI,
        publisher);
  }

  @Test
  void cleanUpShouldKeepAppNamespaceRecreatedAfterTheFinalPortalCheck() {
    dev.idsByName.put(NAMESPACE, LISTED_ID);
    pro.idsByName.put(NAMESPACE, LISTED_ID);
    // the Portal has no row at the start and right before the DEV deletion. Right after that
    // check another cleanup removes the AppNamespace and someone creates it again, so the delayed
    // deletion reaches DEV after the recreation
    AtomicInteger portalChecks = new AtomicInteger();
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenAnswer(invocation -> {
      int check = portalChecks.incrementAndGet();
      if (check == 2) {
        dev.idsByName.put(NAMESPACE, RECREATED_ID);
        pro.idsByName.put(NAMESPACE, RECREATED_ID);
      }
      return check > 2 ? portalRow() : null;
    });

    Throwable error =
        catchThrowable(() -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(dev.idsByName).containsOnly(entry(NAMESPACE, RECREATED_ID));
    assertThat(pro.idsByName).containsOnly(entry(NAMESPACE, RECREATED_ID));
    assertThat(error).isInstanceOf(BadRequestException.class)
        .hasMessage("AppNamespace sample-app:db-config exists in the Portal, so it is not cleaned"
            + " up as a leftover. It may have been recreated. Delete it normally, or check it"
            + " again.");
  }

  @Test
  void cleanUpShouldFailWithoutDeletingWhereTheAdminServiceCannotCheckTheId() {
    dev.idsByName.put(NAMESPACE, LISTED_ID);
    pro.idsByName.put(NAMESPACE, LISTED_ID);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);

    Throwable error =
        catchThrowable(() -> service.cleanUpAppNamespace(APP_ID, NAMESPACE, OPERATOR));

    assertThat(dev.idsByName).containsOnly(entry(NAMESPACE, LISTED_ID));
    assertThat(pro.idsByName).isEmpty();
    assertThat(error).isInstanceOf(ServiceException.class)
        .hasMessage("Cleaning up AppNamespace sample-app:db-config is incomplete, and some changes"
            + " may already have taken effect. Deleted in [PRO]. Failed in [DEV]. Check it again"
            + " after these environments recover.");
  }

  @Test
  void deletionShouldStillDeleteThroughEitherVersion() {
    dev.idsByName.put(NAMESPACE, LISTED_ID);
    pro.idsByName.put(NAMESPACE, LISTED_ID);
    givenPortalDeletion();

    service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    assertThat(dev.idsByName).isEmpty();
    assertThat(pro.idsByName).isEmpty();
  }

  @Test
  void deletionShouldKeepAnAppNamespaceWhoseNameDiffersOnlyInCase() {
    dev.idsByName.put(OTHER_NAMESPACE, OTHER_ID);
    dev.idsByName.put(NAMESPACE, LISTED_ID);
    pro.idsByName.put(OTHER_NAMESPACE, OTHER_ID);
    pro.idsByName.put(NAMESPACE, LISTED_ID);
    givenPortalDeletion();

    service.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR);

    assertThat(dev.idsByName).containsOnly(entry(OTHER_NAMESPACE, OTHER_ID));
    assertThat(pro.idsByName).containsOnly(entry(OTHER_NAMESPACE, OTHER_ID));
  }

  private static ClientHttpRequestFactory adminService(Object controller) {
    return new MockMvcClientHttpRequestFactory(MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalDefaultExceptionHandler()).build());
  }

  private void givenAdminService(Env env, String homepageUrl) {
    ServiceDTO adminService = new ServiceDTO();
    adminService.setHomepageUrl(homepageUrl);
    // a cleanup that stops early does not reach PRO
    lenient().when(adminServiceAddressLocator.getServiceList(env))
        .thenReturn(Collections.singletonList(adminService));
    lenient().when(portalSettings.isEnvActive(env)).thenReturn(true);
  }

  private void givenPortalDeletion() {
    when(appNamespaceService.deleteAppNamespace(APP_ID, NAMESPACE, OPERATOR))
        .thenReturn(portalRow());
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
  }

  private AppNamespace portalRow() {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId(APP_ID);
    appNamespace.setName(NAMESPACE);
    return appNamespace;
  }

  /**
   * The AppNamespaces of one environment, by names compared by case, and their list endpoint. The
   * admin services carry a profile that no test activates, so the Portal's component scan in other
   * tests leaves them out.
   */
  abstract static class AdminService {

    final Map<String, Long> idsByName = new LinkedHashMap<>();

    @GetMapping("/apps/{appId}/appnamespaces")
    public List<AppNamespaceDTO> getAppNamespaces(@PathVariable("appId") String appId) {
      List<AppNamespaceDTO> appNamespaces = new ArrayList<>();
      idsByName.forEach((name, id) -> {
        AppNamespaceDTO appNamespace = new AppNamespaceDTO();
        appNamespace.setId(id);
        appNamespace.setAppId(appId);
        appNamespace.setName(name);
        appNamespaces.add(appNamespace);
      });
      return appNamespaces;
    }

    void remove(String appId, String namespaceName, Long appNamespaceId) {
      Long id = idsByName.get(namespaceName);
      if (id == null) {
        throw BadRequestException.appNamespaceNotExists(appId, namespaceName);
      }
      if (appNamespaceId != null && !appNamespaceId.equals(id)) {
        throw new BadRequestException("appNamespace %s:%s has id %s instead of %s", appId,
            namespaceName, id, appNamespaceId);
      }
      idsByName.remove(namespaceName);
    }
  }

  /**
   * The AppNamespace delete of the adminservice as released up to 3.0.0. It has no appNamespaceId,
   * so it deletes by name whatever else the request carries.
   */
  @RestController
  @Profile(ADMIN_SERVICE_PROFILE)
  static class ReleasedAdminService extends AdminService {

    @DeleteMapping("/apps/{appId}/appnamespaces/{namespaceName:.+}")
    public void delete(@PathVariable("appId") String appId,
        @PathVariable("namespaceName") String namespaceName, @RequestParam String operator) {
      remove(appId, namespaceName, null);
    }
  }

  /**
   * The AppNamespace deletes of the adminservice of this version, which delete the AppNamespace
   * only while it has the given id.
   */
  @RestController
  @Profile(ADMIN_SERVICE_PROFILE)
  static class UpgradedAdminService extends AdminService {

    @DeleteMapping("/apps/{appId}/appnamespaces/{namespaceName:.+}")
    public void delete(@PathVariable("appId") String appId,
        @PathVariable("namespaceName") String namespaceName, @RequestParam String operator,
        @RequestParam(required = false) Long appNamespaceId) {
      remove(appId, namespaceName, appNamespaceId);
    }

    @DeleteMapping("/apps/{appId}/appnamespaces")
    public void deleteIfIdMatches(@PathVariable("appId") String appId,
        @RequestParam String namespaceName, @RequestParam long appNamespaceId,
        @RequestParam String operator) {
      remove(appId, namespaceName, appNamespaceId);
    }
  }
}
