/*
 * Copyright 2021 Apollo Authors
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

import java.util.Collections;
import java.util.List;

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Lists;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Service discovery zookeeper implementation
 */
@Service
@Profile({"zookeeper-discovery"})
@ConditionalOnClass(name = {"org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryClient"})
public class ZookeeperDiscoveryService implements DiscoveryService {

	private final DiscoveryClient discoveryClient;

	public ZookeeperDiscoveryService(DiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	@Override
	public List<ServiceDTO> getServiceInstances(String serviceId) {

		if (discoveryClient == null || CollectionUtils.isEmpty(discoveryClient.getInstances(serviceId))) {
			Tracer.logEvent("Apollo.Discovery.NotFound", serviceId);
			return Collections.emptyList();
		}

		List<ServiceDTO> serviceDTOList = Lists.newLinkedList();
		discoveryClient.getInstances(serviceId).forEach(instance -> {
			ServiceDTO serviceDTO = this.toServiceDTO(instance, serviceId);
			serviceDTOList.add(serviceDTO);
		});

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
