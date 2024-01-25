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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseHistoryDTO;
import com.ctrip.framework.apollo.common.entity.EntityPair;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.ReleaseHistoryAPI;
import com.ctrip.framework.apollo.portal.enricher.adapter.BaseDtoUserInfoEnrichedAdapter;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.util.RelativeDateFormat;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.CollectionUtils;

@Service
public class ReleaseHistoryService {

  private final static Gson GSON = new Gson();
  
  private final AdminServiceAPI.ReleaseHistoryAPI releaseHistoryAPI;
  private final ReleaseService releaseService;
  private final AdditionalUserInfoEnrichService additionalUserInfoEnrichService;

  public ReleaseHistoryService(final ReleaseHistoryAPI releaseHistoryAPI,
      final ReleaseService releaseService,
      AdditionalUserInfoEnrichService additionalUserInfoEnrichService) {
    this.releaseHistoryAPI = releaseHistoryAPI;
    this.releaseService = releaseService;
    this.additionalUserInfoEnrichService = additionalUserInfoEnrichService;
  }


  public ReleaseHistoryBO findLatestByReleaseIdAndOperation(Env env, long releaseId, int operation){
    PageDTO<ReleaseHistoryDTO> pageDTO = releaseHistoryAPI.findByReleaseIdAndOperation(env, releaseId, operation, 0, 1);
    if (pageDTO != null && pageDTO.hasContent()){
      ReleaseHistoryDTO releaseHistory = pageDTO.getContent().get(0);
      ReleaseDTO release = releaseService.findReleaseById(env, releaseHistory.getReleaseId());
      return transformReleaseHistoryDTO2BO(releaseHistory, release);
    }

    return null;
  }

  public ReleaseHistoryBO findLatestByPreviousReleaseIdAndOperation(Env env, long previousReleaseId, int operation){
    PageDTO<ReleaseHistoryDTO> pageDTO = releaseHistoryAPI.findByPreviousReleaseIdAndOperation(env, previousReleaseId, operation, 0, 1);
    if (pageDTO != null && pageDTO.hasContent()){
      ReleaseHistoryDTO releaseHistory = pageDTO.getContent().get(0);
      ReleaseDTO release = releaseService.findReleaseById(env, releaseHistory.getReleaseId());
      return transformReleaseHistoryDTO2BO(releaseHistory, release);
    }

    return null;
  }

  public List<ReleaseHistoryBO> findNamespaceReleaseHistory(String appId, Env env, String clusterName,
                                                            String namespaceName, int page, int size) {
    PageDTO<ReleaseHistoryDTO> result = releaseHistoryAPI.findReleaseHistoriesByNamespace(appId, env, clusterName,
                                                                                          namespaceName, page, size);
    if (result == null || !result.hasContent()) {
      return Collections.emptyList();
    }

    List<ReleaseHistoryDTO> content = result.getContent();
    Set<Long> releaseIds = new HashSet<>();
    for (ReleaseHistoryDTO releaseHistoryDTO : content) {
      long releaseId = releaseHistoryDTO.getReleaseId();
      if (releaseId != 0) {
        releaseIds.add(releaseId);
      }
    }

    List<ReleaseDTO> releases = releaseService.findReleaseByIds(env, releaseIds);

    return transformReleaseHistoryDTO2BO(content, releases);
  }

  private List<ReleaseHistoryBO> transformReleaseHistoryDTO2BO(List<ReleaseHistoryDTO> source,
                                                               List<ReleaseDTO> releases) {
    if (CollectionUtils.isEmpty(source)) {
      return Collections.emptyList();
    }
    this.additionalUserInfoEnrichService.enrichAdditionalUserInfo(source, BaseDtoUserInfoEnrichedAdapter::new);

    Map<Long, ReleaseDTO> releasesMap = BeanUtils.mapByKey("id", releases);

    List<ReleaseHistoryBO> bos = new ArrayList<>(source.size());
    for (ReleaseHistoryDTO dto : source) {
      ReleaseDTO release = releasesMap.get(dto.getReleaseId());
      bos.add(transformReleaseHistoryDTO2BO(dto, release));
    }

    return bos;
  }

  private ReleaseHistoryBO transformReleaseHistoryDTO2BO(ReleaseHistoryDTO dto, ReleaseDTO release){
    ReleaseHistoryBO bo = new ReleaseHistoryBO();
    bo.setId(dto.getId());
    bo.setAppId(dto.getAppId());
    bo.setClusterName(dto.getClusterName());
    bo.setNamespaceName(dto.getNamespaceName());
    bo.setBranchName(dto.getBranchName());
    bo.setReleaseId(dto.getReleaseId());
    bo.setPreviousReleaseId(dto.getPreviousReleaseId());
    bo.setOperator(dto.getDataChangeCreatedBy());
    bo.setOperatorDisplayName(dto.getDataChangeCreatedByDisplayName());
    bo.setOperation(dto.getOperation());
    Date releaseTime = dto.getDataChangeLastModifiedTime();
    bo.setReleaseTime(releaseTime);
    bo.setReleaseTimeFormatted(RelativeDateFormat.format(releaseTime));
    bo.setOperationContext(dto.getOperationContext());
    //set release info
    setReleaseInfoToReleaseHistoryBO(bo, release);

    return bo;
  }
  private void setReleaseInfoToReleaseHistoryBO(ReleaseHistoryBO bo, ReleaseDTO release) {
    if (release != null) {
      bo.setReleaseTitle(release.getName());
      bo.setReleaseComment(release.getComment());
      bo.setReleaseAbandoned(release.isAbandoned());

      Map<String, String> configuration = GSON.fromJson(release.getConfigurations(), GsonType.CONFIG);
      List<EntityPair<String>> items = new ArrayList<>(configuration.size());
      for (Map.Entry<String, String> entry : configuration.entrySet()) {
        EntityPair<String> entityPair = new EntityPair<>(entry.getKey(), entry.getValue());
        items.add(entityPair);
      }
      bo.setConfiguration(items);

    } else {
      bo.setReleaseTitle("no release information");
      bo.setConfiguration(null);
    }
  }
}
