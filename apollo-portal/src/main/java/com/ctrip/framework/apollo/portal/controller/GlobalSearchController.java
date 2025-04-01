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
package com.ctrip.framework.apollo.portal.controller;


import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.http.SearchResponseEntity;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.service.GlobalSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GlobalSearchController {
    private final GlobalSearchService globalSearchService;
    private final PortalConfig portalConfig;

    public GlobalSearchController(final GlobalSearchService globalSearchService, final PortalConfig portalConfig) {
        this.globalSearchService = globalSearchService;
        this.portalConfig = portalConfig;
    }

    @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
    @GetMapping("/global-search/item-info/by-key-or-value")
    public SearchResponseEntity<List<ItemInfo>> getItemInfoBySearch(@RequestParam(value = "key", required = false, defaultValue = "") String key,
                                                                    @RequestParam(value = "value", required = false , defaultValue = "") String value) {

        if(key.isEmpty() && value.isEmpty()) {
            throw new BadRequestException("Please enter at least one search criterion in either key or value.");
        }

        return globalSearchService.getAllEnvItemInfoBySearch(key, value, 0, portalConfig.getPerEnvSearchMaxResults());
    }

}
