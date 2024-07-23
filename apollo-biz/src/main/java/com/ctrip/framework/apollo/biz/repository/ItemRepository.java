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
package com.ctrip.framework.apollo.biz.repository;

import com.ctrip.framework.apollo.biz.entity.Item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ItemRepository extends PagingAndSortingRepository<Item, Long> {

  Item findByNamespaceIdAndKey(Long namespaceId, String key);

  List<Item> findByNamespaceIdOrderByLineNumAsc(Long namespaceId);

  List<Item> findByNamespaceId(Long namespaceId);

  List<Item> findByNamespaceIdAndDataChangeLastModifiedTimeGreaterThan(Long namespaceId, Date date);

  Page<Item> findByKey(String key, Pageable pageable);

  Page<Item> findByNamespaceId(Long namespaceId, Pageable pageable);
  
  Item findFirst1ByNamespaceIdOrderByLineNumDesc(Long namespaceId);

  @Query(nativeQuery = true, value =
          "SELECT "+
          "  n.AppId AS appId, " +
          "  a.Name AS appName, " +
          "  n.ClusterName AS clusterName, " +
          "  n.NamespaceName AS namespaceName, " +
          "  i.`Key` AS `key`, " +
          "  i.Value AS value " +
          "FROM " +
          "  item i " +
          "RIGHT JOIN namespace n ON i.NamespaceId = n.Id " +
          "LEFT JOIN app a ON n.AppId = a.AppId " +
          "WHERE " +
          "  i.`Key` LIKE %:key% " +
          "  AND i.Value LIKE %:value% " +
          "  AND i.IsDeleted = 0")
  List<Object[]> findItemsByKeyAndValueLike(@Param("key")String key, @Param("value")String value);

  @Query(nativeQuery = true, value =
          "SELECT "+
          "  n.AppId AS appId, " +
          "  a.Name AS appName, " +
          "  n.ClusterName AS clusterName, " +
          "  n.NamespaceName AS namespaceName, " +
          "  i.`Key` AS `key`, " +
          "  i.Value AS value " +
          "FROM " +
          "  item i " +
          "RIGHT JOIN namespace n ON i.NamespaceId = n.Id " +
          "LEFT JOIN app a ON n.AppId = a.AppId " +
          "WHERE " +
          "  i.Value LIKE %:value% " +
          "  AND i.IsDeleted = 0")
  List<Object[]> findItemsByValueLike(@Param("value")String value);


  @Query(nativeQuery = true, value =
          "SELECT"+
          "  n.AppId AS appId, " +
          "  a.Name AS appName, " +
          "  n.ClusterName AS clusterName, " +
          "  n.NamespaceName AS namespaceName, " +
          "  i.`Key` AS `key`, " +
          "  i.Value AS value " +
          "FROM " +
          "  item i " +
          "RIGHT JOIN namespace n ON i.NamespaceId = n.Id " +
          "LEFT JOIN app a ON n.AppId = a.AppId " +
          "WHERE " +
          "  i.`Key` LIKE %:key% " +
          "  AND i.IsDeleted = 0")
  List<Object[]> findItemsByKeyLike(@Param("key")String key);

  @Query(nativeQuery = true, value =
          "SELECT "+
          "  n.AppId AS appId, " +
          "  a.Name AS appName, " +
          "  n.ClusterName AS clusterName, " +
          "  n.NamespaceName AS namespaceName, " +
          "  i.`Key` AS `key`, " +
          "  i.Value AS value " +
          "FROM " +
          "  item i " +
          "RIGHT JOIN namespace n ON i.NamespaceId = n.Id " +
          "LEFT JOIN app a ON n.AppId = a.AppId " +
          "WHERE " +
          "  i.IsDeleted = 0")
  List<Object[]> findAllItems();

  @Modifying
  @Query("update Item set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000), DataChange_LastModifiedBy = ?2 where NamespaceId = ?1 and IsDeleted = false")
  int deleteByNamespaceId(long namespaceId, String operator);

}
