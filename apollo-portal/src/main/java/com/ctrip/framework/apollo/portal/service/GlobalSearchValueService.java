package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.dto.ItemInfoDTO;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GlobalSearchValueService {

    private final AdminServiceAPI.ItemAPI itemAPI;

    public GlobalSearchValueService(AdminServiceAPI.ItemAPI itemAPI) {
        this.itemAPI = itemAPI;
    }

    public List<ItemInfo> get_PerEnv_ItemInfo_BySearch(Env env, String key, String value) {
        List<ItemInfo> perEnvItemInfos = new ArrayList<>();
        List<ItemInfoDTO> perEnvItemInfoDTOs = itemAPI.getPerEnvItemInfoBySearch(env, key, value);
        perEnvItemInfoDTOs.forEach(itemInfoDTO -> {
            ItemInfo itemInfo = new ItemInfo(itemInfoDTO.getAppId(),itemInfoDTO.getAppName(),env.getName(),itemInfoDTO.getClusterName(),itemInfoDTO.getNamespaceName(),itemInfoDTO.getStatus(),itemInfoDTO.getKey(),itemInfoDTO.getValue());
            perEnvItemInfos.add(itemInfo);
        });
        return perEnvItemInfos;
    }

}
