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

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ServiceRegistryRepository extends PagingAndSortingRepository<ServiceRegistry, Long> {

  List<ServiceRegistry> findByServiceNameAndDataChangeLastModifiedTimeGreaterThan(
      String serviceName, LocalDateTime localDateTime
  );

  ServiceRegistry findByServiceNameAndUri(String serviceName, String uri);

  List<ServiceRegistry> deleteByDataChangeLastModifiedTimeLessThan(LocalDateTime localDateTime);

  int deleteByServiceNameAndUri(String serviceName, String uri);
}
