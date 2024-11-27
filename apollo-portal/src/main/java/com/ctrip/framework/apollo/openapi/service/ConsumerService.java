/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.service;

import static com.ctrip.framework.apollo.portal.service.SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerAudit;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.repository.ConsumerAuditRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerTokenRepository;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerInfo;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class ConsumerService {

  private static final FastDateFormat TIMESTAMP_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss");
  private static final Joiner KEY_JOINER = Joiner.on("|");

  private final UserInfoHolder userInfoHolder;
  private final ConsumerTokenRepository consumerTokenRepository;
  private final ConsumerRepository consumerRepository;
  private final ConsumerAuditRepository consumerAuditRepository;
  private final ConsumerRoleRepository consumerRoleRepository;
  private final PortalConfig portalConfig;
  private final RolePermissionService rolePermissionService;
  private final UserService userService;
  private final RoleRepository roleRepository;

  public ConsumerService(
      final UserInfoHolder userInfoHolder,
      final ConsumerTokenRepository consumerTokenRepository,
      final ConsumerRepository consumerRepository,
      final ConsumerAuditRepository consumerAuditRepository,
      final ConsumerRoleRepository consumerRoleRepository,
      final PortalConfig portalConfig,
      final RolePermissionService rolePermissionService,
      final UserService userService,
      final RoleRepository roleRepository) {
    this.userInfoHolder = userInfoHolder;
    this.consumerTokenRepository = consumerTokenRepository;
    this.consumerRepository = consumerRepository;
    this.consumerAuditRepository = consumerAuditRepository;
    this.consumerRoleRepository = consumerRoleRepository;
    this.portalConfig = portalConfig;
    this.rolePermissionService = rolePermissionService;
    this.userService = userService;
    this.roleRepository = roleRepository;
  }


  public Consumer createConsumer(Consumer consumer) {
    String appId = consumer.getAppId();

    Consumer managedConsumer = consumerRepository.findByAppId(appId);
    if (managedConsumer != null) {
      throw new BadRequestException("Consumer already exist");
    }

    String ownerName = consumer.getOwnerName();
    UserInfo owner = userService.findByUserId(ownerName);
    if (owner == null) {
      throw BadRequestException.userNotExists(ownerName);
    }
    consumer.setOwnerEmail(owner.getEmail());

    String operator = userInfoHolder.getUser().getUserId();
    consumer.setDataChangeCreatedBy(operator);
    consumer.setDataChangeLastModifiedBy(operator);

    return consumerRepository.save(consumer);
  }

  public ConsumerToken generateAndSaveConsumerToken(Consumer consumer, Integer rateLimit, Date expires) {
    Preconditions.checkArgument(consumer != null, "Consumer can not be null");

    ConsumerToken consumerToken = generateConsumerToken(consumer, rateLimit, expires);
    consumerToken.setId(0);

    return consumerTokenRepository.save(consumerToken);
  }

  public ConsumerToken getConsumerTokenByAppId(String appId) {
    Consumer consumer = consumerRepository.findByAppId(appId);
    if (consumer == null) {
      return null;
    }

    return consumerTokenRepository.findByConsumerId(consumer.getId());
  }

  public ConsumerToken getConsumerTokenByToken(String token) {
    if (Strings.isNullOrEmpty(token)) {
      return null;
    }
    return consumerTokenRepository.findTopByTokenAndExpiresAfter(token, new Date());
  }

  public Long getConsumerIdByToken(String token) {
    ConsumerToken consumerToken = getConsumerTokenByToken(token);
    return consumerToken == null ? null : consumerToken.getConsumerId();
  }

  public Consumer getConsumerByConsumerId(long consumerId) {
    return consumerRepository.findById(consumerId).orElse(null);
  }

  @Transactional
  public List<ConsumerRole> assignNamespaceRoleToConsumer(String token, String appId, String namespaceName) {
    return assignNamespaceRoleToConsumer(token, appId, namespaceName, null);
  }

  @Transactional
  public List<ConsumerRole> assignNamespaceRoleToConsumer(String token, String appId, String namespaceName, String env) {
    Long consumerId = getConsumerIdByToken(token);
    if (consumerId == null) {
      throw new BadRequestException("Token is Illegal");
    }

    Role namespaceModifyRole =
        rolePermissionService.findRoleByRoleName(RoleUtils.buildModifyNamespaceRoleName(appId, namespaceName, env));
    Role namespaceReleaseRole =
        rolePermissionService.findRoleByRoleName(RoleUtils.buildReleaseNamespaceRoleName(appId, namespaceName, env));

    if (namespaceModifyRole == null || namespaceReleaseRole == null) {
      throw new BadRequestException("Namespace's role does not exist. Please check whether namespace has created.");
    }

    long namespaceModifyRoleId = namespaceModifyRole.getId();
    long namespaceReleaseRoleId = namespaceReleaseRole.getId();

    ConsumerRole managedModifyRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, namespaceModifyRoleId);
    ConsumerRole managedReleaseRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, namespaceReleaseRoleId);
    if (managedModifyRole != null && managedReleaseRole != null) {
      return Arrays.asList(managedModifyRole, managedReleaseRole);
    }

    String operator = userInfoHolder.getUser().getUserId();

    ConsumerRole namespaceModifyConsumerRole = createConsumerRole(consumerId, namespaceModifyRoleId, operator);
    ConsumerRole namespaceReleaseConsumerRole = createConsumerRole(consumerId, namespaceReleaseRoleId, operator);

    ConsumerRole createdModifyConsumerRole = consumerRoleRepository.save(namespaceModifyConsumerRole);
    ConsumerRole createdReleaseConsumerRole = consumerRoleRepository.save(namespaceReleaseConsumerRole);

    return Arrays.asList(createdModifyConsumerRole, createdReleaseConsumerRole);
  }

  private ConsumerInfo convert(
      Consumer consumer,
      String token,
      boolean allowCreateApplication,
      Integer rateLimit
  ) {
    ConsumerInfo consumerInfo = new ConsumerInfo();
    consumerInfo.setConsumerId(consumer.getId());
    consumerInfo.setAppId(consumer.getAppId());
    consumerInfo.setName(consumer.getName());
    consumerInfo.setOwnerName(consumer.getOwnerName());
    consumerInfo.setOwnerEmail(consumer.getOwnerEmail());
    consumerInfo.setOrgId(consumer.getOrgId());
    consumerInfo.setOrgName(consumer.getOrgName());
    consumerInfo.setRateLimit(rateLimit);

    consumerInfo.setToken(token);
    consumerInfo.setAllowCreateApplication(allowCreateApplication);
    return consumerInfo;
  }

  public ConsumerInfo getConsumerInfoByAppId(String appId) {
    ConsumerToken consumerToken = getConsumerTokenByAppId(appId);
    if (null == consumerToken) {
      return null;
    }
    Consumer consumer = consumerRepository.findByAppId(appId);
    if (consumer == null) {
      return null;
    }
    return convert(consumer, consumerToken.getToken(), isAllowCreateApplication(consumer.getId()), getRateLimit(consumer.getId()));
  }

  private boolean isAllowCreateApplication(Long consumerId) {
    return isAllowCreateApplication(Collections.singletonList(consumerId)).get(0);
  }

  private Integer getRateLimit(Long consumerId) {
    List<Integer> list = getRateLimit(Collections.singletonList(consumerId));
    if (CollectionUtils.isEmpty(list)) {
      return 0;
    }
    return list.get(0);
  }

  private List<Boolean> isAllowCreateApplication(List<Long> consumerIdList) {
    Role createAppRole = getCreateAppRole();
    if (createAppRole == null) {
      List<Boolean> list = new ArrayList<>(consumerIdList.size());
      for (Long ignored : consumerIdList) {
        list.add(false);
      }
      return list;
    }

    long roleId = createAppRole.getId();
    List<Boolean> list = new ArrayList<>(consumerIdList.size());
    for (Long consumerId : consumerIdList) {
      ConsumerRole createAppConsumerRole = consumerRoleRepository.findByConsumerIdAndRoleId(
          consumerId, roleId
      );
      list.add(createAppConsumerRole != null);
    }

    return list;
  }

  private List<Integer> getRateLimit(List<Long> consumerIds) {
    List<ConsumerToken> consumerTokens = consumerTokenRepository.findByConsumerIdIn(consumerIds);
    Map<Long, Integer> consumerRateLimits = consumerTokens.stream()
        .collect(Collectors.toMap(
            ConsumerToken::getConsumerId,
            consumerToken -> consumerToken.getRateLimit() != null ? consumerToken.getRateLimit() : 0
        ));

    return consumerIds.stream()
        .map(id -> consumerRateLimits.getOrDefault(id, 0))
        .collect(Collectors.toList());
  }

  private Role getCreateAppRole() {
    return rolePermissionService.findRoleByRoleName(CREATE_APPLICATION_ROLE_NAME);
  }

  public ConsumerRole assignCreateApplicationRoleToConsumer(String token) {
    Long consumerId = getConsumerIdByToken(token);
    if (consumerId == null) {
      throw new BadRequestException("Token is Illegal");
    }
    Role createAppRole = getCreateAppRole();
    if (createAppRole == null) {
      throw NotFoundException.roleNotFound(CREATE_APPLICATION_ROLE_NAME);
    }

    long roleId = createAppRole.getId();
    ConsumerRole createAppConsumerRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, roleId);
    if (createAppConsumerRole != null) {
      return createAppConsumerRole;
    }

    String operator = userInfoHolder.getUser().getUserId();
    ConsumerRole consumerRole = createConsumerRole(consumerId, roleId, operator);
    return consumerRoleRepository.save(consumerRole);
  }


  @Transactional
  public ConsumerRole assignAppRoleToConsumer(String token, String appId) {
    Long consumerId = getConsumerIdByToken(token);
    return assignAppRoleToConsumer(consumerId, appId);
  }

  @Transactional
  public ConsumerRole assignAppRoleToConsumer(Long consumerId, String appId) {
    if (consumerId == null) {
      throw new BadRequestException("Token is Illegal");
    }

    Role masterRole = rolePermissionService.findRoleByRoleName(RoleUtils.buildAppMasterRoleName(appId));
    if (masterRole == null) {
      throw new BadRequestException("App's role does not exist. Please check whether app has created.");
    }

    long roleId = masterRole.getId();
    ConsumerRole managedModifyRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, roleId);
    if (managedModifyRole != null) {
      return managedModifyRole;
    }

    String operator = userInfoHolder.getUser().getUserId();
    ConsumerRole consumerRole = createConsumerRole(consumerId, roleId, operator);
    return consumerRoleRepository.save(consumerRole);
  }

  @Transactional
  public void createConsumerAudits(Iterable<ConsumerAudit> consumerAudits) {
    consumerAuditRepository.saveAll(consumerAudits);
  }

  @Transactional
  public ConsumerToken createConsumerToken(ConsumerToken entity) {
    entity.setId(0); //for protection
    return consumerTokenRepository.save(entity);
  }

  private ConsumerToken generateConsumerToken(Consumer consumer, Integer rateLimit, Date expires) {
    long consumerId = consumer.getId();
    String createdBy = userInfoHolder.getUser().getUserId();
    Date createdTime = new Date();

    if (rateLimit == null || rateLimit < 0) {
      rateLimit = 0;
    }

    ConsumerToken consumerToken = new ConsumerToken();
    consumerToken.setConsumerId(consumerId);
    consumerToken.setRateLimit(rateLimit);
    consumerToken.setExpires(expires);
    consumerToken.setDataChangeCreatedBy(createdBy);
    consumerToken.setDataChangeCreatedTime(createdTime);
    consumerToken.setDataChangeLastModifiedBy(createdBy);
    consumerToken.setDataChangeLastModifiedTime(createdTime);

    generateAndEnrichToken(consumer, consumerToken);

    return consumerToken;
  }

  void generateAndEnrichToken(Consumer consumer, ConsumerToken consumerToken) {

    Preconditions.checkArgument(consumer != null);

    if (consumerToken.getDataChangeCreatedTime() == null) {
      consumerToken.setDataChangeCreatedTime(new Date());
    }
    consumerToken.setToken(generateToken(consumer.getAppId(), consumerToken
        .getDataChangeCreatedTime(), portalConfig.consumerTokenSalt()));
  }

  @SuppressWarnings("UnstableApiUsage")
  String generateToken(String consumerAppId, Date generationTime, String consumerTokenSalt) {
    return Hashing.sha256().hashString(KEY_JOINER.join(consumerAppId, TIMESTAMP_FORMAT.format
        (generationTime), consumerTokenSalt), Charsets.UTF_8).toString();
  }

  ConsumerRole createConsumerRole(Long consumerId, Long roleId, String operator) {
    ConsumerRole consumerRole = new ConsumerRole();

    consumerRole.setConsumerId(consumerId);
    consumerRole.setRoleId(roleId);
    consumerRole.setDataChangeCreatedBy(operator);
    consumerRole.setDataChangeLastModifiedBy(operator);

    return consumerRole;
  }

  public Set<String> findAppIdsAuthorizedByConsumerId(long consumerId) {
    List<ConsumerRole> consumerRoles = this.findConsumerRolesByConsumerId(consumerId);
    List<Long> roleIds = consumerRoles.stream().map(ConsumerRole::getRoleId)
        .collect(Collectors.toList());

    return this.findAppIdsByRoleIds(roleIds);
  }

  private List<ConsumerRole> findConsumerRolesByConsumerId(long consumerId) {
    return this.consumerRoleRepository.findByConsumerId(consumerId);
  }

  private Set<String> findAppIdsByRoleIds(List<Long> roleIds) {
    Iterable<Role> roleIterable = this.roleRepository.findAllById(roleIds);

    Set<String> appIds = new HashSet<>();

    roleIterable.forEach(role -> {
      if (!role.isDeleted()) {
        String roleName = role.getRoleName();
        String appId = RoleUtils.extractAppIdFromRoleName(roleName);
        appIds.add(appId);
      }
    });

    return appIds;
  }

  List<Consumer> findAllConsumer(Pageable page) {
    return this.consumerRepository.findAll(page).getContent();
  }

  public List<ConsumerInfo> findConsumerInfoList(Pageable page) {
    List<Consumer> consumerList = findAllConsumer(page);
    List<Long> consumerIdList = consumerList.stream()
        .map(Consumer::getId).collect(Collectors.toList());
    List<Boolean> allowCreateApplicationList = isAllowCreateApplication(consumerIdList);
    List<Integer> rateLimitList = getRateLimit(consumerIdList);

    List<ConsumerInfo> consumerInfoList = new ArrayList<>(consumerList.size());

    for (int i = 0; i < consumerList.size(); i++) {
      Consumer consumer = consumerList.get(i);
      // without token
      ConsumerInfo consumerInfo = convert(
          consumer, null, allowCreateApplicationList.get(i), rateLimitList.get(i)
      );
      consumerInfoList.add(consumerInfo);
    }

    return consumerInfoList;
  }

  @Transactional
  public void deleteConsumer(String appId) {
    Consumer consumer = consumerRepository.findByAppId(appId);
    if (consumer == null) {
      throw new BadRequestException("ConsumerApp not exist");
    }
    long consumerId = consumer.getId();
    List<ConsumerRole> consumerRoleList = consumerRoleRepository.findByConsumerId(consumerId);
    ConsumerToken consumerToken = consumerTokenRepository.findByConsumerId(consumerId);

    consumerRoleRepository.deleteAll(consumerRoleList);
    consumerRepository.delete(consumer);

    if (Objects.nonNull(consumerToken)) {
      consumerTokenRepository.delete(consumerToken);
    }
  }

}
