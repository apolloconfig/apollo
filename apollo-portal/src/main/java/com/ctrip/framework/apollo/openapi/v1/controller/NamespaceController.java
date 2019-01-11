package com.ctrip.framework.apollo.openapi.v1.controller;


import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.enums.Env;
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
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("openapiNamespaceController")
public class NamespaceController {

  @Autowired
  private NamespaceLockService namespaceLockService;
  @Autowired
  private NamespaceService namespaceService;
  @Autowired
  private AppNamespaceService appNamespaceService;
  @Autowired
  private ApplicationEventPublisher publisher;
  @Autowired
  private UserService userService;


  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateNamespacePermission(#request, #appId)")
  @PostMapping(value = "/openapi/v1/apps/{appId}/appnamespaces")
  public OpenAppNamespaceDTO createNamespace(@PathVariable String appId,
                                             @Valid @RequestBody OpenAppNamespaceDTO appNamespaceDTO,
                                             HttpServletRequest request) {

    if (!Objects.equals(appId, appNamespaceDTO.getAppId())) {
      throw new BadRequestException(String.format("AppId not equal. AppId in path = %s, AppId in payload = %s", appId,
                                                  appNamespaceDTO.getAppId()));
    }
    RequestPrecondition.checkArgumentsNotEmpty(appNamespaceDTO.getAppId(), appNamespaceDTO.getName(),
                                               appNamespaceDTO.getFormat(), appNamespaceDTO.getDataChangeCreatedBy());

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
            .fromString(env), clusterName));
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}")
  public OpenNamespaceDTO loadNamespace(@PathVariable String appId, @PathVariable String env,
                                        @PathVariable String clusterName, @PathVariable String
                                            namespaceName) {
    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.fromString
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
        .fromString(env), clusterName, namespaceName);
    NamespaceLockDTO lockDTO = namespaceLockService.getNamespaceLock(appId, Env
        .fromString(env), clusterName, namespaceName);
    return OpenApiBeanUtils.transformFromNamespaceLockDTO(namespace.getNamespaceName(), lockDTO);
  }

}
