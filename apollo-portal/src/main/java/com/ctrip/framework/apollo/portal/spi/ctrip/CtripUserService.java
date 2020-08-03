package com.ctrip.framework.apollo.portal.spi.ctrip;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class CtripUserService implements UserService {
  private RestTemplate restTemplate;
  private List<String> searchUserMatchFields;
  private ParameterizedTypeReference<Map<String, List<UserServiceResponse>>> responseType;
  private PortalConfig portalConfig;

  public CtripUserService(PortalConfig portalConfig) {
    this.portalConfig = portalConfig;
    this.restTemplate = new RestTemplate(clientHttpRequestFactory());
    this.searchUserMatchFields =
        Lists.newArrayList("empcode", "empaccount", "displayname", "c_name", "pinyin");
    this.responseType = new ParameterizedTypeReference<Map<String, List<UserServiceResponse>>>() {
    };
  }

  private ClientHttpRequestFactory clientHttpRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(portalConfig.connectTimeout());
    factory.setReadTimeout(portalConfig.readTimeout());

    return factory;
  }

  @Override
  public List<UserInfo> searchUsers(String keyword, int offset, int limit) {
    UserServiceRequest request = assembleSearchUserRequest(keyword, offset, limit);

    HttpEntity<UserServiceRequest> entity = new HttpEntity<>(request);
    ResponseEntity<Map<String, List<UserServiceResponse>>> response =
        restTemplate.exchange(portalConfig.userServiceUrl(), HttpMethod.POST, entity, responseType);

    if (!response.getBody().containsKey("result")) {
      return Collections.emptyList();
    }

    List<UserInfo> result = Lists.newArrayList();
    result.addAll(
        response.getBody().get("result").stream().map(this::transformUserServiceResponseToUserInfo)
            .collect(Collectors.toList()));

    return result;
  }

  @Override
  public UserInfo findByUserId(String userId) {
    List<UserInfo> userInfoList = this.findByUserIds(Lists.newArrayList(userId));
    if (CollectionUtils.isEmpty(userInfoList)) {
      return null;
    }
    return userInfoList.get(0);
  }

  @Override
  public List<UserInfo> findByUserIds(List<String> userIds) {
    UserServiceRequest request = assembleFindUserRequest(Lists.newArrayList(userIds));

    HttpEntity<UserServiceRequest> entity = new HttpEntity<>(request);
    ResponseEntity<Map<String, List<UserServiceResponse>>> response =
        restTemplate.exchange(portalConfig.userServiceUrl(), HttpMethod.POST, entity, responseType);

    if (!response.getBody().containsKey("result")) {
      return Collections.emptyList();
    }

    List<UserInfo> result = Lists.newArrayList();
    result.addAll(
        response.getBody().get("result").stream().map(this::transformUserServiceResponseToUserInfo)
            .collect(Collectors.toList()));

    return result;
  }

  private UserInfo transformUserServiceResponseToUserInfo(UserServiceResponse userServiceResponse) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userServiceResponse.getEmpaccount());
    userInfo.setName(userServiceResponse.getDisplayname());
    userInfo.setEmail(userServiceResponse.getEmail());
    return userInfo;
  }

  UserServiceRequest assembleSearchUserRequest(String keyword, int offset, int limit) {
    Map<String, Object> query = Maps.newHashMap();
    Map<String, Object> multiMatchMap = Maps.newHashMap();
    multiMatchMap.put("fields", searchUserMatchFields);
    multiMatchMap.put("operator", "and");
    multiMatchMap.put("query", keyword);
    multiMatchMap.put("type", "best_fields");
    query.put("multi_match", multiMatchMap);

    return assembleUserServiceRequest(query, offset, limit);
  }

  UserServiceRequest assembleFindUserRequest(List<String> userIds) {
    Map<String, Object>
        query =
        ImmutableMap.of("filtered", ImmutableMap
            .of("filter", ImmutableMap.of("terms", ImmutableMap.of("empaccount", userIds))));

    return assembleUserServiceRequest(query, 0, userIds.size());
  }

  private UserServiceRequest assembleUserServiceRequest(Map<String, Object> query, int offset,
                                                        int limit) {
    UserServiceRequest request = new UserServiceRequest();
    request.setAccess_token(portalConfig.userServiceAccessToken());

    UserServiceRequestBody requestBody = new UserServiceRequestBody();
    requestBody.setIndexAlias("itdb_emloyee");
    requestBody.setType("emloyee");
    request.setRequest_body(requestBody);

    Map<String, Object> queryJson = Maps.newHashMap();
    requestBody.setQueryJson(queryJson);

    queryJson.put("query", query);

    queryJson.put("from", offset);
    queryJson.put("size", limit);

    return request;
  }


  static class UserServiceRequest {
    private String access_token;
    private UserServiceRequestBody request_body;

    public String getAccess_token() {
      return access_token;
    }

    public void setAccess_token(String access_token) {
      this.access_token = access_token;
    }

    public UserServiceRequestBody getRequest_body() {
      return request_body;
    }

    public void setRequest_body(
        UserServiceRequestBody request_body) {
      this.request_body = request_body;
    }
  }

  static class UserServiceRequestBody {
    private String indexAlias;
    private String type;
    private Map<String, Object> queryJson;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getIndexAlias() {
      return indexAlias;
    }

    public void setIndexAlias(String indexAlias) {
      this.indexAlias = indexAlias;
    }

    public Map<String, Object> getQueryJson() {
      return queryJson;
    }

    public void setQueryJson(Map<String, Object> queryJson) {
      this.queryJson = queryJson;
    }
  }

  static class UserServiceResponse {
    private String empaccount;
    private String displayname;
    private String email;

    public String getEmpaccount() {
      return empaccount;
    }

    public void setEmpaccount(String empaccount) {
      this.empaccount = empaccount;
    }

    public String getDisplayname() {
      return displayname;
    }

    public void setDisplayname(String displayname) {
      this.displayname = displayname;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }

}
