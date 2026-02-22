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

import com.ctrip.framework.apollo.biz.entity.Release;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ReleaseRepository extends JpaRepository<Release, Long> {

  Release findFirstByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseOrderByIdDesc(
      @Param("appId") String appId, @Param("clusterName") String clusterName,
      @Param("namespaceName") String namespaceName);

  Release findByIdAndIsAbandonedFalse(long id);

  Release findByReleaseKey(String releaseKey);

  List<Release> findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(String appId,
      String clusterName, String namespaceName, Pageable page);

  List<Release> findByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseOrderByIdDesc(
      String appId, String clusterName, String namespaceName, Pageable page);

  List<Release> findByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseAndIdBetweenOrderByIdDesc(
      String appId, String clusterName, String namespaceName, long fromId, long toId);

  List<Release> findByReleaseKeyIn(Set<String> releaseKeys);

  List<Release> findByIdIn(Set<Long> releaseIds);

  @Modifying
  @Query("update Release set isDeleted = true, "
      + "deletedAt = :#{T(java.lang.System).currentTimeMillis()}, "
      + "dataChangeLastModifiedBy = ?4 where appId=?1 and clusterName=?2 "
      + "and namespaceName = ?3 and isDeleted = false")
  int batchDelete(String appId, String clusterName, String namespaceName, String operator);

  // For release history conversion program, need to delete after conversion it done
  List<Release> findByAppIdAndClusterNameAndNamespaceNameOrderByIdAsc(String appId,
      String clusterName, String namespaceName);
}
