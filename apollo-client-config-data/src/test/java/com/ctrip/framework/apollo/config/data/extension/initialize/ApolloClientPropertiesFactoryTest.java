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
package com.ctrip.framework.apollo.config.data.extension.initialize;

import com.ctrip.framework.apollo.config.data.extension.enums.ApolloClientMessagingType;
import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientProperties;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientPropertiesFactoryTest {

  @Test
  public void testCreateApolloClientProperties() throws IOException {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("apollo.client.extension.enabled", "true");
    map.put("apollo.client.extension.messaging-type", "long_polling");
    MapConfigurationPropertySource propertySource = new MapConfigurationPropertySource(map);
    Binder binder = new Binder(propertySource);
    ApolloClientPropertiesFactory factory = new ApolloClientPropertiesFactory();
    ApolloClientProperties apolloClientProperties =
        factory.createApolloClientProperties(binder, null);

    Assert.assertEquals(apolloClientProperties.getExtension().getEnabled(), true);
    Assert.assertEquals(
        apolloClientProperties.getExtension().getMessagingType(),
        ApolloClientMessagingType.LONG_POLLING);
  }
}
