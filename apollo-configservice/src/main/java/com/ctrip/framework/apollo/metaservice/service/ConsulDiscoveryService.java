package com.ctrip.framework.apollo.metaservice.service;

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author : kl
 * Service discovery consul implementation
 **/
@Service
@Profile({"consul-discovery"})
public class ConsulDiscoveryService implements DiscoveryService {

    private final ConsulDiscoveryClient discoveryClient;

    public ConsulDiscoveryService(ConsulDiscoveryClient discoveryClient) {
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
