package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigManager {
  /**
   * Get the config instance for the namespace specified.
   * @param namespace the namespace
   * @return the config instance for the namespace
   */
  Config getConfig(String namespace);

  /**
   * Get the config file instance for the namespace specified.
   * @param namespace the namespace
   * @param configFileFormat the config file format
   * @return the config file instance for the namespace
   */
  ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
