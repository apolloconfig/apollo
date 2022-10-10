/*
 * Copyright 2022 Apollo Authors
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

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import java.time.LocalDateTime;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ServiceRegistryRepository extends PagingAndSortingRepository<ServiceRegistry, Long> {

  List<ServiceRegistry> findByServiceName(String serviceName);

  ServiceRegistry findByServiceNameAndUri(String serviceName, String uri);

  @Modifying
  @Transactional
  List<ServiceRegistry> deleteByDataChangeLastModifiedTimeLessThan(LocalDateTime localDateTime);

  @Modifying
  @Transactional
  int deleteByServiceNameAndUri(String serviceName, String uri);

  /**
   * use time in database instead of JVM
   */
  @Query(value = "SELECT CURRENT_TIMESTAMP", nativeQuery = true)
  LocalDateTime currentTimestamp();
}