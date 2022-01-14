/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.internals.ConfigRepository;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class DefaultConfigFactoryFileCachePropertyTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCreateFileEnableConfigRepository() throws Exception {
        MockInjector.setInstance(ConfigUtil.class, new MockFileCacheEnableConfigUtil());
        DefaultConfigFactory defaultConfigFactory = new DefaultConfigFactory();
        ConfigRepository configRepository = defaultConfigFactory.createConfigRepository("namespace");
        Assertions.assertTrue(configRepository instanceof LocalFileConfigRepository);
    }

    @Test
    public void testCreateFileDisableConfigRepository() throws Exception {
        MockInjector.setInstance(ConfigUtil.class, new MockFileCacheDisableConfigUtil());
        DefaultConfigFactory defaultConfigFactory = new DefaultConfigFactory();
        ConfigRepository configRepository = defaultConfigFactory.createConfigRepository("namespace");
        Assertions.assertTrue(configRepository instanceof RemoteConfigRepository);
    }

    public static class MockFileCacheEnableConfigUtil extends ConfigUtil {
        @Override
        public boolean isPropertyFileCacheEnabled() {
            return true;
        }
    }

    public static class MockFileCacheDisableConfigUtil extends ConfigUtil {
        @Override
        public boolean isPropertyFileCacheEnabled() {
            return false;
        }
    }

}
