package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.google.common.base.Function;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface Config {
  /**
   * Return the property value with the given key, or {@code defaultValue} if the key doesn't exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value
   */
  String getProperty(String key, String defaultValue);

  /**
   * Return the integer property value with the given key, or {@code defaultValue} if the key
   * doesn't exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as integer
   */
  Integer getIntProperty(String key, Integer defaultValue);

  /**
   * Return the long property value with the given key, or {@code defaultValue} if the key doesn't
   * exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as long
   */
  Long getLongProperty(String key, Long defaultValue);

  /**
   * Return the short property value with the given key, or {@code defaultValue} if the key doesn't
   * exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as short
   */
  Short getShortProperty(String key, Short defaultValue);

  /**
   * Return the float property value with the given key, or {@code defaultValue} if the key doesn't
   * exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as float
   */
  Float getFloatProperty(String key, Float defaultValue);

  /**
   * Return the double property value with the given key, or {@code defaultValue} if the key doesn't
   * exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as double
   */
  Double getDoubleProperty(String key, Double defaultValue);

  /**
   * Return the byte property value with the given key, or {@code defaultValue} if the key doesn't
   * exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as byte
   */
  Byte getByteProperty(String key, Byte defaultValue);

  /**
   * Return the boolean property value with the given key, or {@code defaultValue} if the key
   * doesn't exist.
   *
   * @param key          the property name
   * @param defaultValue the default value when key is not found or any error occurred
   * @return the property value as boolean
   */
  Boolean getBooleanProperty(String key, Boolean defaultValue);

  /**
   * Return the array property value with the given key, or {@code defaultValue} if the key doesn't exist.
   *
   * @param key          the property name
   * @param delimiter    the delimiter regex
   * @param defaultValue the default value when key is not found or any error occurred
   */
  String[] getArrayProperty(String key, String delimiter, String[] defaultValue);

  /**
   * Return the Date property value with the given name, or {@code defaultValue} if the name doesn't exist.
   * Will try to parse the date with Locale.US and formats as follows: yyyy-MM-dd HH:mm:ss.SSS,
   * yyyy-MM-dd HH:mm:ss and yyyy-MM-dd
   *
   * @param key          the property name
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the property value
   */
  Date getDateProperty(String key, Date defaultValue);

  /**
   * Return the Date property value with the given name, or {@code defaultValue} if the name doesn't exist.
   * Will parse the date with the format specified and Locale.US
   *
   * @param key          the property name
   * @param format       the date format, see {@link java.text.SimpleDateFormat} for more
   *                     information
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the property value
   */
  Date getDateProperty(String key, String format, Date defaultValue);

  /**
   * Return the Date property value with the given name, or {@code defaultValue} if the name doesn't exist.
   *
   * @param key          the property name
   * @param format       the date format, see {@link java.text.SimpleDateFormat} for more
   *                     information
   * @param locale       the locale to use
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the property value
   */
  Date getDateProperty(String key, String format, Locale locale, Date defaultValue);

  /**
   * Return the Enum property value with the given key, or {@code defaultValue} if the key doesn't exist.
   *
   * @param key          the property name
   * @param enumType     the enum class
   * @param defaultValue the default value when key is not found or any error occurred
   * @param <T>          the enum
   * @return the property value
   */
  <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue);

  /**
   * Return the duration property value(in milliseconds) with the given name, or {@code
   * defaultValue} if the name doesn't exist. Please note the format should comply with the follow
   * example (case insensitive). Examples:
   * <pre>
   *    "123MS"          -- parses as "123 milliseconds"
   *    "20S"            -- parses as "20 seconds"
   *    "15M"            -- parses as "15 minutes" (where a minute is 60 seconds)
   *    "10H"            -- parses as "10 hours" (where an hour is 3600 seconds)
   *    "2D"             -- parses as "2 days" (where a day is 24 hours or 86400 seconds)
   *    "2D3H4M5S123MS"  -- parses as "2 days, 3 hours, 4 minutes, 5 seconds and 123 milliseconds"
   * </pre>
   *
   * @param key          the property name
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the parsed property value(in milliseconds)
   */
  long getDurationProperty(String key, long defaultValue);

  /**
   * Add change listener to this config instance, will be notified when any key is changed in this namespace.
   *
   * @param listener the config change listener
   */
  void addChangeListener(ConfigChangeListener listener);

  /**
   * Add change listener to this config instance, will only be notified when any of the interested keys is changed in this namespace.
   *
   * @param listener the config change listener
   * @param interestedKeys the keys interested by the listener
   *
   * @since 1.0.0
   */
  void addChangeListener(ConfigChangeListener listener, Set<String> interestedKeys);

  /**
   * Add change listener to this config instance, will only be notified when any of the interested keys is changed in this namespace.
   *
   * @param listener the config change listener
   * @param interestedKeys the keys that the listener is interested in
   * @param interestedKeyPrefixes the key prefixes that the listener is interested in,
   *                              e.g. "spring." means that {@code listener} is interested in keys that starts with "spring.", such as "spring.banner", "spring.jpa", etc.
   *                              and "application" means that {@code listener} is interested in keys that starts with "application", such as "applicationName", "application.port", etc.
   *                              For more details, see {@link com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener#interestedKeyPrefixes()}
   *                              and {@link java.lang.String#startsWith(String)}
   *
   * @since 1.3.0
   */
  void addChangeListener(ConfigChangeListener listener, Set<String> interestedKeys,
      Set<String> interestedKeyPrefixes);

  /**
   * Remove the change listener
   *
   * @param listener the specific config change listener to remove
   * @return true if the specific config change listener is found and removed
   *
   * @since 1.1.0
   */
  boolean removeChangeListener(ConfigChangeListener listener);

  /**
   * Return a set of the property names
   *
   * @return the property names
   */
  Set<String> getPropertyNames();

  /**
   * Return the user-defined property value with the given key, or {@code defaultValue} if the key doesn't exist.
   *
   * @param key          the property name
   * @param function     the transform {@link Function}. from String to user-defined type
   * @param defaultValue the default value when key is not found or any error occurred
   * @param <T>          user-defined type
   * @return the property value
   *
   * @since 1.1.0
   */
  <T> T getProperty(String key, Function<String, T> function, T defaultValue);

  /**
   * Return the config's source type, i.e. where is the config loaded from
   *
   * @return the config's source type
   *
   * @since 1.1.0
   */
  ConfigSourceType getSourceType();
}
