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
package com.ctrip.framework.apollo.biz.repository;

import com.ctrip.framework.apollo.common.entity.AppNamespace;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;


public interface AppNamespaceRepository extends JpaRepository<AppNamespace, Long> {

  AppNamespace findByAppIdAndName(String appId, String namespaceName);

  List<AppNamespace> findByAppIdAndNameIn(String appId, Set<String> namespaceNames);

  AppNamespace findByNameAndIsPublicTrue(String namespaceName);

  List<AppNamespace> findByNameInAndIsPublicTrue(Set<String> namespaceNames);

  List<AppNamespace> findByAppIdAndIsPublic(String appId, boolean isPublic);

  List<AppNamespace> findByAppIdOrderByIdAsc(String appId);

  List<AppNamespace> findFirst500ByIdGreaterThanOrderByIdAsc(long id);

  @Modifying
  @Query("UPDATE AppNamespace SET isDeleted = true, "
      + "deletedAt = :#{T(java.lang.System).currentTimeMillis()}, "
      + "dataChangeLastModifiedBy = ?2 WHERE appId=?1 and isDeleted = false")
  int batchDeleteByAppId(String appId, String operator);

  @Modifying
  @Query("UPDATE AppNamespace SET isDeleted = true, "
      + "deletedAt = :#{T(java.lang.System).currentTimeMillis()}, "
      + "dataChangeLastModifiedBy = ?3 WHERE appId=?1 and name = ?2 and isDeleted = false")
  int delete(String appId, String namespaceName, String operator);
}
