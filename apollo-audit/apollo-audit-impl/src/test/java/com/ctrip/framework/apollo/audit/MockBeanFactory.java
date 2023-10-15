package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
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
