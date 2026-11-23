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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.entity.vo.Number;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated Portal UI uses /openapi/v1 endpoints. This legacy WebAPI controller is kept for
 *     compatibility.
 */
@Deprecated
@RestController
public class InstanceController {

  private static final Splitter RELEASES_SPLITTER =
      Splitter.on(",").omitEmptyStrings().trimResults();

  /**
   * Fallback page size used when an explicit size of zero or less is requested. Matches the
   * fallback {@code Pageable} the AdminService registers for the normal path
   * ({@code WebMvcConfig}: {@code PageRequest.of(0, 10)}). This is intentionally different
   * from the default of 20 the legacy endpoints apply when {@code size} is omitted, which
   * arrives here as 20 through the {@code @RequestParam(defaultValue = "20")}.
   */
  private static final int FALLBACK_PAGE_SIZE = 10;

  /**
   * Cap applied to the requested page size. Matches Spring Data's default max page size
   * ({@code PageableHandlerMethodArgumentResolverSupport.DEFAULT_MAX_PAGE_SIZE}) that the
   * AdminService applies when it resolves the {@code Pageable} for the normal path.
   */
  private static final int MAX_PAGE_SIZE = 2000;

  /**
   * User id the stock {@code SpringSecurityUserInfoHolder} fabricates for the anonymous
   * authentication token. It is not a signed-in user, so it never counts as a trusted Portal
   * user.
   */
  private static final String ANONYMOUS_USER_ID = "anonymousUser";

  private final InstanceService instanceService;
  private final ReleaseService releaseService;
  private final UnifiedPermissionValidator unifiedPermissionValidator;
  private final UserPermissionValidator userPermissionValidator;
  private final UserInfoHolder userInfoHolder;

  public InstanceController(final InstanceService instanceService,
      final ReleaseService releaseService,
      final UnifiedPermissionValidator unifiedPermissionValidator,
      final UserPermissionValidator userPermissionValidator, final UserInfoHolder userInfoHolder) {
    this.instanceService = instanceService;
    this.releaseService = releaseService;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
    this.userPermissionValidator = userPermissionValidator;
    this.userInfoHolder = userInfoHolder;
  }

  @GetMapping("/envs/{env}/instances/by-release")
  public PageDTO<InstanceDTO> getByRelease(@PathVariable String env, @RequestParam long releaseId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ReleaseDTO release = findReleaseOrThrow(Env.valueOf(env), releaseId);
    if (decideReadAccess(release.getAppId(), env, release.getClusterName(),
        release.getNamespaceName()) != ReadAccess.ALLOW) {
      // Both HIDE and DENY deny on by-release: the owning namespace is only known after
      // resolving the release, so there is no empty-result view to return here.
      throw new AccessDeniedException("Access is denied");
    }

    return instanceService.getByRelease(Env.valueOf(env), releaseId, page, size);
  }

  @GetMapping("/envs/{env}/instances/by-namespace")
  public PageDTO<InstanceDTO> getByNamespace(@PathVariable String env, @RequestParam String appId,
      @RequestParam String clusterName, @RequestParam String namespaceName,
      @RequestParam(required = false) String instanceAppId,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ReadAccess access = decideReadAccess(appId, env, clusterName, namespaceName);
    if (access == ReadAccess.HIDE) {
      return emptyInstancePage(page, size);
    }
    if (access == ReadAccess.DENY) {
      throw new AccessDeniedException("Access is denied");
    }

    return instanceService.getByNamespace(Env.valueOf(env), appId, clusterName, namespaceName,
        instanceAppId, page, size);
  }

  @GetMapping("/envs/{env}/instances/by-namespace/count")
  public ResponseEntity<Number> getInstanceCountByNamespace(@PathVariable String env,
      @RequestParam String appId, @RequestParam String clusterName,
      @RequestParam String namespaceName) {
    ReadAccess access = decideReadAccess(appId, env, clusterName, namespaceName);
    if (access == ReadAccess.HIDE) {
      return ResponseEntity.ok(new Number(0));
    }
    if (access == ReadAccess.DENY) {
      throw new AccessDeniedException("Access is denied");
    }

    int count = instanceService.getInstanceCountByNamespace(appId, Env.valueOf(env), clusterName,
        namespaceName);
    return ResponseEntity.ok(new Number(count));
  }

