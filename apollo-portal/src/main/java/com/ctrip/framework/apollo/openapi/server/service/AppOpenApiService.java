package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.common.http.MultiResponseEntity;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface AppOpenApiService {

  default void createApp(OpenCreateAppDTO req) {
    throw new UnsupportedOperationException();
  }

  List<OpenEnvClusterDTO> getEnvClusterInfo(String appId);

  List<OpenAppDTO> getAllApps();

  List<OpenAppDTO> getAppsInfo(List<String> appIds);

  List<OpenAppDTO> getAuthorizedApps();

  List<OpenAppDTO> getAppsBySelf(Set<String> appIds, Pageable page);

  MultiResponseEntity<EnvClusterInfo> getAppNavTree(String appId);
}
