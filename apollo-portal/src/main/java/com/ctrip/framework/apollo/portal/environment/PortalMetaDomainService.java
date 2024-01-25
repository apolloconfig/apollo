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

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.core.utils.NetUtil;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Only use in apollo-portal
 * Provider an available meta server url.
 * If there is no available meta server url for the given environment,
 * the default meta server url will be used(http://apollo.meta).
 * @see com.ctrip.framework.apollo.core.MetaDomainConsts
 * @author wxq
 */
@Service
public class PortalMetaDomainService {

    private static final Logger logger = LoggerFactory.getLogger(PortalMetaDomainService.class);
    private static final long REFRESH_INTERVAL_IN_SECOND = 60;// 1 min
    static final String DEFAULT_META_URL = "http://apollo.meta";

    private final Map<Env, String> metaServerAddressCache = Maps.newConcurrentMap();

    /**
     * initialize meta server provider without cache.
     * Multiple {@link PortalMetaServerProvider}
     */
    private final List<PortalMetaServerProvider> portalMetaServerProviders = new ArrayList<>();
    // env -> meta server address cache
    // comma separated meta server address -> selected single meta server address cache
    private final Map<String, String> selectedMetaServerAddressCache = Maps.newConcurrentMap();
    private final AtomicBoolean periodicRefreshStarted = new AtomicBoolean(false);

    PortalMetaDomainService(final PortalConfig portalConfig) {
        // high priority with data in database
        portalMetaServerProviders.add(new DatabasePortalMetaServerProvider(portalConfig));

        // System properties, OS environment, configuration file
        portalMetaServerProviders.add(new DefaultPortalMetaServerProvider());
    }

    /**
     * Return one meta server address. If multiple meta server addresses are configured, will select one.
     */
    public String getDomain(Env env) {
        String metaServerAddress = getMetaServerAddress(env);
        // if there is more than one address, need to select one
        if (metaServerAddress.contains(",")) {
            return selectMetaServerAddress(metaServerAddress);
        }
        return metaServerAddress;
    }

    /**
     * Return meta server address. If multiple meta server addresses are configured, will return the comma separated string.
     */
    public String getMetaServerAddress(Env env) {
        // in cache?
        if (!metaServerAddressCache.containsKey(env)) {
            // put it to cache
            metaServerAddressCache
                .put(env, getMetaServerAddressCacheValue(portalMetaServerProviders, env)
            );
        }

        // get from cache
        return metaServerAddressCache.get(env);
    }

    /**
     * Get the meta server from provider by given environment.
     * If there is no available meta server url for the given environment,
     * the default meta server url will be used(http://apollo.meta).
     * @param providers provide environment's meta server address
     * @param env environment
     * @return  meta server address
     */
    private String getMetaServerAddressCacheValue(
        Collection<PortalMetaServerProvider> providers, Env env) {

        // null value
        String metaAddress = null;

        for(PortalMetaServerProvider portalMetaServerProvider : providers) {
            if(portalMetaServerProvider.exists(env)) {
                metaAddress = portalMetaServerProvider.getMetaServerAddress(env);
                logger.info("Located meta server address [{}] for env [{}]", metaAddress, env);
                break;
            }
        }

        // check find it or not
        if (Strings.isNullOrEmpty(metaAddress)) {
            // Fallback to default meta address
            metaAddress = DEFAULT_META_URL;
            logger.warn(
                    "Meta server address fallback to [{}] for env [{}], because it is not available in MetaServerProvider",
                    metaAddress, env);
        }
        return metaAddress.trim();
    }

    /**
     * reload all {@link PortalMetaServerProvider}.
     * clear cache {@link this#metaServerAddressCache}
     */
    public void reload() {
        for(PortalMetaServerProvider portalMetaServerProvider : portalMetaServerProviders) {
            portalMetaServerProvider.reload();
        }
        metaServerAddressCache.clear();
    }

    /**
     * Select one available meta server from the comma separated meta server addresses, e.g.
     * http://1.2.3.4:8080,http://2.3.4.5:8080
     *
     * <br />
     *
     * In production environment, we still suggest using one single domain like http://config.xxx.com(backed by software
     * load balancers like nginx) instead of multiple ip addresses
     */
    private String selectMetaServerAddress(String metaServerAddresses) {
        String metaAddressSelected = selectedMetaServerAddressCache.get(metaServerAddresses);
        if (metaAddressSelected == null) {
            // initialize
            if (periodicRefreshStarted.compareAndSet(false, true)) {
                schedulePeriodicRefresh();
            }
            updateMetaServerAddresses(metaServerAddresses);
            metaAddressSelected = selectedMetaServerAddressCache.get(metaServerAddresses);
        }

        return metaAddressSelected;
    }

    private void updateMetaServerAddresses(String metaServerAddresses) {
        logger.debug("Selecting meta server address for: {}", metaServerAddresses);

        Transaction transaction = Tracer.newTransaction("Apollo.MetaService", "refreshMetaServerAddress");
        transaction.addData("Url", metaServerAddresses);

        try {
            List<String> metaServers = Lists.newArrayList(metaServerAddresses.split(","));
            // random load balancing
            Collections.shuffle(metaServers);

            boolean serverAvailable = false;

            for (String address : metaServers) {
                address = address.trim();
                //check whether /services/config is accessible
                if (NetUtil.pingUrl(address + "/services/config")) {
                    // select the first available meta server
                    selectedMetaServerAddressCache.put(metaServerAddresses, address);
                    serverAvailable = true;
                    logger.debug("Selected meta server address {} for {}", address, metaServerAddresses);
                    break;
                }
            }

            // we need to make sure the map is not empty, e.g. the first update might be failed
            if (!selectedMetaServerAddressCache.containsKey(metaServerAddresses)) {
                selectedMetaServerAddressCache.put(metaServerAddresses, metaServers.get(0).trim());
            }

            if (!serverAvailable) {
                logger.warn("Could not find available meta server for configured meta server addresses: {}, fallback to: {}",
                        metaServerAddresses, selectedMetaServerAddressCache.get(metaServerAddresses));
            }

            transaction.setStatus(Transaction.SUCCESS);
        } catch (Throwable ex) {
            transaction.setStatus(ex);
            throw ex;
        } finally {
            transaction.complete();
        }
    }

    private void schedulePeriodicRefresh() {
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("MetaServiceLocator", true));

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                for (String metaServerAddresses : selectedMetaServerAddressCache.keySet()) {
                    updateMetaServerAddresses(metaServerAddresses);
                }
            } catch (Throwable ex) {
                logger.warn(String.format("Refreshing meta server address failed, will retry in %d seconds",
                        REFRESH_INTERVAL_IN_SECOND), ex);
            }
        }, REFRESH_INTERVAL_IN_SECOND, REFRESH_INTERVAL_IN_SECOND, TimeUnit.SECONDS);
    }

}
