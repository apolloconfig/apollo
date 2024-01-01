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
package com.ctrip.framework.apollo.audit.dto;

import java.util.List;

/**
 * A combine of a log and its data influences
 */
public class ApolloAuditLogDetailsDTO {

  private ApolloAuditLogDTO logDTO;
  private List<ApolloAuditLogDataInfluenceDTO> dataInfluenceDTOList;

  public ApolloAuditLogDetailsDTO(ApolloAuditLogDTO logDTO,
      List<ApolloAuditLogDataInfluenceDTO> dataInfluenceDTOList) {
    this.logDTO = logDTO;
    this.dataInfluenceDTOList = dataInfluenceDTOList;
  }

  public ApolloAuditLogDetailsDTO() {
  }

  public ApolloAuditLogDTO getLogDTO() {
    return logDTO;
  }

  public void setLogDTO(ApolloAuditLogDTO logDTO) {
    this.logDTO = logDTO;
  }

  public List<ApolloAuditLogDataInfluenceDTO> getDataInfluenceDTOList() {
    return dataInfluenceDTOList;
  }

  public void setDataInfluenceDTOList(
      List<ApolloAuditLogDataInfluenceDTO> dataInfluenceDTOList) {
    this.dataInfluenceDTOList = dataInfluenceDTOList;
  }
}
