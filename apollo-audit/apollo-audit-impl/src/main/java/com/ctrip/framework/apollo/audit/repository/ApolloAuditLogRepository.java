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
package com.ctrip.framework.apollo.audit.repository;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface ApolloAuditLogRepository extends PagingAndSortingRepository<ApolloAuditLog, Long> {

  List<ApolloAuditLog> findByTraceIdOrderByDataChangeCreatedTimeDesc(String traceId);

  List<ApolloAuditLog> findByOpName(String opName, Pageable page);

  List<ApolloAuditLog> findByOpNameAndDataChangeCreatedTimeGreaterThanEqualAndDataChangeCreatedTimeLessThanEqual(
      String opName, Date startDate, Date endDate, Pageable pageable);

  List<ApolloAuditLog> findByOpNameContainingOrOpTypeContainingOrOperatorContaining(String opName,
      String opType, String operator, Pageable pageable);
}
