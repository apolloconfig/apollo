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
package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;


public class ApolloAuditLogDataInfluenceService {

  private final ApolloAuditLogDataInfluenceRepository dataInfluenceRepository;

  public ApolloAuditLogDataInfluenceService(
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    this.dataInfluenceRepository = dataInfluenceRepository;
  }

  public ApolloAuditLogDataInfluence save(ApolloAuditLogDataInfluence dataInfluence) {
    return dataInfluenceRepository.save(dataInfluence);
  }

  public List<ApolloAuditLogDataInfluence> findBySpanId(String spanId) {
    return dataInfluenceRepository.findBySpanId(spanId);
  }

  public List<ApolloAuditLogDataInfluence> findByEntityNameAndEntityIdAndFieldName(
      String entityName, String entityId, String fieldName, int page, int size) {
    Pageable pageable = pageSortByTime(page, size);
    return dataInfluenceRepository.findByInfluenceEntityNameAndInfluenceEntityIdAndFieldName(
        entityName, entityId, fieldName, pageable);
  }

  Pageable pageSortByTime(int page, int size) {
    return PageRequest.of(page, size, Sort.by(new Order(Direction.DESC, "DataChangeCreatedTime")));
  }

}
