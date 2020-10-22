package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.TracerEventType;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

/**
 * 封装RestTemplate. admin server集群在某些机器宕机或者超时的情况下轮询重试
 */
@Component
public class RetryableRestTemplate {

  private Logger logger = LoggerFactory.getLogger(RetryableRestTemplate.class);

  private UriTemplateHandler uriTemplateHandler = new DefaultUriBuilderFactory();

  private static final Gson GSON = new Gson();
  /**
   * Admin service access tokens in "PortalDB.ServerConfig"
   */
  private static final Type ACCESS_TOKENS = new TypeToken<Map<String, String>>(){}.getType();

  private RestTemplate restTemplate;

  private final RestTemplateFactory restTemplateFactory;
  private final AdminServiceAddressLocator adminServiceAddressLocator;
  private final PortalMetaDomainService portalMetaDomainService;
  private final PortalConfig portalConfig;
  private volatile String lastAdminServiceAccessTokens;
  private volatile Map<Env, String> adminServiceAccessTokenMap;

  public RetryableRestTemplate(
      final @Lazy RestTemplateFactory restTemplateFactory,
      final @Lazy AdminServiceAddressLocator adminServiceAddressLocator,
      final PortalMetaDomainService portalMetaDomainService,
      final PortalConfig portalConfig
  ) {
    this.restTemplateFactory = restTemplateFactory;
    this.adminServiceAddressLocator = adminServiceAddressLocator;
    this.portalMetaDomainService = portalMetaDomainService;
    this.portalConfig = portalConfig;
  }


  @PostConstruct
  private void postConstruct() {
    restTemplate = restTemplateFactory.getObject();
  }

  public <T> T get(Env env, String path, Class<T> responseType, Object... urlVariables)
      throws RestClientException {
    return execute(HttpMethod.GET, env, path, null, responseType, urlVariables);
  }

  public <T> ResponseEntity<T> get(Env env, String path, ParameterizedTypeReference<T> reference,
                                   Object... uriVariables)
      throws RestClientException {

    return exchangeGet(env, path, reference, uriVariables);
  }

  public <T> T post(Env env, String path, Object request, Class<T> responseType, Object... uriVariables)
      throws RestClientException {
    return execute(HttpMethod.POST, env, path, request, responseType, uriVariables);
  }

  public void put(Env env, String path, Object request, Object... urlVariables) throws RestClientException {
    execute(HttpMethod.PUT, env, path, request, null, urlVariables);
  }

  public void delete(Env env, String path, Object... urlVariables) throws RestClientException {
    execute(HttpMethod.DELETE, env, path, null, null, urlVariables);
  }

  private <T> T execute(HttpMethod method, Env env, String path, Object request, Class<T> responseType,
                        Object... uriVariables) {

    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    String uri = uriTemplateHandler.expand(path, uriVariables).getPath();
    Transaction ct = Tracer.newTransaction("AdminAPI", uri);
    ct.addData("Env", env);

    List<ServiceDTO> services = getAdminServices(env, ct);
    HttpHeaders extraHeaders = assembleExtraHeaders(env);

    for (ServiceDTO serviceDTO : services) {
      try {

        T result = doExecute(method, extraHeaders, serviceDTO, path, request, responseType, uriVariables);

        ct.setStatus(Transaction.SUCCESS);
        ct.complete();
        return result;
      } catch (Throwable t) {
        logger.error("Http request failed, uri: {}, method: {}", uri, method, t);
        Tracer.logError(t);
        if (canRetry(t, method)) {
          Tracer.logEvent(TracerEventType.API_RETRY, uri);
        } else {//biz exception rethrow
          ct.setStatus(t);
          ct.complete();
          throw t;
        }
      }
    }

    //all admin server down
    ServiceException e =
        new ServiceException(String.format("Admin servers are unresponsive. meta server address: %s, admin servers: %s",
                portalMetaDomainService.getDomain(env), services));
    ct.setStatus(e);
    ct.complete();
    throw e;
  }

  private <T> ResponseEntity<T> exchangeGet(Env env, String path, ParameterizedTypeReference<T> reference,
                                            Object... uriVariables) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    String uri = uriTemplateHandler.expand(path, uriVariables).getPath();
    Transaction ct = Tracer.newTransaction("AdminAPI", uri);
    ct.addData("Env", env);

    List<ServiceDTO> services = getAdminServices(env, ct);
    HttpEntity<Void> entity = new HttpEntity<>(assembleExtraHeaders(env));

