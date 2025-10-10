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
package com.ctrip.framework.apollo.portal.repository;

import com.ctrip.framework.apollo.portal.entity.po.Permission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface PermissionRepository extends PagingAndSortingRepository<Permission, Long> {

  /**
   * find permission by permission type and targetId
   */
  Permission findTopByPermissionTypeAndTargetId(String permissionType, String targetId);

  /**
   * find permissions by permission types and targetId
   */
  List<Permission> findByPermissionTypeInAndTargetId(Collection<String> permissionTypes,
      String targetId);

  @Query("SELECT p.id from Permission p where p.targetId like ?1 or p.targetId like CONCAT(?1, '+%')")
  List<Long> findPermissionIdsByAppId(String appId);

  @Query("SELECT p.id from Permission p "
      + "where ("
      + "p.targetId like CONCAT(?1, '+', ?2) OR p.targetId like CONCAT(?1, '+', ?2, '+%')"
      + ") AND ( "
      + "p.permissionType = 'ModifyNamespace' OR p.permissionType = 'ReleaseNamespace'"
      + ")")
  List<Long> findPermissionIdsByAppIdAndNamespace(String appId, String namespaceName);

  @Modifying
  @Query("UPDATE Permission SET IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000), DataChange_LastModifiedBy = ?2 WHERE Id in ?1 and IsDeleted = false")
  Integer batchDelete(List<Long> permissionIds, String operator);

  @Query("SELECT p.id from Permission p where p.targetId = CONCAT(?1, '+', ?2, '+', ?3)"
      + " AND ( p.permissionType = 'ModifyNamespacesInCluster' OR p.permissionType = 'ReleaseNamespacesInCluster')")
  List<Long> findPermissionIdsByAppIdAndEnvAndCluster(String appId, String env, String clusterName);

  @Query("SELECT DISTINCT p " + "FROM UserRole ur "
      + "JOIN RolePermission rp ON ur.roleId = rp.roleId "
      + "JOIN Permission p ON rp.permissionId = p.id " + "WHERE ur.userId = :userId "
      + "AND ur.isDeleted = false " + "AND rp.isDeleted = false " + "AND p.isDeleted = false")
  List<Permission> findUserPermissions(@Param("userId") String userId);

  @Query("SELECT DISTINCT p " + "FROM ConsumerRole cr "
      + "JOIN RolePermission rp ON cr.roleId = rp.roleId "
      + "JOIN Permission p ON rp.permissionId = p.id " + "WHERE cr.consumerId = :consumerId "
      + "AND cr.isDeleted = false " + "AND rp.isDeleted = false " + "AND p.isDeleted = false")
  List<Permission> findConsumerPermissions(@Param("consumerId") long consumerId);
}