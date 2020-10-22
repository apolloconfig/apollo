package com.ctrip.framework.apollo.spi;

/**
 * The manually config registry, use with caution!
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigRegistry {
  /**
   * Register the config factory for the namespace specified.
   *
   * @param namespace the namespace
   * @param factory   the factory for this namespace
   */
  void register(String namespace, ConfigFactory factory);

  /**
   * Get the registered config factory for the namespace.
   *
   * @param namespace the namespace
   * @return the factory registered for this namespace
   */
  ConfigFactory getFactory(String namespace);
}
