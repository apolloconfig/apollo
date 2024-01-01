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
package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import java.util.ArrayList;
import java.util.List;

public class MockBeanFactory {

  public static ApolloAuditLogDTO mockAuditLogDTO() {
    return new ApolloAuditLogDTO();
  }

  public static List<ApolloAuditLogDTO> mockAuditLogDTOListByLength(int length) {
    List<ApolloAuditLogDTO> mockList = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      mockList.add(mockAuditLogDTO());
    }
    return mockList;
  }

  public static ApolloAuditLog mockAuditLog() {
    return new ApolloAuditLog();
  }

  public static List<ApolloAuditLog> mockAuditLogListByLength(int length) {
    List<ApolloAuditLog> mockList = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      mockList.add(mockAuditLog());
    }
    return mockList;
  }

  public static List<ApolloAuditLogDetailsDTO> mockTraceDetailsDTOListByLength(int length) {
    List<ApolloAuditLogDetailsDTO> mockList = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      ApolloAuditLogDetailsDTO dto = new ApolloAuditLogDetailsDTO();
      dto.setLogDTO(mockAuditLogDTO());
      mockList.add(dto);
    }
    return mockList;
  }

  public static ApolloAuditLogDataInfluenceDTO mockDataInfluenceDTO() {
    return new ApolloAuditLogDataInfluenceDTO();
  }

  public static List<ApolloAuditLogDataInfluenceDTO> mockDataInfluenceDTOListByLength(int length) {
    List<ApolloAuditLogDataInfluenceDTO> mockList = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      mockList.add(mockDataInfluenceDTO());
    }
    return mockList;
  }

  public static ApolloAuditLogDataInfluence mockDataInfluence() {
    return new ApolloAuditLogDataInfluence();
  }

  public static List<ApolloAuditLogDataInfluence> mockDataInfluenceListByLength(int length) {
    List<ApolloAuditLogDataInfluence> mockList = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      mockList.add(mockDataInfluence());
    }
    return mockList;
  }

  public static MockDataInfluenceEntity mockDataInfluenceEntity() {
    return new MockDataInfluenceEntity();
  }

  public static List<Object> mockDataInfluenceEntityListByLength(int length) {
    List<Object> mockList = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      MockDataInfluenceEntity e = mockDataInfluenceEntity();
      e.setId(i+1);
      mockList.add(e);
    }
    return mockList;
  }

}
