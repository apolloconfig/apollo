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

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author : kl
 * Service discovery consul implementation
 **/
@Service
@Profile({"consul-discovery", "zookeeper-discovery"})
public class SpringCloudInnerDiscoveryService implements DiscoveryService {

    private final DiscoveryClient discoveryClient;

    public SpringCloudInnerDiscoveryService(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }


    @Override
    public List<ServiceDTO> getServiceInstances(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        List<ServiceDTO> serviceDTOList = Lists.newLinkedList();
        if (!CollectionUtils.isEmpty(instances)) {
            instances.forEach(instance -> {
                ServiceDTO serviceDTO = this.toServiceDTO(instance, serviceId);
                serviceDTOList.add(serviceDTO);
            });
        }
        return serviceDTOList;
    }

    private ServiceDTO toServiceDTO(ServiceInstance instance, String appName) {
        ServiceDTO service = new ServiceDTO();
        service.setAppName(appName);
        service.setInstanceId(instance.getInstanceId());
        String homePageUrl = "http://" + instance.getHost() + ":" + instance.getPort() + "/";
        service.setHomepageUrl(homePageUrl);
        return service;
    }
}
