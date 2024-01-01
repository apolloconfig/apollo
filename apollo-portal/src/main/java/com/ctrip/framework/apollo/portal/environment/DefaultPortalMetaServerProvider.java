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

import static com.ctrip.framework.apollo.portal.environment.Env.transformToEnvMap;

import com.ctrip.framework.apollo.core.utils.ResourceUtils;
import com.ctrip.framework.apollo.portal.util.KeyValueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Only use in apollo-portal
 * load all meta server address from
 *  - System Property           [key ends with "_meta" (case insensitive)]
 *  - OS environment variable   [key ends with "_meta" (case insensitive)]
 *  - user's configuration file [key ends with ".meta" (case insensitive)]
 * when apollo-portal start up.
 * @see com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider
 * @author wxq
 */
class DefaultPortalMetaServerProvider implements PortalMetaServerProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPortalMetaServerProvider.class);

    /**
     * environments and their meta server address
     * properties file path
     */
    private static final String APOLLO_ENV_PROPERTIES_FILE_PATH = "apollo-env.properties";

    private volatile Map<Env, String> domains;

    DefaultPortalMetaServerProvider() {
      reload();
    }

    @Override
    public String getMetaServerAddress(Env targetEnv) {
        String metaServerAddress = domains.get(targetEnv);
        return metaServerAddress == null ? null : metaServerAddress.trim();
    }

    @Override
    public boolean exists(Env targetEnv) {
        return domains.containsKey(targetEnv);
    }

    @Override
    public void reload() {
        domains = initializeDomains();
        logger.info("Loaded meta server addresses from system property, os environment and properties file: {}", domains);
    }

    /**
     * load all environment's meta address dynamically when this class loaded by JVM
     */
    private Map<Env, String> initializeDomains() {

        // add to domain
        Map<Env, String> map = new ConcurrentHashMap<>();
        // lower priority add first
        map.putAll(getDomainsFromPropertiesFile());
        map.putAll(getDomainsFromOSEnvironment());
        map.putAll(getDomainsFromSystemProperty());

        // log all
        return map;
    }

    private Map<Env, String> getDomainsFromSystemProperty() {
        // find key-value from System Property which key ends with "_meta" (case insensitive)
        Map<String, String> metaServerAddressesFromSystemProperty = KeyValueUtils.filterWithKeyIgnoreCaseEndsWith(System.getProperties(), "_meta");
        // remove key's suffix "_meta" (case insensitive)
        metaServerAddressesFromSystemProperty = KeyValueUtils.removeKeySuffix(metaServerAddressesFromSystemProperty, "_meta".length());
        return transformToEnvMap(metaServerAddressesFromSystemProperty);
    }

    private Map<Env, String> getDomainsFromOSEnvironment() {
        // find key-value from OS environment variable which key ends with "_meta" (case insensitive)
        Map<String, String> metaServerAddressesFromOSEnvironment = KeyValueUtils.filterWithKeyIgnoreCaseEndsWith(System.getenv(), "_meta");
        // remove key's suffix "_meta" (case insensitive)
        metaServerAddressesFromOSEnvironment = KeyValueUtils.removeKeySuffix(metaServerAddressesFromOSEnvironment, "_meta".length());
        return transformToEnvMap(metaServerAddressesFromOSEnvironment);
    }

    private Map<Env, String> getDomainsFromPropertiesFile() {
        // find key-value from properties file which key ends with ".meta" (case insensitive)
        Properties properties = new Properties();
        properties = ResourceUtils.readConfigFile(APOLLO_ENV_PROPERTIES_FILE_PATH, properties);
        Map<String, String> metaServerAddressesFromPropertiesFile = KeyValueUtils.filterWithKeyIgnoreCaseEndsWith(properties, ".meta");
        // remove key's suffix ".meta" (case insensitive)
        metaServerAddressesFromPropertiesFile = KeyValueUtils.removeKeySuffix(metaServerAddressesFromPropertiesFile, ".meta".length());
        return transformToEnvMap(metaServerAddressesFromPropertiesFile);
    }

}
