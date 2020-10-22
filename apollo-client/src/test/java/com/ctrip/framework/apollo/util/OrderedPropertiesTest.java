package com.ctrip.framework.apollo.util;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class OrderedPropertiesTest {

  private OrderedProperties orderedProperties;

  @Before
  public void setUp() {
    orderedProperties = new OrderedProperties();
    orderedProperties.setProperty("key1", "value1");
    orderedProperties.setProperty("key2", "value2");
  }

  @Test
  public void testOrderedPropertiesInvokedAsLegacyProperties() {
    Properties legacyProperties = orderedProperties;
    assertEquals(orderedProperties.size(), legacyProperties.size());

    legacyProperties.put("key3", "value3");
    assertEquals(orderedProperties.size(), legacyProperties.size());

    assertEquals(orderedProperties.getProperty("key3"), legacyProperties.getProperty("key3"));
    assertEquals(orderedProperties.get("key3"), legacyProperties.get("key3"));

    assertEquals(orderedProperties.containsKey("key2"), legacyProperties.containsKey("key2"));
    assertEquals(orderedProperties.containsValue("key2"), legacyProperties.containsValue("key2"));
    assertEquals(orderedProperties.containsValue("value2"),
        legacyProperties.containsValue("value2"));

    assertEquals(orderedProperties.entrySet(), legacyProperties.entrySet());
    assertEquals(orderedProperties.keySet(), legacyProperties.keySet());

  }

  @Test
  public void testClear() {
    orderedProperties.clear();
    assertEquals(0, orderedProperties.size());
    assertTrue(orderedProperties.isEmpty());
  }

  @Test
  public void testClone() {
    OrderedProperties clone = (OrderedProperties) orderedProperties.clone();

    assertNotSame(clone, orderedProperties);
    assertEquals(orderedProperties, clone);
  }

  @Test
  public void testRemove() {
    Object value1 = orderedProperties.remove("key1");
    assertEquals("value1", value1);

    value1 = orderedProperties.remove("key1");
    assertNull(value1);

    assertNull(orderedProperties.get("key1"));
    assertFalse(orderedProperties.containsKey("key1"));
  }

  @Test
  public void testValues() {
    Collection<Object> values = orderedProperties.values();
    assertEquals(2, values.size());
    assertTrue(values.contains("value1"));
    assertTrue(values.contains("value2"));
  }


  @Test(expected = NullPointerException.class)
  public void testPutNull() {
    orderedProperties.put("key3", null);
  }

  @Test
  public void testPropertyNames() {
    Enumeration<?> propertyNames = orderedProperties.propertyNames();
    assertEquals("key1", propertyNames.nextElement());
    assertEquals("key2", propertyNames.nextElement());
  }


  @Test
  public void testKeys() {
    Enumeration<Object> keys = orderedProperties.keys();
    assertTrue(keys.hasMoreElements());
    assertEquals("key1", keys.nextElement());
    assertTrue(keys.hasMoreElements());
    assertEquals("key2", keys.nextElement());

  }

}
