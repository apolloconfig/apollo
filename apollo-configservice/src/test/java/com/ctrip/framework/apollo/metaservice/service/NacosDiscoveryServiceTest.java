package com.ctrip.framework.apollo.metaservice.service;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kl (http://kailing.pub)
 * @since 2020/12/21
 */
@RunWith(MockitoJUnitRunner.class)
public class NacosDiscoveryServiceTest {

    private NacosDiscoveryService nacosDiscoveryService;

    @Mock
    private NacosNamingService nacosNamingService;

    private String someServiceId;


    @Before
    public void setUp() throws Exception {
        nacosDiscoveryService = new NacosDiscoveryService();
        nacosDiscoveryService.setNamingService(nacosNamingService);
        someServiceId = "someServiceId";
    }

    @Test
    public void testGetServiceInstancesWithEmptyInstances() throws Exception {
        assertTrue(nacosNamingService.selectInstances(someServiceId, true).isEmpty());
    }


    @Test
    public void testGetServiceInstancesWithInvalidServiceId() {
        assertTrue(nacosDiscoveryService.getServiceInstances(someServiceId).isEmpty());
    }

    @Test
    public void testGetServiceInstances() throws Exception {
        String someIp = "1.2.3.4";
        int somePort = 8080;
        String someInstanceId = "someInstanceId";
        Instance someServiceInstance = mockServiceInstance(someServiceId, someInstanceId,
                someIp,somePort);

        when(nacosNamingService.selectInstances(someServiceId,true)).thenReturn(
                Lists.newArrayList(someServiceInstance));

        List<ServiceDTO> serviceDTOList = nacosDiscoveryService.getServiceInstances(someServiceId);

        assertEquals(1, serviceDTOList.size());
        assertEquals(serviceDTOList.get(0).getHomepageUrl(),"http://1.2.3.4:8080/");

    }

    private Instance mockServiceInstance(String serviceId, String instanceId, String ip, int port) {
        Instance serviceInstance = mock(Instance.class);
        when(serviceInstance.getServiceName()).thenReturn(serviceId);
        when(serviceInstance.getInstanceId()).thenReturn(instanceId);
        when(serviceInstance.getIp()).thenReturn(ip);
        when(serviceInstance.getPort()).thenReturn(port);

        return serviceInstance;
    }

}
