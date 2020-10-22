package com.ctrip.framework.apollo.openapi.client;

import com.ctrip.framework.apollo.openapi.client.constant.ApolloOpenApiConstants;
import com.ctrip.framework.apollo.openapi.client.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.client.service.ClusterOpenApiService;
import com.ctrip.framework.apollo.openapi.client.service.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.client.service.NamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.client.service.ReleaseOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenReleaseDTO;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.util.List;

/**
 * This class contains collections of methods to access Apollo Open Api.
 * <br />
 * For more information, please refer <a href="https://github.com/ctripcorp/apollo/wiki/">Apollo Wiki</a>.
 *
 */
public class ApolloOpenApiClient {
  private final String portalUrl;
  private final String token;
  private final AppOpenApiService appService;
  private final ItemOpenApiService itemService;
  private final ReleaseOpenApiService releaseService;
  private final NamespaceOpenApiService namespaceService;
  private final ClusterOpenApiService clusterService;
  private static final Gson GSON = new GsonBuilder().setDateFormat(ApolloOpenApiConstants.JSON_DATE_FORMAT).create();

  private ApolloOpenApiClient(String portalUrl, String token, RequestConfig requestConfig) {
    this.portalUrl = portalUrl;
    this.token = token;
    CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig)
        .setDefaultHeaders(Lists.newArrayList(new BasicHeader("Authorization", token))).build();

    String baseUrl = this.portalUrl + ApolloOpenApiConstants.OPEN_API_V1_PREFIX;
    appService = new AppOpenApiService(client, baseUrl, GSON);
    clusterService = new ClusterOpenApiService(client, baseUrl, GSON);
    namespaceService = new NamespaceOpenApiService(client, baseUrl, GSON);
    itemService = new ItemOpenApiService(client, baseUrl, GSON);
    releaseService = new ReleaseOpenApiService(client, baseUrl, GSON);
  }

  /**
   * Get the environment and cluster information
   */
  public List<OpenEnvClusterDTO> getEnvClusterInfo(String appId) {
    return appService.getEnvClusterInfo(appId);
  }

  /**
   * Get all App information
   */
  public List<OpenAppDTO> getAllApps() {
    return appService.getAppsInfo(null);
  }

  /**
   * Get App information by app ids
   */
  public List<OpenAppDTO> getAppsByIds(List<String> appIds) {
    return appService.getAppsInfo(appIds);
  }

  /**
   * Get the namespaces
   */
  public List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName) {
    return namespaceService.getNamespaces(appId, env, clusterName);
  }

  /**
   * Get the cluster
   *
   * @since 1.5.0
   */
  public OpenClusterDTO getCluster(String appId, String env, String clusterName) {
    return clusterService.getCluster(appId, env, clusterName);
  }

  /**
   * Create the cluster
   *
   * @since 1.5.0
   */
  public OpenClusterDTO createCluster(String env, OpenClusterDTO openClusterDTO) {
    return clusterService.createCluster(env, openClusterDTO);
  }

  /**
   * Get the namespace
   */
  public OpenNamespaceDTO getNamespace(String appId, String env, String clusterName, String namespaceName) {
    return namespaceService.getNamespace(appId, env, clusterName, namespaceName);
  }

  /**
   * Create the app namespace
   */
  public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespaceDTO) {
    return namespaceService.createAppNamespace(appNamespaceDTO);
  }

  /**
   * Get the namespace lock
   */
  public OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName, String namespaceName) {
    return namespaceService.getNamespaceLock(appId, env, clusterName, namespaceName);
  }

  /**
   * Get config
   *
   * @return the item or null if not exists
   *
   * @since 1.2.0
   */
  public OpenItemDTO getItem(String appId, String env, String clusterName, String namespaceName, String key) {
    return itemService.getItem(appId, env, clusterName, namespaceName, key);
  }

  /**
   * Add config
   * @return the created config
   */
  public OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {
    return itemService.createItem(appId, env, clusterName, namespaceName, itemDTO);
  }

  /**
   * Update config
   */
  public void updateItem(String appId, String env, String clusterName, String namespaceName, OpenItemDTO itemDTO) {
    itemService.updateItem(appId, env, clusterName, namespaceName, itemDTO);
  }

  /**
   * Create config if not exists or update config if already exists
   */
  public void createOrUpdateItem(String appId, String env, String clusterName, String namespaceName, OpenItemDTO itemDTO) {
    itemService.createOrUpdateItem(appId, env, clusterName, namespaceName, itemDTO);
  }

  /**
   * Remove config
   *
   * @param operator the user who removes the item
   */
  public void removeItem(String appId, String env, String clusterName, String namespaceName, String key,
      String operator) {
    itemService.removeItem(appId, env, clusterName, namespaceName, key, operator);
  }

  /**
   * publish namespace
   * @return the released configurations
   */
  public OpenReleaseDTO publishNamespace(String appId, String env, String clusterName, String namespaceName,
      NamespaceReleaseDTO releaseDTO) {
    return releaseService.publishNamespace(appId, env, clusterName, namespaceName, releaseDTO);
  }

  /**
   * @return the latest active release information or <code>null</code> if not found
   */
  public OpenReleaseDTO getLatestActiveRelease(String appId, String env, String clusterName, String namespaceName) {
    return releaseService.getLatestActiveRelease(appId, env, clusterName, namespaceName);
  }

  /**
   * Rollback the release
   *
   * @param operator the user who rollbacks the release
   * @since 1.5.0
   */
  public void rollbackRelease(String env, long releaseId, String operator) {
    releaseService.rollbackRelease(env, releaseId, operator);
  }


  public String getPortalUrl() {
    return portalUrl;
  }

  public String getToken() {
    return token;
  }

  public static ApolloOpenApiClientBuilder newBuilder() {
    return new ApolloOpenApiClientBuilder();
  }

  public static class ApolloOpenApiClientBuilder {

    private String portalUrl;
    private String token;
    private int connectTimeout = -1;
    private int readTimeout = -1;

    /**
     * @param portalUrl The apollo portal url, e.g http://localhost:8070
     */
    public ApolloOpenApiClientBuilder withPortalUrl(String portalUrl) {
      this.portalUrl = portalUrl;
      return this;
    }

    /**
     * @param token The authorization token, e.g. e16e5cd903fd0c97a116c873b448544b9d086de8
     */
    public ApolloOpenApiClientBuilder withToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * @param connectTimeout an int that specifies the connect timeout value in milliseconds
     */
    public ApolloOpenApiClientBuilder withConnectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * @param readTimeout an int that specifies the timeout value to be used in milliseconds
     */
    public ApolloOpenApiClientBuilder withReadTimeout(int readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public ApolloOpenApiClient build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(portalUrl), "Portal url should not be null or empty!");
      Preconditions.checkArgument(portalUrl.startsWith("http://") || portalUrl.startsWith("https://"), "Portal url should start with http:// or https://" );
      Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token should not be null or empty!");

      if (connectTimeout < 0) {
        connectTimeout = ApolloOpenApiConstants.DEFAULT_CONNECT_TIMEOUT;
      }

      if (readTimeout < 0) {
        readTimeout = ApolloOpenApiConstants.DEFAULT_READ_TIMEOUT;
      }

      RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout)
          .setSocketTimeout(readTimeout).build();

      return new ApolloOpenApiClient(portalUrl, token, requestConfig);
    }
  }
}
