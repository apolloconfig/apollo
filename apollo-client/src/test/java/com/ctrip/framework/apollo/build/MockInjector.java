package com.ctrip.framework.apollo.build;

import com.ctrip.framework.apollo.internals.DefaultInjector;
import com.ctrip.framework.apollo.internals.Injector;
import com.ctrip.framework.apollo.spi.ApolloInjectorCustomizer;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class MockInjector implements Injector {

  private static Map<Class, Object> classMap = Maps.newHashMap();
  private static Table<Class, String, Object> classTable = HashBasedTable.create();
  private static Injector delegate = new DefaultInjector();

  @Override
  public <T> T getInstance(Class<T> clazz) {
    if (delegate != null) {
      return delegate.getInstance(clazz);
    }

    return null;
  }

  @Override
  public <T> T getInstance(Class<T> clazz, String name) {
    if (delegate != null) {
      return delegate.getInstance(clazz, name);
    }

    return null;
  }

  public static void setInstance(Class clazz, Object o) {
    classMap.put(clazz, o);
  }

  public static void setInstance(Class clazz, String name, Object o) {
    classTable.put(clazz, name, o);
  }

  public static void setDelegate(Injector delegateInjector) {
    delegate = delegateInjector;
  }

  public static void reset() {
    classMap.clear();
    classTable.clear();
    delegate = new DefaultInjector();
  }

  public static class InjectCustomizer implements ApolloInjectorCustomizer {

    @Override
    public <T> T getInstance(Class<T> clazz) {
      return (T) classMap.get(clazz);
    }

    @Override
    public <T> T getInstance(Class<T> clazz, String name) {
      return (T) classTable.get(clazz, name);
    }

    @Override
    public int getOrder() {
      return 0;
    }
  }
}
