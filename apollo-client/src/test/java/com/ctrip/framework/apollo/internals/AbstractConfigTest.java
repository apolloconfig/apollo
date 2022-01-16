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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.mockito.Matchers;

/**
 * @author wxq
 */
public class AbstractConfigTest {

  /**
   * @see AbstractConfig#fireConfigChange(ConfigChangeEvent)
   */
  @Test
  public void testFireConfigChange_cannot_notify() throws InterruptedException {
    AbstractConfig abstractConfig = spy(new ErrorConfig());
    final String namespace = "app-namespace-0";

    ConfigChangeListener configChangeListener =
        spy(
            new ConfigChangeListener() {
              @Override
              public void onChange(ConfigChangeEvent changeEvent) {}
            });
    abstractConfig.addChangeListener(
        configChangeListener, Collections.singleton("cannot-be-match-key"));

    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(
        "key1", new ConfigChange(namespace, "key1", null, "new-value", PropertyChangeType.ADDED));
    ConfigChangeEvent configChangeEvent = new ConfigChangeEvent(namespace, changes);

    abstractConfig.fireConfigChange(configChangeEvent);
    abstractConfig.fireConfigChange(namespace, changes);

    // wait a minute for invoking
    Thread.sleep(100);

    verify(configChangeListener, times(0)).onChange(Matchers.<ConfigChangeEvent>any());
  }

  @Test
  public void testFireConfigChange_event_notify_once()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-1";
    final String key = "great-key";

    final SettableFuture<ConfigChangeEvent> future1 = SettableFuture.create();
    final SettableFuture<ConfigChangeEvent> future2 = SettableFuture.create();

    final AtomicInteger invokeCount = new AtomicInteger();

    final ConfigChangeListener configChangeListener1 =
        spy(
            new ConfigChangeListener() {
              @Override
              public void onChange(ConfigChangeEvent changeEvent) {
                invokeCount.incrementAndGet();
                future1.set(changeEvent);
              }
            });
    final ConfigChangeListener configChangeListener2 =
        spy(
            new ConfigChangeListener() {
              @Override
              public void onChange(ConfigChangeEvent changeEvent) {
                invokeCount.incrementAndGet();
                future2.set(changeEvent);
              }
            });
    abstractConfig.addChangeListener(configChangeListener1, Collections.singleton(key));
    abstractConfig.addChangeListener(configChangeListener2, Collections.singleton(key));

    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(
        key,
        new ConfigChange(namespace, key, "old-value", "new-value", PropertyChangeType.MODIFIED));
    ConfigChangeEvent configChangeEvent = new ConfigChangeEvent(namespace, changes);

    abstractConfig.fireConfigChange(configChangeEvent);

    assertEquals(configChangeEvent, future1.get(500, TimeUnit.MILLISECONDS));
    assertEquals(configChangeEvent, future2.get(500, TimeUnit.MILLISECONDS));

    assertEquals(2, invokeCount.get());

    verify(configChangeListener1, times(1)).onChange(Matchers.eq(configChangeEvent));
    verify(configChangeListener2, times(1)).onChange(Matchers.eq(configChangeEvent));
  }

  @Test
  public void testFireConfigChange_changes_notify_once()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-1";
    final String key = "great-key";

    final SettableFuture<ConfigChangeEvent> future1 = SettableFuture.create();
    final SettableFuture<ConfigChangeEvent> future2 = SettableFuture.create();

    final AtomicInteger invokeCount = new AtomicInteger();

    final ConfigChangeListener configChangeListener1 =
        spy(
            new ConfigChangeListener() {
              @Override
              public void onChange(ConfigChangeEvent changeEvent) {
                invokeCount.incrementAndGet();
                future1.set(changeEvent);
              }
            });
    final ConfigChangeListener configChangeListener2 =
        spy(
            new ConfigChangeListener() {
              @Override
              public void onChange(ConfigChangeEvent changeEvent) {
                invokeCount.incrementAndGet();
                future2.set(changeEvent);
              }
            });
    abstractConfig.addChangeListener(configChangeListener1, Collections.singleton(key));
    abstractConfig.addChangeListener(configChangeListener2, Collections.singleton(key));

    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(
        key,
        new ConfigChange(namespace, key, "old-value", "new-value", PropertyChangeType.MODIFIED));

    abstractConfig.fireConfigChange(namespace, changes);

    assertEquals(Collections.singleton(key), future1.get(500, TimeUnit.MILLISECONDS).changedKeys());
    assertEquals(Collections.singleton(key), future2.get(500, TimeUnit.MILLISECONDS).changedKeys());

    assertEquals(2, invokeCount.get());

    verify(configChangeListener1, times(1)).onChange(Matchers.<ConfigChangeEvent>any());
    verify(configChangeListener2, times(1)).onChange(Matchers.<ConfigChangeEvent>any());
  }

  /**
   * Only for current test usage.
   *
   * @author wxq
   */
  private static class ErrorConfig extends AbstractConfig {

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
