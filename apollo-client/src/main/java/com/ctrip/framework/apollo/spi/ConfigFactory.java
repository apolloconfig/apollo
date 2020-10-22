package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigFactory {
  /**
   * Create the config instance for the namespace.
   *
   * @param namespace the namespace
   * @return the newly created config instance
   */
  Config create(String namespace);

  /**
   * Create the config file instance for the namespace
   * @param namespace the namespace
   * @return the newly created config file instance
   */
  ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
