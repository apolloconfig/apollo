package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AccessKeyService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author nisiyong
 */
@RestController
public class AccessKeyController {

  private final UserInfoHolder userInfoHolder;
  private final AccessKeyService accessKeyService;

  public AccessKeyController(
      UserInfoHolder userInfoHolder,
      AccessKeyService accessKeyService) {
    this.userInfoHolder = userInfoHolder;
    this.accessKeyService = accessKeyService;
  }

  @PreAuthorize(value = "@permissionValidator.isAppAdmin(#appId)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/accesskeys")
  public AccessKeyDTO save(@PathVariable String appId, @PathVariable String env,
      @RequestBody AccessKeyDTO accessKeyDTO) {
    String secret = UUID.randomUUID().toString().replaceAll("-", "");
    accessKeyDTO.setAppId(appId);
    accessKeyDTO.setSecret(secret);
    return accessKeyService.createAccessKey(Env.valueOf(env), accessKeyDTO);
  }

  @PreAuthorize(value = "@permissionValidator.isAppAdmin(#appId)")
  @GetMapping(value = "/apps/{appId}/envs/{env}/accesskeys")
  public List<AccessKeyDTO> findByAppId(@PathVariable String appId,
      @PathVariable String env) {
    return accessKeyService.findByAppId(Env.valueOf(env), appId);
  }

  @PreAuthorize(value = "@permissionValidator.isAppAdmin(#appId)")
  @DeleteMapping(value = "/apps/{appId}/envs/{env}/accesskeys/{id}")
  public void delete(@PathVariable String appId,
      @PathVariable String env,
      @PathVariable long id) {
    String operator = userInfoHolder.getUser().getUserId();
    accessKeyService.deleteAccessKey(Env.valueOf(env), appId, id, operator);
  }

  @PreAuthorize(value = "@permissionValidator.isAppAdmin(#appId)")
  @PutMapping(value = "/apps/{appId}/envs/{env}/accesskeys/{id}/enable")
  public void enable(@PathVariable String appId,
      @PathVariable String env,
      @PathVariable long id) {
    String operator = userInfoHolder.getUser().getUserId();
    accessKeyService.enable(Env.valueOf(env), appId, id, operator);
  }

  @PreAuthorize(value = "@permissionValidator.isAppAdmin(#appId)")
  @PutMapping(value = "/apps/{appId}/envs/{env}/accesskeys/{id}/disable")
  public void disable(@PathVariable String appId,
      @PathVariable String env,
      @PathVariable long id) {
    String operator = userInfoHolder.getUser().getUserId();
    accessKeyService.disable(Env.valueOf(env), appId, id, operator);
  }
}
