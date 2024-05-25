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

import com.ctrip.framework.apollo.portal.entity.po.Role;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface RoleRepository extends PagingAndSortingRepository<Role, Long> {

  /**
   * find role by role name
   */
  Role findTopByRoleName(String roleName);

  @Query("SELECT r.id from Role r where r.roleName like CONCAT('Master+', ?1) "
      + "OR r.roleName like CONCAT('ModifyNamespace+', ?1, '+%') "
      + "OR r.roleName like CONCAT('ReleaseNamespace+', ?1, '+%')  "
      + "OR r.roleName like CONCAT('ManageAppMaster+', ?1)")
  List<Long> findRoleIdsByAppId(String appId);

  @Query("SELECT r.id from Role r where r.roleName like CONCAT('ModifyNamespace+', ?1, '+', ?2) "
      + "OR r.roleName like CONCAT('ModifyNamespace+', ?1, '+', ?2, '+%') "
      + "OR r.roleName like CONCAT('ReleaseNamespace+', ?1, '+', ?2) "
      + "OR r.roleName like CONCAT('ReleaseNamespace+', ?1, '+', ?2, '+%')")
  List<Long> findRoleIdsByAppIdAndNamespace(String appId, String namespaceName);

  @Modifying
  @Query("UPDATE Role SET IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000), DataChange_LastModifiedBy = ?2 WHERE Id in ?1 and IsDeleted = false")
  Integer batchDelete(List<Long> roleIds, String operator);
}
