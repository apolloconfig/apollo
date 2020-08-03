package com.ctrip.framework.apollo.build;

import java.util.Map;

import com.ctrip.framework.apollo.internals.DefaultInjector;
import com.ctrip.framework.apollo.internals.Injector;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class MockInjector implements Injector {
  private static Map<Class, Object> classMap = Maps.newHashMap();
  private static Table<Class, String, Object> classTable = HashBasedTable.create();
  private static Injector delegate = new DefaultInjector();

  @Override
  public <T> T getInstance(Class<T> clazz) {
    T o = (T) classMap.get(clazz);
    if (o != null) {
      return o;
    }

    if (delegate != null) {
      return delegate.getInstance(clazz);
    }

    return null;
  }

  @Override
  public <T> T getInstance(Class<T> clazz, String name) {
    T o = (T) classTable.get(clazz, name);
    if (o != null) {
      return o;
    }

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
}
