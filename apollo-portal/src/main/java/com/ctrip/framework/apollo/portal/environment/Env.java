/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.portal.environment;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides functionalities to manage and hold all environments of the portal. By default
 * all the Env from {@link com.ctrip.framework.apollo.core.enums.Env} are included.
 *
 * @author wxq
 * @author Diego Krupitza(info@diegokrupitza.com)
 */
public class Env {

  private static final Logger logger = LoggerFactory.getLogger(Env.class);
  // use to cache Env
  private static final Map<String, Env> STRING_ENV_MAP = new ConcurrentHashMap<>();
  // default environments
  public static final Env LOCAL = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.LOCAL.name());
  public static final Env DEV = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.DEV.name());
  public static final Env FAT = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.FAT.name());
  public static final Env FWS = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.FWS.name());
  public static final Env UAT = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.UAT.name());
  public static final Env LPT = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.LPT.name());
  public static final Env PRO = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.PRO.name());
  public static final Env TOOLS = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.TOOLS.name());
  public static final Env UNKNOWN = addEnvironment(
      com.ctrip.framework.apollo.core.enums.Env.UNKNOWN.name());
  // name of environment, cannot be null
  private final String name;

  /**
   * Cannot create by other
   *
   * @param name
   */
  private Env(String name) {
    this.name = name;
  }

  /**
   * add some change to environment name trim and to upper
   *
   * @param envName
   * @return
   */
  private static String getWellFormName(String envName) {
    if (StringUtils.isBlank(envName)) {
      return "";
    }

    String envWellFormName = envName.trim().toUpperCase();

    // special case for production in case of typo
    if ("PROD".equals(envWellFormName)) {
      return Env.PRO.name;
    }

    // special case that FAT & FWS should map to FAT
    if ("FWS".equals(envWellFormName)) {
      return Env.FAT.name;
    }

    return envWellFormName;
  }

  /**
   * logic same as {@link com.ctrip.framework.apollo.core.enums.EnvUtils#transformEnv}
   *
   * @param envName the name we want to transform
   * @return the env object matching the <code>envName</code>
   */
  public static Env transformEnv(String envName) {
    final String envWellFormName = getWellFormName(envName);

    if (Env.exists(envWellFormName)) {
      return Env.valueOf(envWellFormName);
    }
    // cannot be found or blank name
    return Env.UNKNOWN;
  }

  /**
   * a environment name exist or not
   *
   * @param name the name we want to check if it exists
   * @return does the env name exists or not
   */
  public static boolean exists(String name) {
    name = getWellFormName(name);
    return STRING_ENV_MAP.containsKey(name);
  }

  /**
   * add an environment
   *
   * @param name the name of the environment to add
   * @return the newly created environment
   */
  public static Env addEnvironment(String name) {
    if (StringUtils.isBlank(name)) {
      throw new RuntimeException("Cannot add a blank environment: " + "[" + name + "]");
    }

    name = getWellFormName(name);
    if (STRING_ENV_MAP.containsKey(name)) {
      // has been existed
      logger.debug("{} already exists.", name);
    } else {
      // not existed
      STRING_ENV_MAP.put(name, new Env(name));
    }
    return STRING_ENV_MAP.get(name);
  }

  /**
   * replace valueOf in enum But what would happened if environment not exist?
   *
   * @param name
   * @return
   * @throws IllegalArgumentException if this existed environment has no Env with the specified
   *                                  name
   */
  public static Env valueOf(String name) {
    name = getWellFormName(name);
    if (exists(name)) {
      return STRING_ENV_MAP.get(name);
    } else {
      throw new IllegalArgumentException(name + " not exist");
    }
  }

  /**
   * Please use {@code Env.valueOf} instead this method
   *
   * @param env
   * @return
   */
  @Deprecated
  public static Env fromString(String env) {
    Env environment = transformEnv(env);
    Preconditions.checkArgument(environment != UNKNOWN, String.format("Env %s is invalid", env));
    return environment;
  }

  /**
   * conversion key from {@link String} to {@link Env}
   *
   * @param metaServerAddresses key is environment, value is environment's meta server address
   * @return relationship between {@link Env} and meta server address
   */
  static Map<Env, String> transformToEnvMap(Map<String, String> metaServerAddresses) {
    // add to domain
    Map<Env, String> map = new ConcurrentHashMap<>();
    for (Map.Entry<String, String> entry : metaServerAddresses.entrySet()) {
      // add new environment
      Env env = Env.addEnvironment(entry.getKey());
      // get meta server address value
      String value = entry.getValue();
      // put pair (Env, meta server address)
      map.put(env, value);
    }
    return map;
  }

  /**
   * Not just name in Env, the address of Env must be same, or it will throw {@code
   * RuntimeException}
   *
   * @param o
   * @return
   * @throws RuntimeException When same name but different address
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Env env = (Env) o;
    if (getName().equals(env.getName())) {
      throw new RuntimeException(getName() + " is same environment name, but their Env not same");
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName());
  }

  /**
   * a Env convert to string, ie its name.
   *
   * @return
   */
  @Override
  public String toString() {
    return name;
  }

  public String getName() {
    return name;
  }
}
