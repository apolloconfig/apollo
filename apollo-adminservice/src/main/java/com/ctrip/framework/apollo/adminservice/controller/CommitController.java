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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.service.CommitService;
import com.ctrip.framework.apollo.common.dto.CommitDTO;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class CommitController {

  private final CommitService commitService;

  public CommitController(final CommitService commitService) {
    this.commitService = commitService;
  }

  @GetMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/commit")
  public List<CommitDTO> find(@PathVariable String appId, @PathVariable String clusterName,
                              @PathVariable String namespaceName, @RequestParam(required = false) String key, Pageable pageable){

    List<Commit> commits;
    if (StringUtils.isEmpty(key)) {
      commits = commitService.find(appId, clusterName, namespaceName, pageable);
    } else {
      commits = commitService.findByKey(appId, clusterName, namespaceName, key, pageable);
    }
    return BeanUtils.batchTransform(CommitDTO.class, commits);
  }

}
