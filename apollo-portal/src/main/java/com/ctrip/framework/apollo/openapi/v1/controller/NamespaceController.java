package com.ctrip.framework.apollo.openapi.v1.controller;


import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

@RestController("openapiNamespaceController")
public class NamespaceController {

  private final NamespaceLockService namespaceLockService;
  private final NamespaceService namespaceService;
  private final AppNamespaceService appNamespaceService;
  private final ApplicationEventPublisher publisher;
  private final UserService userService;

  public NamespaceController(
      final NamespaceLockService namespaceLockService,
      final NamespaceService namespaceService,
      final AppNamespaceService appNamespaceService,
      final ApplicationEventPublisher publisher,
      final UserService userService) {
    this.namespaceLockService = namespaceLockService;
    this.namespaceService = namespaceService;
    this.appNamespaceService = appNamespaceService;
    this.publisher = publisher;
    this.userService = userService;
  }


  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateNamespacePermission(#request, #appId)")
  @PostMapping(value = "/openapi/v1/apps/{appId}/appnamespaces")
  public OpenAppNamespaceDTO createNamespace(@PathVariable String appId,
                                             @RequestBody OpenAppNamespaceDTO appNamespaceDTO,
                                             HttpServletRequest request) {

    if (!Objects.equals(appId, appNamespaceDTO.getAppId())) {
      throw new BadRequestException(String.format("AppId not equal. AppId in path = %s, AppId in payload = %s", appId,
                                                  appNamespaceDTO.getAppId()));
    }
    RequestPrecondition.checkArgumentsNotEmpty(appNamespaceDTO.getAppId(), appNamespaceDTO.getName(),
                                               appNamespaceDTO.getFormat(), appNamespaceDTO.getDataChangeCreatedBy());

    if (!InputValidator.isValidAppNamespace(appNamespaceDTO.getName())) {
      throw new BadRequestException(String.format("Invalid Namespace format: %s",
                                                  InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE + " & "
                                                  + InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE));
    }

    if (!ConfigFileFormat.isValidFormat(appNamespaceDTO.getFormat())) {
      throw new BadRequestException(String.format("Invalid namespace format. format = %s", appNamespaceDTO.getFormat()));
    }

    String operator = appNamespaceDTO.getDataChangeCreatedBy();
    if (userService.findByUserId(operator) == null) {
      throw new BadRequestException(String.format("Illegal user. user = %s", operator));
    }

    AppNamespace appNamespace = OpenApiBeanUtils.transformToAppNamespace(appNamespaceDTO);
    AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(appNamespace, appNamespaceDTO.isAppendNamespacePrefix());

    publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));

    return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(createdAppNamespace);
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces")
  public List<OpenNamespaceDTO> findNamespaces(@PathVariable String appId, @PathVariable String env,
                                               @PathVariable String clusterName) {

    return OpenApiBeanUtils
        .batchTransformFromNamespaceBOs(namespaceService.findNamespaceBOs(appId, Env
            .valueOf(env), clusterName));
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}")
  public OpenNamespaceDTO loadNamespace(@PathVariable String appId, @PathVariable String env,
                                        @PathVariable String clusterName, @PathVariable String
                                            namespaceName) {
    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf
        (env), clusterName, namespaceName);
    if (namespaceBO == null) {
      return null;
    }
    return OpenApiBeanUtils.transformFromNamespaceBO(namespaceBO);
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock")
  public OpenNamespaceLockDTO getNamespaceLock(@PathVariable String appId, @PathVariable String env,
                                               @PathVariable String clusterName, @PathVariable
                                                   String namespaceName) {

    NamespaceDTO namespace = namespaceService.loadNamespaceBaseInfo(appId, Env
        .valueOf(env), clusterName, namespaceName);
    NamespaceLockDTO lockDTO = namespaceLockService.getNamespaceLock(appId, Env
        .valueOf(env), clusterName, namespaceName);
    return OpenApiBeanUtils.transformFromNamespaceLockDTO(namespace.getNamespaceName(), lockDTO);
  }

}
