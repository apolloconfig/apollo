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

import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.AbstractApolloHttpException;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.bo.AppNamespaceDeletionCheckBO;
import com.ctrip.framework.apollo.portal.entity.bo.AppNamespaceEnvDeletionStatusBO;
import com.ctrip.framework.apollo.portal.entity.bo.AppNamespaceEnvDeletionStatusBO.Status;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceDeletionEvent;
import com.ctrip.framework.apollo.tracer.Tracer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Deletes an AppNamespace in the Portal and in every environment, cleans up what remains in the
 * environments after the Portal row is gone, and checks what remains without changing anything.
 * The methods are not transactional, so the changes that took effect stay when an environment
 * fails, and the deletion and the cleanup name the environments where they are not complete. An
 * environment deletes only the AppNamespace with the id listed there just before, so it keeps one
 * recreated under the same name after that.
 */
@Service
public class AppNamespaceDeletionService {

  private static final Logger logger = LoggerFactory.getLogger(AppNamespaceDeletionService.class);

  private final AppNamespaceService appNamespaceService;
  private final PortalSettings portalSettings;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;
  private final ApplicationEventPublisher publisher;

  public AppNamespaceDeletionService(final AppNamespaceService appNamespaceService,
      final PortalSettings portalSettings, final AdminServiceAPI.NamespaceAPI namespaceAPI,
      final ApplicationEventPublisher publisher) {
    this.appNamespaceService = appNamespaceService;
    this.portalSettings = portalSettings;
    this.namespaceAPI = namespaceAPI;
    this.publisher = publisher;
  }

  /**
   * Deletes the AppNamespace in the Portal and then in every environment that has it. The Portal
   * deletion stays committed when an environment fails.
   *
   * @throws BadRequestException if the AppNamespace is not in the Portal, before any environment
   *     is called
   * @throws ServiceException if an environment failed or was skipped because the health check
   *     marks it down
   */
  public void deleteAppNamespace(String appId, String namespaceName, String operator) {
    AppNamespace appNamespace =
        appNamespaceService.deleteAppNamespace(appId, namespaceName, operator);
    // the lookup may ignore case, so the environments get the names stored in the Portal
    appId = appNamespace.getAppId();
    namespaceName = appNamespace.getName();

    EnvResults results = deleteInEnvs(appId, namespaceName, operator, false);
    publisher.publishEvent(new AppNamespaceDeletionEvent(appNamespace));

    if (!results.isComplete()) {
      throw new ServiceException("Deleting AppNamespace %s:%s is incomplete, and some changes have"
          + " already taken effect. It was removed from the Portal.%s After these environments"
          + " recover, check the AppNamespace on the delete management page and clean up what"
          + " remains.", appId, namespaceName, results.describe());
    }
  }

  /**
   * Cleans up what remains of the AppNamespace in the environments after it was removed from the
   * Portal. The Portal is checked again right before each environment deletion and after one that
   * failed, and the cleanup stops when the AppNamespace was recreated there.
   *
   * @throws BadRequestException if the AppNamespace is in the Portal before anything was deleted,
   *     or if every environment was checked and none has it
   * @throws ServiceException if an environment failed or was skipped because the health check
   *     marks it down, or if the AppNamespace was recreated in the Portal after the cleanup had
   *     deleted it somewhere
   */
  public void cleanUpAppNamespace(String appId, String namespaceName, String operator) {
    if (existsInPortal(appId, namespaceName)) {
      throw appNamespaceExistsInPortal(appId, namespaceName);
    }

    EnvResults results = deleteInEnvs(appId, namespaceName, operator, true);

    if (results.isComplete()) {
      if (results.deletedEnvs.isEmpty()) {
        throw BadRequestException.appNamespaceNotExists(appId, namespaceName);
      }
      return;
    }
    throw new ServiceException(
        "Cleaning up AppNamespace %s:%s is incomplete, and some changes may"
            + " already have taken effect.%s Check it again after these environments recover.",
        appId, namespaceName, results.describe());
  }

  /**
   * Checks whether the AppNamespace is in the Portal and in every environment. Nothing is deleted
   * and no event is published.
   */
  public AppNamespaceDeletionCheckBO checkAppNamespaceDeletion(String appId, String namespaceName) {
    boolean existsInPortal = existsInPortal(appId, namespaceName);
    List<AppNamespaceEnvDeletionStatusBO> environments = new ArrayList<>();
    for (Env env : portalSettings.getAllEnvs()) {
      EnvCheck check = checkEnv(env, appId, namespaceName);
      environments.add(new AppNamespaceEnvDeletionStatusBO(env, check.status, check.message));
    }
    return new AppNamespaceDeletionCheckBO(appId, namespaceName, existsInPortal, environments);
  }

