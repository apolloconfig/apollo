/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.core;

import com.ctrip.framework.apollo.core.enums.Env;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import org.slf4j.Logger;

import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.core.utils.NetUtil;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * The meta domain will try to load the meta server address from MetaServerProviders, the default
 * ones are:
 *
 * <ul>
 * <li>com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider</li>
 * </ul>
 * <p>
 * If no provider could provide the meta server url, the default meta url will be used(http://apollo.meta).
 * <br />
 * <p>
 * 3rd party MetaServerProvider could be injected by typical Java Service Loader pattern.
 *
 * @see com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider
 */
public class MetaDomainConsts {

  public static final String DEFAULT_META_URL = "http://apollo.meta";

  // env -> meta server address cache
  private static final Map<Env, String> metaServerAddressCache = Maps.newConcurrentMap();
  private static volatile List<MetaServerProvider> metaServerProviders = null;

  private static final long REFRESH_INTERVAL_IN_SECOND = 60;// 1 min
  private static final Logger logger = DeferredLoggerFactory.getLogger(MetaDomainConsts.class);
  // comma separated meta server address -> selected single meta server address cache
  private static final Map<String, String> selectedMetaServerAddressCache = Maps.newConcurrentMap();
  private static final AtomicBoolean periodicRefreshStarted = new AtomicBoolean(false);

  private static final Object LOCK = new Object();

  /**
   * Return one meta server address. If multiple meta server addresses are configured, will select
   * one.
   */
  public static String getDomain(Env env) {
    String metaServerAddress = getMetaServerAddress(env);
    // if there is more than one address, need to select one
    if (metaServerAddress.contains(",")) {
      return selectMetaServerAddress(metaServerAddress);
    }
    return metaServerAddress;
  }

  /**
   * Return meta server address. If multiple meta server addresses are configured, will return the
   * comma separated string.
   */
  public static String getMetaServerAddress(Env env) {
    if (!metaServerAddressCache.containsKey(env)) {
      initMetaServerAddress(env);
    }

    return metaServerAddressCache.get(env);
  }

  private static void initMetaServerAddress(Env env) {
    if (metaServerProviders == null) {
      synchronized (LOCK) {
        if (metaServerProviders == null) {
          metaServerProviders = initMetaServerProviders();
        }
      }
    }

    String metaAddress = null;

    for (MetaServerProvider provider : metaServerProviders) {
      metaAddress = provider.getMetaServerAddress(env);
      if (!Strings.isNullOrEmpty(metaAddress)) {
        logger.info("Located meta server address {} for env {} from {}", metaAddress, env,
            provider.getClass().getName());
        break;
      }
    }

    if (Strings.isNullOrEmpty(metaAddress)) {
      // Fallback to default meta address
      metaAddress = DEFAULT_META_URL;
      logger.warn(
          "Meta server address fallback to {} for env {}, because it is not available in all MetaServerProviders",
          metaAddress, env);
    }

    metaServerAddressCache.put(env, metaAddress.trim());
  }

  private static List<MetaServerProvider> initMetaServerProviders() {
    Iterator<MetaServerProvider> metaServerProviderIterator = ServiceBootstrap
        .loadAll(MetaServerProvider.class);

    List<MetaServerProvider> metaServerProviders = Lists.newArrayList(metaServerProviderIterator);

    Collections.sort(metaServerProviders, new Comparator<MetaServerProvider>() {
      @Override
      public int compare(MetaServerProvider o1, MetaServerProvider o2) {
        // the smaller order has higher priority
        return Integer.compare(o1.getOrder(), o2.getOrder());
      }
    });

    return metaServerProviders;
  }

  /**
   * Select one available meta server from the comma separated meta server addresses, e.g.
   * http://1.2.3.4:8080,http://2.3.4.5:8080
   * <p>
   * <br />
   * <p>
   * In production environment, we still suggest using one single domain like
   * http://config.xxx.com(backed by software load balancers like nginx) instead of multiple ip
   * addresses
   */
  private static String selectMetaServerAddress(String metaServerAddresses) {
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

  private static void updateMetaServerAddresses(String metaServerAddresses) {
    logger.debug("Selecting meta server address for: {}", metaServerAddresses);

    Transaction transaction = Tracer
        .newTransaction("Apollo.MetaService", "refreshMetaServerAddress");
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
        logger.warn(
            "Could not find available meta server for configured meta server addresses: {}, fallback to: {}",
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

  private static void schedulePeriodicRefresh() {
    ScheduledExecutorService scheduledExecutorService =
        Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("MetaServiceLocator", true));

    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          for (String metaServerAddresses : selectedMetaServerAddressCache.keySet()) {
            updateMetaServerAddresses(metaServerAddresses);
          }
        } catch (Throwable ex) {
          logger
              .warn(String.format("Refreshing meta server address failed, will retry in %d seconds",
                  REFRESH_INTERVAL_IN_SECOND), ex);
        }
      }
    }, REFRESH_INTERVAL_IN_SECOND, REFRESH_INTERVAL_IN_SECOND, TimeUnit.SECONDS);
  }
}
