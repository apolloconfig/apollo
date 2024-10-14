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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AdminServiceAddressLocator {

  private static final int RETRY_TIMES = 3;
  private static final String ADMIN_SERVICE_URL_PATH = "/services/admin";
  private static final Logger logger = LoggerFactory.getLogger(AdminServiceAddressLocator.class);

  private ScheduledExecutorService refreshServiceAddressService;
  private RestTemplate restTemplate;
  private List<Env> allEnvs;
  private Map<Env, List<ServiceDTO>> cache = new ConcurrentHashMap<>();

  private final PortalSettings portalSettings;
  private final RestTemplateFactory restTemplateFactory;
  private final PortalMetaDomainService portalMetaDomainService;
  private final PortalConfig portalConfig;

  public AdminServiceAddressLocator(
      final HttpMessageConverters httpMessageConverters,
      final PortalSettings portalSettings,
      final RestTemplateFactory restTemplateFactory,
      final PortalMetaDomainService portalMetaDomainService,
      final PortalConfig portalConfig
  ) {
    this.portalSettings = portalSettings;
    this.restTemplateFactory = restTemplateFactory;
    this.portalMetaDomainService = portalMetaDomainService;
    this.portalConfig = portalConfig;
  }

  @PostConstruct
  public void init() {
    allEnvs = portalSettings.getAllEnvs();

    //init restTemplate
    restTemplate = restTemplateFactory.getObject();

    refreshServiceAddressService =
        Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("ServiceLocator", true));

    refreshServiceAddressService.schedule(new RefreshAdminServerAddressTask(), 1, TimeUnit.MILLISECONDS);
  }

  public List<ServiceDTO> getServiceList(Env env) {
    List<ServiceDTO> services = cache.get(env);
    if (CollectionUtils.isEmpty(services)) {
      return Collections.emptyList();
    }
    List<ServiceDTO> randomConfigServices = Lists.newArrayList(services);
    Collections.shuffle(randomConfigServices);
    return randomConfigServices;
  }

  //maintain admin server address
  private class RefreshAdminServerAddressTask implements Runnable {

    @Override
    public void run() {
      boolean refreshSuccess = true;
      //refresh fail if get any env address fail
      for (Env env : allEnvs) {
        boolean currentEnvRefreshResult = refreshServerAddressCache(env);
        refreshSuccess = refreshSuccess && currentEnvRefreshResult;
      }

      if (refreshSuccess) {
        refreshServiceAddressService
            .schedule(new RefreshAdminServerAddressTask(), portalConfig.refreshAdminServerAddressTaskNormalIntervalSecond(), TimeUnit.SECONDS);
      } else {
        refreshServiceAddressService
            .schedule(new RefreshAdminServerAddressTask(), portalConfig.refreshAdminServerAddressTaskOfflineIntervalSecond(), TimeUnit.SECONDS);
      }
    }
  }

  private boolean refreshServerAddressCache(Env env) {

    for (int i = 0; i < RETRY_TIMES; i++) {

      try {
        ServiceDTO[] services = getAdminServerAddress(env);
        if (services == null || services.length == 0) {
          continue;
        }
        cache.put(env, Arrays.asList(services));
        return true;
      } catch (Throwable e) {
        logger.error(String.format("Get admin server address from meta server failed. env: %s, meta server address:%s",
                                   env, portalMetaDomainService.getDomain(env)), e);
        Tracer
            .logError(String.format("Get admin server address from meta server failed. env: %s, meta server address:%s",
                                    env, portalMetaDomainService.getDomain(env)), e);
      }
    }
    return false;
  }

  private ServiceDTO[] getAdminServerAddress(Env env) {
    String domainName = portalMetaDomainService.getDomain(env);
    String url = domainName + ADMIN_SERVICE_URL_PATH;
    return restTemplate.getForObject(url, ServiceDTO[].class);
  }


}
