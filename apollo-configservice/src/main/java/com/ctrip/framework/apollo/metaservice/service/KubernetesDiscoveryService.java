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
package com.ctrip.framework.apollo.metaservice.service;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * This is a simple implementation that skips any service discovery and just return what is configured
 *
 * <ul>
 *   <li>getServiceInstances("apollo-configservice") returns ${apollo.config-service.url}</li>
 *   <li>getServiceInstances("apollo-adminservice") returns ${apollo.admin-service.url}</li>
 * </ul>
 */
@Service
@Profile({"kubernetes", "custom-defined-discovery"})
public class KubernetesDiscoveryService implements DiscoveryService {
  private static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
  private static final Map<String, String> SERVICE_ID_TO_CONFIG_NAME = ImmutableMap
      .of(ServiceNameConsts.APOLLO_CONFIGSERVICE, "apollo.config-service.url",
          ServiceNameConsts.APOLLO_ADMINSERVICE, "apollo.admin-service.url");

  private final BizConfig bizConfig;

  public KubernetesDiscoveryService(final BizConfig bizConfig) {
    this.bizConfig = bizConfig;
  }

  @Override
  public List<ServiceDTO> getServiceInstances(String serviceId) {
    String configName = SERVICE_ID_TO_CONFIG_NAME.get(serviceId);
    if (configName == null) {
      return Collections.emptyList();
    }

    return assembleServiceDTO(serviceId, bizConfig.getValue(configName));
  }

  private List<ServiceDTO> assembleServiceDTO(String serviceId, String directUrl) {
    if (Strings.isNullOrEmpty(directUrl)) {
      return Collections.emptyList();
    }
    List<ServiceDTO> serviceDTOList = Lists.newLinkedList();
    COMMA_SPLITTER.split(directUrl).forEach(url -> {
      ServiceDTO service = new ServiceDTO();
      service.setAppName(serviceId);
      service.setInstanceId(String.format("%s:%s", serviceId, url));
      service.setHomepageUrl(url);
      serviceDTOList.add(service);
    });

    return serviceDTOList;
  }
}