  private EnvResults deleteInEnvs(String appId, String namespaceName, String operator,
      boolean cleanup) {
    EnvResults results = new EnvResults();
    for (Env env : portalSettings.getAllEnvs()) {
      EnvCheck check = checkEnv(env, appId, namespaceName);
      if (check.status == Status.SKIPPED) {
        results.skippedEnvs.add(env);
      } else if (check.status == Status.UNREACHABLE) {
        results.failedEnvs.add(env);
      } else if (check.status == Status.REMAINING) {
        // a cleanup must not delete an AppNamespace that was recreated in the Portal meanwhile
        if (cleanup && existsInPortal(appId, namespaceName)) {
          throw recreatedDuringCleanup(appId, namespaceName, results.deletedEnvs);
        }
        // the environment deletes it only while it has the listed id, so it keeps an AppNamespace
        // recreated after the listing
        if (deleteInEnv(env, appId, check.storedName, check.storedId, operator)) {
          results.deletedEnvs.add(env);
        } else if (cleanup && existsInPortal(appId, namespaceName)) {
          // the environment may have refused the deletion because the AppNamespace was recreated
          throw recreatedDuringCleanup(appId, namespaceName, results.deletedEnvs);
        } else if (!isRemoved(check, checkEnv(env, appId, namespaceName))) {
          results.failedEnvs.add(env);
        }
      }
    }
    return results;
  }

  private EnvCheck checkEnv(Env env, String appId, String namespaceName) {
    if (!portalSettings.isEnvActive(env)) {
      return new EnvCheck(Status.SKIPPED, null, 0, null);
    }
    try {
      // the databases compare names ignoring case, so the environment's own spelling is used
      AppNamespaceDTO stored = namespaceAPI.getAppNamespaces(appId, env).stream()
          .filter(appNamespace -> namespaceName.equalsIgnoreCase(appNamespace.getName()))
          .findFirst().orElse(null);
      if (stored == null) {
        return new EnvCheck(Status.ABSENT, null, 0, null);
      }
      return new EnvCheck(Status.REMAINING, stored.getName(), stored.getId(), null);
    } catch (Throwable e) {
      logFailure("Check", env, appId, namespaceName, e);
      return new EnvCheck(Status.UNREACHABLE, null, 0, e.getMessage());
    }
  }

  /**
   * Whether the AppNamespace listed in an environment is gone there because another deletion or
   * cleanup removed it after the listing. The environment may have one with the same name again,
   * recreated with another id.
   */
  private static boolean isRemoved(EnvCheck listed, EnvCheck recheck) {
    return recheck.status == Status.ABSENT
        || (recheck.status == Status.REMAINING && recheck.storedId != listed.storedId);
  }

  private boolean deleteInEnv(Env env, String appId, String namespaceName, long appNamespaceId,
      String operator) {
    try {
      namespaceAPI.deleteAppNamespace(env, appId, namespaceName, appNamespaceId, operator);
      return true;
    } catch (Throwable e) {
      logFailure("Delete", env, appId, namespaceName, e);
      return false;
    }
  }

  private boolean existsInPortal(String appId, String namespaceName) {
    return appNamespaceService.findByAppIdAndName(appId, namespaceName) != null;
  }

  private static BadRequestException appNamespaceExistsInPortal(String appId,
      String namespaceName) {
    return new BadRequestException("AppNamespace %s:%s exists in the Portal, so it is not cleaned"
        + " up as a leftover. It may have been recreated. Delete it normally, or check it again.",
        appId, namespaceName);
  }

  private static AbstractApolloHttpException recreatedDuringCleanup(String appId,
      String namespaceName, List<Env> deletedEnvs) {
    if (deletedEnvs.isEmpty()) {
      return appNamespaceExistsInPortal(appId, namespaceName);
    }
    return new ServiceException(
        "Cleaning up AppNamespace %s:%s stopped because the AppNamespace"
            + " was recreated in the Portal during the cleanup, and some changes may already have"
            + " taken effect. Deleted in %s. The other environments were not changed.",
        appId, namespaceName, deletedEnvs);
  }

  private static void logFailure(String action, Env env, String appId, String namespaceName,
      Throwable e) {
    String message = String.format("%s appNamespace failed. appId = %s, namespace = %s, env = %s",
        action, appId, namespaceName, env);
    logger.error(message, e);
    Tracer.logError(message, e);
  }

  /**
   * The status of the AppNamespace in one environment, and the name and id that environment stores.
   */
  private static class EnvCheck {

    private final Status status;
    private final String storedName;
    private final long storedId;
    private final String message;

    private EnvCheck(Status status, String storedName, long storedId, String message) {
      this.status = status;
      this.storedName = storedName;
      this.storedId = storedId;
      this.message = message;
    }
  }

  /**
   * The environments where a deletion or a cleanup deleted the AppNamespace, failed or was skipped.
   */
  private static class EnvResults {

    private final List<Env> deletedEnvs = new ArrayList<>();
    private final List<Env> failedEnvs = new ArrayList<>();
    private final List<Env> skippedEnvs = new ArrayList<>();

    private boolean isComplete() {
      return failedEnvs.isEmpty() && skippedEnvs.isEmpty();
    }

    /**
     * Names the environments of each nonempty list in a sentence that starts with a space.
     */
    private String describe() {
      StringBuilder message = new StringBuilder();
      append(message, "Deleted in", deletedEnvs);
      append(message, "Failed in", failedEnvs);
      append(message, "Skipped because the health check marks them down:", skippedEnvs);
      return message.toString();
    }

    private static void append(StringBuilder message, String label, List<Env> envs) {
      if (!envs.isEmpty()) {
        message.append(' ').append(label).append(' ').append(envs).append('.');
      }
    }
  }
}
