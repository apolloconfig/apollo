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
package com.ctrip.framework.apollo.portal.repository;

import com.ctrip.framework.apollo.portal.entity.po.Role;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * find role by role name
   */
  Role findTopByRoleName(String roleName);

  @Query("SELECT r.id from Role r where r.roleName like CONCAT('Master+', ?1) "
      + "OR r.roleName like CONCAT('ModifyNamespace+', ?1, '+%') "
      + "OR r.roleName like CONCAT('ReleaseNamespace+', ?1, '+%')  "
      + "OR r.roleName like CONCAT('ManageAppMaster+', ?1) "
      + "OR r.roleName like CONCAT('ModifyNamespacesInCluster+', ?1, '+%')"
      + "OR r.roleName like CONCAT('ReleaseNamespacesInCluster+', ?1, '+%')")
  List<Long> findRoleIdsByAppId(String appId);

  @Query("SELECT r.id from Role r where r.roleName like CONCAT('ModifyNamespace+', ?1, '+', ?2) "
      + "OR r.roleName like CONCAT('ModifyNamespace+', ?1, '+', ?2, '+%') "
      + "OR r.roleName like CONCAT('ReleaseNamespace+', ?1, '+', ?2) "
      + "OR r.roleName like CONCAT('ReleaseNamespace+', ?1, '+', ?2, '+%')")
  List<Long> findRoleIdsByAppIdAndNamespace(String appId, String namespaceName);

  @Modifying
  @Query("UPDATE Role SET isDeleted = true, "
      + "deletedAt = :#{T(java.lang.System).currentTimeMillis()}, "
      + "dataChangeLastModifiedBy = :operator WHERE id in :roleIds and isDeleted = false")
  Integer batchDelete(@Param("roleIds") List<Long> roleIds, @Param("operator") String operator);

  @Query("SELECT r.id from Role r where r.roleName = CONCAT('ModifyNamespacesInCluster+', ?1, '+', ?2, '+', ?3) "
      + "OR r.roleName = CONCAT('ReleaseNamespacesInCluster+', ?1, '+', ?2, '+', ?3)")
  List<Long> findRoleIdsByCluster(String appId, String env, String clusterName);
}
