package com.ctrip.framework.apollo.biz.repository;

import com.ctrip.framework.apollo.biz.entity.Release;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ReleaseRepository extends PagingAndSortingRepository<Release, Long> {

  Release findFirstByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseOrderByIdDesc(@Param("appId") String appId, @Param("clusterName") String clusterName,
                                                                                         @Param("namespaceName") String namespaceName);

  Release findByIdAndIsAbandonedFalse(long id);

  List<Release> findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(String appId, String clusterName, String namespaceName, Pageable page);

  List<Release> findByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseOrderByIdDesc(String appId, String clusterName, String namespaceName, Pageable page);

  List<Release> findByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseAndIdBetweenOrderByIdDesc(String appId, String clusterName, String namespaceName, long fromId, long toId);

  List<Release> findByReleaseKeyIn(Set<String> releaseKey);

  List<Release> findByIdIn(Set<Long> releaseIds);

  @Modifying
  @Query("update Release set isdeleted=1,DataChange_LastModifiedBy = ?4 where appId=?1 and clusterName=?2 and namespaceName = ?3")
  int batchDelete(String appId, String clusterName, String namespaceName, String operator);

  // For release history conversion program, need to delete after conversion it done
  List<Release> findByAppIdAndClusterNameAndNamespaceNameOrderByIdAsc(String appId, String clusterName, String namespaceName);
}
