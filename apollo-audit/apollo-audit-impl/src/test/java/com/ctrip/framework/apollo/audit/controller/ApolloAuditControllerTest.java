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
package com.ctrip.framework.apollo.audit.controller;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.MockBeanFactory;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest
@ContextConfiguration(classes = ApolloAuditController.class)
public class ApolloAuditControllerTest {

  final int page = 0;
  final int size = 10;
  @Autowired
  ApolloAuditController apolloAuditController;
  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private ApolloAuditLogApi api;
  @MockBean
  private ApolloAuditProperties properties;

  @Test
  public void testFindAllAuditLogs() throws Exception {

    {
      List<ApolloAuditLogDTO> mockLogDTOList = MockBeanFactory.mockAuditLogDTOListByLength(size);
      Mockito.when(api.queryLogs(Mockito.eq(page), Mockito.eq(size))).thenReturn(mockLogDTOList);
    }

    mockMvc.perform(MockMvcRequestBuilders.get("/apollo/audit/logs")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(size));

    Mockito.verify(api, Mockito.times(1)).queryLogs(Mockito.eq(page), Mockito.eq(size));
  }

  @Test
  public void testFindTraceDetails() throws Exception {
    final String traceId = "query-trace-id";
    final int traceDetailsListLength = 3;
    {
      List<ApolloAuditLogDetailsDTO> mockDetailsDTOList = MockBeanFactory.mockTraceDetailsDTOListByLength(
          traceDetailsListLength);
      mockDetailsDTOList.forEach(e -> e.getLogDTO().setTraceId(traceId));
      Mockito.when(api.queryTraceDetails(Mockito.eq(traceId))).thenReturn(mockDetailsDTOList);
    }

    mockMvc.perform(MockMvcRequestBuilders.get("/apollo/audit/trace")
            .param("traceId", traceId))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(traceDetailsListLength))
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].logDTO.traceId").value(traceId));

    Mockito.verify(api, Mockito.times(1)).queryTraceDetails(Mockito.eq(traceId));
  }

  @Test
  public void testFindAllAuditLogsByOpNameAndTime() throws Exception {
    final String opName = "query-op-name";
    final Date startDate = new Date(2023, Calendar.OCTOBER, 15);
    final Date endDate = new Date(2023, Calendar.OCTOBER, 16);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    {
      List<ApolloAuditLogDTO> mockLogDTOList = MockBeanFactory.mockAuditLogDTOListByLength(size);
      mockLogDTOList.forEach(e -> {
        e.setOpName(opName);
      });
      Mockito.when(
          api.queryLogsByOpName(Mockito.eq(opName), Mockito.eq(startDate), Mockito.eq(endDate),
              Mockito.eq(page), Mockito.eq(size))).thenReturn(mockLogDTOList);
    }

    mockMvc.perform(MockMvcRequestBuilders.get("/apollo/audit/logs/opName").param("opName", opName)
            .param("startDate", sdf.format(startDate))
            .param("endDate", sdf.format(endDate))
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(size))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].opName").value(opName));

    Mockito.verify(api, Mockito.times(1))
        .queryLogsByOpName(Mockito.eq(opName), Mockito.eq(startDate), Mockito.eq(endDate),
            Mockito.eq(page), Mockito.eq(size));
  }

  @Test
  public void testFindDataInfluencesByField() throws Exception {
    final String entityName = "query-entity-name";
    final String entityId = "query-entity-id";
    final String fieldName = "query-field-name";
    {
      List<ApolloAuditLogDataInfluenceDTO> mockDataInfluenceDTOList = MockBeanFactory.mockDataInfluenceDTOListByLength(
          size);
      mockDataInfluenceDTOList.forEach(e -> {
        e.setInfluenceEntityName(entityName);
        e.setInfluenceEntityId(entityId);
        e.setFieldName(fieldName);
      });
      Mockito.when(api.queryDataInfluencesByField(Mockito.eq(entityName), Mockito.eq(entityId),
              Mockito.eq(fieldName), Mockito.eq(page), Mockito.eq(size)))
          .thenReturn(mockDataInfluenceDTOList);
    }

    mockMvc.perform(MockMvcRequestBuilders.get("/apollo/audit/logs/dataInfluences/field")
            .param("entityName", entityName)
            .param("entityId", entityId)
            .param("fieldName", fieldName)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(size));

    Mockito.verify(api, Mockito.times(1))
        .queryDataInfluencesByField(Mockito.eq(entityName), Mockito.eq(entityId),
            Mockito.eq(fieldName), Mockito.eq(page), Mockito.eq(size));
  }


}