  @GetMapping("/envs/{env}/instances/by-namespace-and-releases-not-in")
  public List<InstanceDTO> getByReleasesNotIn(@PathVariable String env, @RequestParam String appId,
      @RequestParam String clusterName, @RequestParam String namespaceName,
      @RequestParam String releaseIds) {
    ReadAccess access = decideReadAccess(appId, env, clusterName, namespaceName);
    if (access == ReadAccess.HIDE) {
      return Collections.emptyList();
    }
    if (access == ReadAccess.DENY) {
      throw new AccessDeniedException("Access is denied");
    }

    Set<Long> releaseIdSet = RELEASES_SPLITTER.splitToList(releaseIds).stream().map(Long::parseLong)
        .collect(Collectors.toSet());

    if (CollectionUtils.isEmpty(releaseIdSet)) {
      throw new BadRequestException("release ids can not be empty");
    }

    return instanceService.getByReleasesNotIn(Env.valueOf(env), appId, clusterName, namespaceName,
        releaseIdSet);
  }

  /**
   * Outcome of the single read-access decision made for one request. The decision is evaluated
   * exactly once per request by {@link #decideReadAccess}, so the permission checks behind it
   * are never repeated for the same namespace.
   */
  private enum ReadAccess {
    /** The caller may read the instances. */
    ALLOW,
    /**
     * The config is hidden from this caller: empty results on the namespace queries, 403 on
     * by-release.
     */
    HIDE,
    /** The caller may not read the instances: 403 on all four endpoints. */
    DENY
  }

  /**
   * Decide once whether the current caller may read instances of the given namespace. Every
   * identity gets an explicit outcome before any {@code InstanceService} call: a Portal
   * session and a trusted custom SSO user follow the namespace visibility policy (hidden
   * configs are HIDE), a user token may not read a hidden config at all (DENY), a consumer
   * needs release-namespace permission, and an identity that cannot be resolved to a user
   * is denied.
   */
  private ReadAccess decideReadAccess(String appId, String env, String clusterName,
      String namespaceName) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(authType)) {
      return unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
          namespaceName) ? ReadAccess.HIDE : ReadAccess.ALLOW;
    }
    if (UserIdentityConstants.USER_TOKEN.equals(authType)) {
      return unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
          namespaceName) ? ReadAccess.DENY : ReadAccess.ALLOW;
    }
    if (UserIdentityConstants.CONSUMER.equals(authType)) {
      return unifiedPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
          namespaceName) ? ReadAccess.ALLOW : ReadAccess.DENY;
    }
    if (UserIdentityConstants.ANONYMOUS.equals(authType)) {
      // A custom SSO integration can resolve a signed-in user through UserInfoHolder while
      // the identity resolver still reports ANONYMOUS. Apply the same visibility policy
      // through the Portal-user delegate directly: UnifiedPermissionValidator cannot serve
      // ANONYMOUS, its getDelegate() throws IllegalStateException for unknown types.
      // An identity that cannot be resolved to a user is denied without consulting any
      // permission check.
      if (!hasTrustedPortalUser()) {
        return ReadAccess.DENY;
      }
      return userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
          namespaceName) ? ReadAccess.HIDE : ReadAccess.ALLOW;
    }
    // Null or any other value means the identity resolver did not classify the request;
    // deny instead of falling through to InstanceService.
    return ReadAccess.DENY;
  }

  /**
   * Whether the current request carries a signed-in Portal user even though the identity
   * resolver reports ANONYMOUS. The UserInfoHolder is the trust boundary for custom SSO
   * integrations; the stock SpringSecurityUserInfoHolder fabricates the {@code anonymousUser}
   * id for the anonymous token, which is not a signed-in user.
   */
  private boolean hasTrustedPortalUser() {
    UserInfo user = userInfoHolder.getUser();
    return user != null && StringUtils.hasText(user.getUserId())
        && !ANONYMOUS_USER_ID.equals(user.getUserId());
  }

  private ReleaseDTO findReleaseOrThrow(Env env, long releaseId) {
    ReleaseDTO release = releaseService.findReleaseById(env, releaseId);
    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    return release;
  }

  private PageDTO<InstanceDTO> emptyInstancePage(int page, int size) {
    return new PageDTO<>(Collections.<InstanceDTO>emptyList(),
        PageRequest.of(resolvePage(page), resolvePageSize(size)), 0L);
  }

  /**
   * Mirror the pagination normalization the AdminService applies when it resolves the
   * {@code Pageable} for the normal path (Spring Data's
   * {@code PageableHandlerMethodArgumentResolverSupport}): a negative page becomes 0, a size
   * below one falls back to the AdminService resolver's fallback page size (10), and a size
   * above the default max page size is capped. Valid inputs are returned unchanged, so the
   * echoed page/size match the normal path. Without this, {@code PageRequest.of} throws
   * IllegalArgumentException for a negative page or a size below one and the hidden-result
   * branch returns HTTP 500.
   */
  private int resolvePage(int page) {
    return Math.max(page, 0);
  }

  private int resolvePageSize(int size) {
    if (size < 1) {
      return FALLBACK_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }

}
