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

import com.ctrip.framework.apollo.common.dto.ItemInfoDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.http.SearchResponseEntity;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GlobalSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchService.class);
    private final AdminServiceAPI.ItemAPI itemAPI;
    private final PortalSettings portalSettings;

    public GlobalSearchService(AdminServiceAPI.ItemAPI itemAPI, PortalSettings portalSettings) {
        this.itemAPI = itemAPI;
        this.portalSettings = portalSettings;
    }

    public SearchResponseEntity<List<ItemInfo>> getAllEnvItemInfoBySearch(String key, String value, int page, int size) {
        List<Env> activeEnvs = portalSettings.getActiveEnvs();
        List<String> envBeyondLimit = new ArrayList<>();
        AtomicBoolean hasMoreData = new AtomicBoolean(false);
        List<ItemInfo> allEnvItemInfos = new ArrayList<>();
        activeEnvs.forEach(env -> {
            PageDTO<ItemInfoDTO> perEnvItemInfoDTOs = itemAPI.getPerEnvItemInfoBySearch(env, key, value, page, size);
            if (!perEnvItemInfoDTOs.hasContent()) {
                return;
            }
            perEnvItemInfoDTOs.getContent().forEach(itemInfoDTO -> {
                try {
                    ItemInfo itemInfo = new ItemInfo(itemInfoDTO.getAppId(),env.getName(),itemInfoDTO.getClusterName(),itemInfoDTO.getNamespaceName(),itemInfoDTO.getKey(),itemInfoDTO.getValue());
                    allEnvItemInfos.add(itemInfo);
                } catch (Exception e) {
                    LOGGER.error("Error converting ItemInfoDTO to ItemInfo for item: {}", itemInfoDTO, e);
                }
            });
            if(perEnvItemInfoDTOs.getTotal() > size){
                envBeyondLimit.add(env.getName());
                hasMoreData.set(true);
            }
        });
        if(hasMoreData.get()){
            return SearchResponseEntity.okWithMessage(allEnvItemInfos,String.format(
                    "In %s , more than %d items found (Exceeded the maximum search quantity for a single environment). Please enter more precise criteria to narrow down the search scope.",
                    String.join(" , ", envBeyondLimit), size));
        }
        return SearchResponseEntity.ok(allEnvItemInfos);
    }

}
