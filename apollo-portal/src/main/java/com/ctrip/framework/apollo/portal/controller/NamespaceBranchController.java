package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.component.PermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.listener.ConfigPublishEvent;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NamespaceBranchController {

  private final PermissionValidator permissionValidator;
  private final ReleaseService releaseService;
  private final NamespaceBranchService namespaceBranchService;
  private final ApplicationEventPublisher publisher;
  private final PortalConfig portalConfig;

  public NamespaceBranchController(
      final PermissionValidator permissionValidator,
      final ReleaseService releaseService,
      final NamespaceBranchService namespaceBranchService,
      final ApplicationEventPublisher publisher,
      final PortalConfig portalConfig) {
    this.permissionValidator = permissionValidator;
    this.releaseService = releaseService;
    this.namespaceBranchService = namespaceBranchService;
    this.publisher = publisher;
    this.portalConfig = portalConfig;
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches")
  public NamespaceBO findBranch(@PathVariable String appId,
                                @PathVariable String env,
                                @PathVariable String clusterName,
                                @PathVariable String namespaceName) {
    NamespaceBO namespaceBO = namespaceBranchService.findBranch(appId, Env.valueOf(env), clusterName, namespaceName);

    if (namespaceBO != null && permissionValidator.shouldHideConfigToCurrentUser(appId, env, namespaceName)) {
      namespaceBO.hideItems();
    }

    return namespaceBO;
  }

  @PreAuthorize(value = "@permissionValidator.hasModifyNamespacePermission(#appId, #namespaceName, #env)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches")
  public NamespaceDTO createBranch(@PathVariable String appId,
                                   @PathVariable String env,
                                   @PathVariable String clusterName,
                                   @PathVariable String namespaceName) {

    return namespaceBranchService.createBranch(appId, Env.valueOf(env), clusterName, namespaceName);
  }

  @DeleteMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}")
  public void deleteBranch(@PathVariable String appId,
                           @PathVariable String env,
                           @PathVariable String clusterName,
                           @PathVariable String namespaceName,
                           @PathVariable String branchName) {

    boolean canDelete = permissionValidator.hasReleaseNamespacePermission(appId, namespaceName, env) ||
            (permissionValidator.hasModifyNamespacePermission(appId, namespaceName, env) &&
                      releaseService.loadLatestRelease(appId, Env.valueOf(env), branchName, namespaceName) == null);


    if (!canDelete) {
      throw new AccessDeniedException("Forbidden operation. "
                                      + "Caused by: 1.you don't have release permission "
                                      + "or 2. you don't have modification permission "
                                      + "or 3. you have modification permission but branch has been released");
    }

    namespaceBranchService.deleteBranch(appId, Env.valueOf(env), clusterName, namespaceName, branchName);

  }



  @PreAuthorize(value = "@permissionValidator.hasReleaseNamespacePermission(#appId, #namespaceName, #env)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/merge")
  public ReleaseDTO merge(@PathVariable String appId, @PathVariable String env,
                          @PathVariable String clusterName, @PathVariable String namespaceName,
                          @PathVariable String branchName, @RequestParam(value = "deleteBranch", defaultValue = "true") boolean deleteBranch,
                          @RequestBody NamespaceReleaseModel model) {

    if (model.isEmergencyPublish() && !portalConfig.isEmergencyPublishAllowed(Env.valueOf(env))) {
      throw new BadRequestException(String.format("Env: %s is not supported emergency publish now", env));
    }

    ReleaseDTO createdRelease = namespaceBranchService.merge(appId, Env.valueOf(env), clusterName, namespaceName, branchName,
                                                             model.getReleaseTitle(), model.getReleaseComment(),
                                                             model.isEmergencyPublish(), deleteBranch);

    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(appId)
        .withCluster(clusterName)
        .withNamespace(namespaceName)
        .withReleaseId(createdRelease.getId())
        .setMergeEvent(true)
        .setEnv(Env.valueOf(env));

    publisher.publishEvent(event);

    return createdRelease;
  }


  @GetMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules")
  public GrayReleaseRuleDTO getBranchGrayRules(@PathVariable String appId, @PathVariable String env,
                                                     @PathVariable String clusterName,
                                                     @PathVariable String namespaceName,
                                                     @PathVariable String branchName) {

    return namespaceBranchService.findBranchGrayRules(appId, Env.valueOf(env), clusterName, namespaceName, branchName);
  }


  @PreAuthorize(value = "@permissionValidator.hasOperateNamespacePermission(#appId, #namespaceName, #env)")
  @PutMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules")
  public void updateBranchRules(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @PathVariable String branchName, @RequestBody GrayReleaseRuleDTO rules) {

    namespaceBranchService
        .updateBranchGrayRules(appId, Env.valueOf(env), clusterName, namespaceName, branchName, rules);

  }

}
