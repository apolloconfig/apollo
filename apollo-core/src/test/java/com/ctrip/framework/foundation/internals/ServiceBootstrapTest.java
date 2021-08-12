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
package com.ctrip.framework.foundation.internals;

import com.ctrip.framework.apollo.core.spi.Ordered;
import org.junit.Test;

import java.util.ServiceConfigurationError;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ServiceBootstrapTest {
  @Test
  public void loadFirstSuccessfully() throws Exception {
    Interface1 service = ServiceBootstrap.loadFirst(Interface1.class);
    assertTrue(service instanceof Interface1Impl);
  }

  @Test(expected = IllegalStateException.class)
  public void loadFirstWithNoServiceFileDefined() throws Exception {
    ServiceBootstrap.loadFirst(Interface2.class);
  }

  @Test(expected = IllegalStateException.class)
  public void loadFirstWithServiceFileButNoServiceImpl() throws Exception {
    ServiceBootstrap.loadFirst(Interface3.class);
  }

  @Test(expected = ServiceConfigurationError.class)
  public void loadFirstWithWrongServiceImpl() throws Exception {
    ServiceBootstrap.loadFirst(Interface4.class);
  }

  @Test(expected = ServiceConfigurationError.class)
  public void loadFirstWithServiceImplNotExists() throws Exception {
    ServiceBootstrap.loadFirst(Interface5.class);
  }

  @Test
  public void loadAllWithServiceFileButNoServiceImpl() {
    assertFalse(ServiceBootstrap.loadAll(Interface7.class).hasNext());
  }

  @Test
  public void loadPrimarySuccessfully() {
    Interface6 service = ServiceBootstrap.loadPrimary(Interface6.class);
    assertTrue(service instanceof Interface6Impl1);
  }

  @Test(expected = IllegalStateException.class)
  public void loadPrimaryWithServiceFileButNoServiceImpl() {
    ServiceBootstrap.loadPrimary(Interface7.class);
  }

  @Test
  public void loadAllOrderedWithServiceFileButNoServiceImpl() {
    assertTrue(ServiceBootstrap.loadAllOrdered(Interface7.class).isEmpty());
  }

  interface Interface1 {
  }

  public static class Interface1Impl implements Interface1 {
  }

  interface Interface2 {
  }

  interface Interface3 {
  }

  interface Interface4 {
  }

  interface Interface5 {
  }

  interface Interface6 extends Ordered {
  }

  public static class Interface6Impl1 implements Interface6 {
    @Override
    public int getOrder() {
      return 1;
    }
  }

  public static class Interface6Impl2 implements Interface6 {
    @Override
    public int getOrder() {
      return 2;
    }
  }

  interface Interface7 extends Ordered {
  }
}
