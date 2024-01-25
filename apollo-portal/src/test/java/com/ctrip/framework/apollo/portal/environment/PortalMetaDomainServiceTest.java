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
package com.ctrip.framework.apollo.portal.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PortalMetaDomainServiceTest extends BaseIntegrationTest {

    private PortalMetaDomainService portalMetaDomainService;
    @Mock
    private PortalConfig portalConfig;

    @Before
    public void init() {
        final Map<String, String> map = new HashMap<>();
        map.put("nothing", "http://unknown.com");
        Mockito.when(portalConfig.getMetaServers()).thenReturn(map);

        portalMetaDomainService = new PortalMetaDomainService(portalConfig);
    }

    @Test
    public void testGetMetaDomain() {
        // local
        String localMetaServerAddress = "http://localhost:8080";
        mockMetaServerAddress(Env.LOCAL, localMetaServerAddress);
        assertEquals(localMetaServerAddress, portalMetaDomainService.getDomain(Env.LOCAL));

        // add this environment without meta server address
        String randomEnvironment = "randomEnvironment";
        Env.addEnvironment(randomEnvironment);
        assertEquals(PortalMetaDomainService.DEFAULT_META_URL, portalMetaDomainService.getDomain(Env.valueOf(randomEnvironment)));
    }

    @Test
    public void testGetValidAddress() throws Exception {
        String someResponse = "some response";
        startServerWithHandlers(mockServerHandler(HttpServletResponse.SC_OK, someResponse));

        String validServer = " http://localhost:" + PORT + " ";
        String invalidServer = "http://localhost:" + findFreePort();

        mockMetaServerAddress(Env.FAT, validServer + "," + invalidServer);
        mockMetaServerAddress(Env.UAT, invalidServer + "," + validServer);
        portalMetaDomainService.reload();

        assertEquals(validServer.trim(), portalMetaDomainService.getDomain(Env.FAT));
        assertEquals(validServer.trim(), portalMetaDomainService.getDomain(Env.UAT));
    }

    @Test
    public void testInvalidAddress() {
        String invalidServer = "http://localhost:" + findFreePort() + " ";
        String anotherInvalidServer = "http://localhost:" + findFreePort() + " ";

        mockMetaServerAddress(Env.LPT, invalidServer + "," + anotherInvalidServer);

        portalMetaDomainService.reload();

        String metaServer = portalMetaDomainService.getDomain(Env.LPT);

        assertTrue(metaServer.equals(invalidServer.trim()) || metaServer.equals(anotherInvalidServer.trim()));
    }

    private void mockMetaServerAddress(Env env, String metaServerAddress) {
        // add it to system's property
        System.setProperty(env.getName() + "_meta", metaServerAddress);
    }

}