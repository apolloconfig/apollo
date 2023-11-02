/*
 * Copyright 2023 Apollo Authors
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

  @Modifying
  @Query("update Item set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000), DataChange_LastModifiedBy = ?2 where NamespaceId = ?1 and IsDeleted = false")
  int deleteByNamespaceId(long namespaceId, String operator);

}
