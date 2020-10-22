package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.ReleaseHistory;
import com.ctrip.framework.apollo.biz.service.ReleaseHistoryService;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseHistoryDTO;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RestController
public class ReleaseHistoryController {

  private static final Gson GSON = new Gson();

  private Type configurationTypeReference = new TypeToken<Map<String, Object>>() {
  }.getType();

  private final ReleaseHistoryService releaseHistoryService;

  public ReleaseHistoryController(final ReleaseHistoryService releaseHistoryService) {
    this.releaseHistoryService = releaseHistoryService;
  }

  @GetMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/histories")
  public PageDTO<ReleaseHistoryDTO> findReleaseHistoriesByNamespace(
      @PathVariable String appId, @PathVariable String clusterName,
      @PathVariable String namespaceName,
      Pageable pageable) {

    Page<ReleaseHistory> result = releaseHistoryService.findReleaseHistoriesByNamespace(appId, clusterName,
                                                                                        namespaceName, pageable);
    return transform2PageDTO(result, pageable);
  }


  @GetMapping("/releases/histories/by_release_id_and_operation")
  public PageDTO<ReleaseHistoryDTO> findReleaseHistoryByReleaseIdAndOperation(
      @RequestParam("releaseId") long releaseId,
      @RequestParam("operation") int operation,
      Pageable pageable) {

    Page<ReleaseHistory> result = releaseHistoryService.findByReleaseIdAndOperation(releaseId, operation, pageable);

    return transform2PageDTO(result, pageable);
  }

  @GetMapping("/releases/histories/by_previous_release_id_and_operation")
  public PageDTO<ReleaseHistoryDTO> findReleaseHistoryByPreviousReleaseIdAndOperation(
      @RequestParam("previousReleaseId") long previousReleaseId,
      @RequestParam("operation") int operation,
      Pageable pageable) {

    Page<ReleaseHistory> result = releaseHistoryService.findByPreviousReleaseIdAndOperation(previousReleaseId, operation, pageable);

    return transform2PageDTO(result, pageable);

  }

  private PageDTO<ReleaseHistoryDTO> transform2PageDTO(Page<ReleaseHistory> releaseHistoriesPage, Pageable pageable){
    if (!releaseHistoriesPage.hasContent()) {
      return null;
    }

    List<ReleaseHistory> releaseHistories = releaseHistoriesPage.getContent();
    List<ReleaseHistoryDTO> releaseHistoryDTOs = new ArrayList<>(releaseHistories.size());
    for (ReleaseHistory releaseHistory : releaseHistories) {
      releaseHistoryDTOs.add(transformReleaseHistory2DTO(releaseHistory));
    }

    return new PageDTO<>(releaseHistoryDTOs, pageable, releaseHistoriesPage.getTotalElements());
  }

  private ReleaseHistoryDTO transformReleaseHistory2DTO(ReleaseHistory releaseHistory) {
    ReleaseHistoryDTO dto = new ReleaseHistoryDTO();
    BeanUtils.copyProperties(releaseHistory, dto, "operationContext");
    dto.setOperationContext(GSON.fromJson(releaseHistory.getOperationContext(),
                                          configurationTypeReference));

    return dto;
  }
}
