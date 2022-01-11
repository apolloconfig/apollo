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
package com.ctrip.framework.apollo.internals;

import static org.junit.Assert.*;

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.internals.AbstractConfig;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.mockito.Matchers;

import static org.mockito.Mockito.*;

/**
 * @author wxq
 */
public class InterestedConfigChangeEventTest {

  @Test
  public void TestInterestedChangedKeys()
      throws ExecutionException, InterruptedException, TimeoutException {

    final String namespace = "app";
    final String keyPrefix = "key-abc.";
    final String key = keyPrefix + UUID.randomUUID();
    final String anotherKey = UUID.randomUUID().toString();

    final SettableFuture<ConfigChangeEvent> onChangeFuture = SettableFuture.create();
    ConfigChangeListener configChangeListener = spy(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        assertEquals(namespace, changeEvent.getNamespace());
        assertEquals(2, changeEvent.changedKeys().size());
        assertTrue(changeEvent.changedKeys().containsAll(Sets.newHashSet(key, anotherKey)));
        assertEquals(1, changeEvent.interestedChangedKeys().size());
        assertTrue(changeEvent.interestedChangedKeys().contains(key));
        onChangeFuture.set(changeEvent);
      }
    });

    UnsupportedOperationConfig config = new UnsupportedOperationConfig();
    config.addChangeListener(configChangeListener, Collections.singleton("key-nothing"), Collections.singleton(keyPrefix));


    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(key, new ConfigChange(namespace, key, "123", "456", PropertyChangeType.MODIFIED));
    changes.put(anotherKey,
        new ConfigChange(namespace, anotherKey, null, "someValue", PropertyChangeType.ADDED));
    config.fireConfigChange(namespace, changes);

    onChangeFuture.get(500, TimeUnit.MILLISECONDS);

    verify(configChangeListener, atLeastOnce()).onChange(Matchers.<ConfigChangeEvent>any());
  }

  /**
   * @author wxq
   */
  private static class UnsupportedOperationConfig extends AbstractConfig {

    @Override
    public String getProperty(String key, String defaultValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPropertyNames() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ConfigSourceType getSourceType() {
      throw new UnsupportedOperationException();
    }
  }
}