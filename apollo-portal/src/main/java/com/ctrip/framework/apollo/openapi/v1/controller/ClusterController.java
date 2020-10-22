package com.ctrip.framework.apollo.openapi.v1.controller;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.spi.UserService;

@RestController("openapiClusterController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ClusterController {

  private final ClusterService clusterService;
  private final UserService userService;

  public ClusterController(final ClusterService clusterService, final UserService userService) {
    this.clusterService = clusterService;
    this.userService = userService;
  }

  @GetMapping(value = "apps/{appId}/clusters/{clusterName:.+}")
  public OpenClusterDTO loadCluster(@PathVariable("appId") String appId, @PathVariable String env,
      @PathVariable("clusterName") String clusterName) {

    ClusterDTO clusterDTO = clusterService.loadCluster(appId, Env.valueOf(env), clusterName);
    return clusterDTO == null ? null : OpenApiBeanUtils.transformFromClusterDTO(clusterDTO);
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateClusterPermission(#request, #appId)")
  @PostMapping(value = "apps/{appId}/clusters")
  public OpenClusterDTO createCluster(@PathVariable String appId, @PathVariable String env,
      @Valid @RequestBody OpenClusterDTO cluster, HttpServletRequest request) {

    if (!Objects.equals(appId, cluster.getAppId())) {
      throw new BadRequestException(String.format(
          "AppId not equal. AppId in path = %s, AppId in payload = %s", appId, cluster.getAppId()));
    }

    String clusterName = cluster.getName();
    String operator = cluster.getDataChangeCreatedBy();

    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(clusterName, operator),
        "name and dataChangeCreatedBy should not be null or empty");

    if (!InputValidator.isValidClusterNamespace(clusterName)) {
      throw new BadRequestException(
          String.format("Invalid ClusterName format: %s", InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
    }

    if (userService.findByUserId(operator) == null) {
      throw new BadRequestException("User " + operator + " doesn't exist!");
    }

    ClusterDTO toCreate = OpenApiBeanUtils.transformToClusterDTO(cluster);
    ClusterDTO createdClusterDTO = clusterService.createCluster(Env.valueOf(env), toCreate);

    return OpenApiBeanUtils.transformFromClusterDTO(createdClusterDTO);
  }

}