    for (ServiceDTO serviceDTO : services) {
      try {

        ResponseEntity<T> result =
            restTemplate.exchange(parseHost(serviceDTO) + path, HttpMethod.GET, entity, reference, uriVariables);

        ct.setStatus(Transaction.SUCCESS);
        ct.complete();
        return result;
      } catch (Throwable t) {
        logger.error("Http request failed, uri: {}, method: {}", uri, HttpMethod.GET, t);
        Tracer.logError(t);
        if (canRetry(t, HttpMethod.GET)) {
          Tracer.logEvent(TracerEventType.API_RETRY, uri);
        } else {// biz exception rethrow
          ct.setStatus(t);
          ct.complete();
          throw t;
        }

      }
    }

    //all admin server down
    ServiceException e =
        new ServiceException(String.format("Admin servers are unresponsive. meta server address: %s, admin servers: %s",
                portalMetaDomainService.getDomain(env), services));
    ct.setStatus(e);
    ct.complete();
    throw e;

  }

  private HttpHeaders assembleExtraHeaders(Env env) {
    String adminServiceAccessToken = getAdminServiceAccessToken(env);

    if (!Strings.isNullOrEmpty(adminServiceAccessToken)) {
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.AUTHORIZATION, adminServiceAccessToken);
      return headers;
    }

    return null;
  }

  private List<ServiceDTO> getAdminServices(Env env, Transaction ct) {

    List<ServiceDTO> services = adminServiceAddressLocator.getServiceList(env);

    if (CollectionUtils.isEmpty(services)) {
      ServiceException e = new ServiceException(String.format("No available admin server."
                                                              + " Maybe because of meta server down or all admin server down. "
                                                              + "Meta server address: %s",
              portalMetaDomainService.getDomain(env)));
      ct.setStatus(e);
      ct.complete();
      throw e;
    }

    return services;
  }

  private String getAdminServiceAccessToken(Env env) {
    String accessTokens = portalConfig.getAdminServiceAccessTokens();

    if (Strings.isNullOrEmpty(accessTokens)) {
      return null;
    }

    if (!accessTokens.equals(lastAdminServiceAccessTokens)) {
      synchronized (this) {
        adminServiceAccessTokenMap = parseAdminServiceAccessTokens(accessTokens);
        lastAdminServiceAccessTokens = accessTokens;
      }
    }

    return adminServiceAccessTokenMap.get(env);
  }

  private Map<Env, String> parseAdminServiceAccessTokens(String accessTokens) {
    Map<Env, String> tokenMap = Maps.newHashMap();
    try {
      // try to parse
      Map<String, String> map = GSON.fromJson(accessTokens, ACCESS_TOKENS);
      map.forEach((env, token) -> {
        if (Env.exists(env)) {
          tokenMap.put(Env.valueOf(env), token);
        }
      });
    } catch (Exception e) {
      logger.error("Wrong format of admin service access tokens: {}", accessTokens, e);
    }
    return tokenMap;
  }
  private <T> T doExecute(HttpMethod method, HttpHeaders extraHeaders, ServiceDTO service, String path, Object request,
                          Class<T> responseType, Object... uriVariables) {
    T result = null;
    switch (method) {
      case GET:
      case POST:
      case PUT:
      case DELETE:
        HttpEntity entity;
        if (request instanceof HttpEntity) {
          entity = (HttpEntity) request;
          if (!CollectionUtils.isEmpty(extraHeaders)) {
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(entity.getHeaders());
            headers.addAll(extraHeaders);
            entity = new HttpEntity<>(entity.getBody(), headers);
          }
        } else {
          entity = new HttpEntity<>(request, extraHeaders);
        }
        result = restTemplate
            .exchange(parseHost(service) + path, method, entity, responseType, uriVariables)
            .getBody();
        break;
      default:
        throw new UnsupportedOperationException(String.format("unsupported http method(method=%s)", method));
    }
    return result;
  }

  private String parseHost(ServiceDTO serviceAddress) {
    return serviceAddress.getHomepageUrl() + "/";
  }

  //post,delete,put请求在admin server处理超时情况下不重试
  private boolean canRetry(Throwable e, HttpMethod method) {
    Throwable nestedException = e.getCause();
    if (method == HttpMethod.GET) {
      return nestedException instanceof SocketTimeoutException
             || nestedException instanceof HttpHostConnectException
             || nestedException instanceof ConnectTimeoutException;
    }
    return nestedException instanceof HttpHostConnectException
           || nestedException instanceof ConnectTimeoutException;
  }

}
