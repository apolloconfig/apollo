package com.ctrip.framework.apollo.portal.controller;


import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.GlobalSearchValueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
public class GlobalSearchValueController {

    private final PortalSettings portalSettings;
    private final GlobalSearchValueService globalSearchValueService;

    public GlobalSearchValueController(final PortalSettings portalSettings, final GlobalSearchValueService globalSearchValueService) {
        this.portalSettings = portalSettings;
        this.globalSearchValueService = globalSearchValueService;
    }

    @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
    @GetMapping("/global-search/item-info/by-key-or-value")
    public List<ItemInfo> get_ItemInfo_BySearch(@RequestParam(value = "key", required = false, defaultValue = "") String key,
                                                @RequestParam(value = "value", required = false, defaultValue = "") String value) {
        List<ItemInfo> allEnvItemInfos = new ArrayList<>();
        List<Env> allEnvs = portalSettings.getAllEnvs();
        allEnvs.forEach(env -> {
            List<ItemInfo> perEnvItemInfos = globalSearchValueService.get_PerEnv_ItemInfo_BySearch(env, key, value);
            allEnvItemInfos.addAll(perEnvItemInfos);
        });
        return allEnvItemInfos;
    }

}
