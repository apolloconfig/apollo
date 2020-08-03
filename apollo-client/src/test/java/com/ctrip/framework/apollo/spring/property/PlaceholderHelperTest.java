package com.ctrip.framework.apollo.spring.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class PlaceholderHelperTest {

  private PlaceholderHelper placeholderHelper;

  @Before
  public void setUp() throws Exception {
    placeholderHelper = new PlaceholderHelper();
  }

  @Test
  public void testExtractPlaceholderKeys() throws Exception {
    check("${some.key}", "some.key");
    check("${some.key:100}", "some.key");
    check("${some.key:${some.other.key}}", "some.key", "some.other.key");
    check("${some.key:${some.other.key:100}}", "some.key", "some.other.key");

    check("${some.key}/xx", "some.key");
    check("${some.key:100}/xx", "some.key");
    check("${some.key:${some.other.key}/xx}/yy", "some.key", "some.other.key");
    check("${some.key:${some.other.key:100}/xx}/yy", "some.key", "some.other.key");
  }

  @Test
  public void testExtractNestedPlaceholderKeys() throws Exception {
    check("${${some.key}}", "some.key");
    check("${${some.key:other.key}}", "some.key");
    check("${${some.key}:100}", "some.key");
    check("${${some.key}:${another.key}}", "some.key", "another.key");

    check("${${some.key}/xx}/xx", "some.key");
    check("${${some.key:other.key/xx}/xx}/xx", "some.key");
    check("${${some.key}/xx:100}/xx", "some.key");
    check("${${some.key}/xx:${another.key}/xx}xx", "some.key", "another.key");

  }

  @Test
  public void testExtractComplexNestedPlaceholderKeys() throws Exception {
    check("${${a}1${b}:3.${c:${d:100}}}", "a", "b", "c", "d");
    check("${1${a}2${b}3:4.${c:5${d:100}6}7}", "a", "b", "c", "d");

    check("${${a}1${b}:3.${c:${d:100}}}/xx", "a", "b", "c", "d");
    check("${1${a}2${b}3:4.${c:5${d:100}6}7}/xx", "a", "b", "c", "d");
  }

  @Test
  public void testExtractPlaceholderKeysFromExpression() throws Exception {
    check("#{new java.text.SimpleDateFormat('${some.key}').parse('${another.key}')}", "some.key", "another.key");
    check("#{new java.text.SimpleDateFormat('${some.key:abc}').parse('${another.key:100}')}", "some.key", "another.key");
    check("#{new java.text.SimpleDateFormat('${some.key:${some.other.key}}').parse('${another.key}')}", "some.key", "another.key", "some.other.key");
    check("#{new java.text.SimpleDateFormat('${some.key:${some.other.key:abc}}').parse('${another.key}')}", "some.key", "another.key", "some.other.key");
    check("#{new java.text.SimpleDateFormat('${${some.key}}').parse('${${another.key:other.key}}')}", "some.key", "another.key");

    check("#{new java.text.SimpleDateFormat('${some.key}/xx').parse('${another.key}/xx')}", "some.key", "another.key");
    check("#{new java.text.SimpleDateFormat('${some.key:abc}/xx').parse('${another.key:100}/xx')}", "some.key", "another.key");
    check("#{new java.text.SimpleDateFormat('${some.key:${some.other.key}/xx}/xx').parse('${another.key}/xx')}", "some.key", "another.key", "some.other.key");
    check("#{new java.text.SimpleDateFormat('${some.key:${some.other.key:abc}/xx}/xx').parse('${another.key}/xx')}", "some.key", "another.key", "some.other.key");
    check("#{new java.text.SimpleDateFormat('${${some.key}/xx}').parse('${${another.key:other.key}/xx}/xx')}", "some.key", "another.key");


    assertTrue(placeholderHelper.extractPlaceholderKeys("#{systemProperties[some.key] ?: 123}").isEmpty());
    assertTrue(placeholderHelper.extractPlaceholderKeys("#{ T(java.lang.Math).random() * 100.0 }").isEmpty());
  }

  @Test
  public void testExtractInvalidPlaceholderKeys() throws Exception {
    assertTrue(placeholderHelper.extractPlaceholderKeys("some.key").isEmpty());
    assertTrue(placeholderHelper.extractPlaceholderKeys("some.key:100").isEmpty());
  }

  private void check(String propertyString, String... expectedPlaceholders) {
    assertEquals(Sets.newHashSet(expectedPlaceholders), placeholderHelper.extractPlaceholderKeys(propertyString));
  }
}
