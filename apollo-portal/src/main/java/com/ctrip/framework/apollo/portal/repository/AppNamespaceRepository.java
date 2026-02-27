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

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface AppNamespaceRepository extends JpaRepository<AppNamespace, Long> {

  AppNamespace findByAppIdAndName(String appId, String namespaceName);

  AppNamespace findByName(String namespaceName);

  List<AppNamespace> findByNameAndIsPublic(String namespaceName, boolean isPublic);

  List<AppNamespace> findByIsPublicTrue();

  @Query("SELECT a.name FROM AppNamespace a WHERE a.isPublic = true AND a.isDeleted = false")
  List<String> findNamesByIsPublicTrue();

  List<AppNamespace> findByAppId(String appId);

  @Modifying
  @Query("UPDATE AppNamespace SET isDeleted = true, "
      + "deletedAt = :#{T(java.lang.System).currentTimeMillis()}, "
      + "dataChangeLastModifiedBy = :operator WHERE appId = :appId and isDeleted = false")
  int batchDeleteByAppId(@Param("appId") String appId, @Param("operator") String operator);

  @Modifying
  @Query("UPDATE AppNamespace SET isDeleted = true, "
      + "deletedAt = :#{T(java.lang.System).currentTimeMillis()}, "
      + "dataChangeLastModifiedBy = :operator WHERE appId = :appId and name = :namespaceName and isDeleted = false")
  int delete(@Param("appId") String appId, @Param("namespaceName") String namespaceName,
      @Param("operator") String operator);
}
