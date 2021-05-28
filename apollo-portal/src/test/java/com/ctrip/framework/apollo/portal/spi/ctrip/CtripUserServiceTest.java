/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.portal.spi.ctrip;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.ctrip.framework.apollo.portal.AbstractUnitTest;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class CtripUserServiceTest extends AbstractUnitTest{
  private CtripUserService ctripUserService;
  private String someUserServiceUrl;
  private String someUserServiceToken;
  private ParameterizedTypeReference<Map<String, List<CtripUserService.UserServiceResponse>>>
      someResponseType;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private RestTemplate restTemplate;

  @Before
  public void setUp() throws Exception {
    when(portalConfig.connectTimeout()).thenReturn(3000);
    when(portalConfig.readTimeout()).thenReturn(3000);
    ctripUserService = new CtripUserService(portalConfig);
    ReflectionTestUtils.setField(ctripUserService, "restTemplate", restTemplate);
    someResponseType =
        (ParameterizedTypeReference<Map<String, List<CtripUserService.UserServiceResponse>>>) ReflectionTestUtils
            .getField(ctripUserService, "responseType");

    someUserServiceUrl = "http://someurl";
    someUserServiceToken = "someToken";
    when(portalConfig.userServiceUrl()).thenReturn(someUserServiceUrl);
    when(portalConfig.userServiceAccessToken()).thenReturn(someUserServiceToken);

  }

  @Test
  public void testAssembleSearchUserRequest() throws Exception {
    String someKeyword = "someKeyword";
    int someOffset = 0;
    int someLimit = 10;

    CtripUserService.UserServiceRequest request =
        ctripUserService.assembleSearchUserRequest(someKeyword, someOffset, someLimit);

    assertEquals(someUserServiceToken, request.getAccess_token());

    CtripUserService.UserServiceRequestBody requestBody = request.getRequest_body();

    assertEquals("itdb_emloyee", requestBody.getIndexAlias());

    Map<String, Object> queryJson = requestBody.getQueryJson();
    assertEquals(someOffset, queryJson.get("from"));
    assertEquals(someLimit, queryJson.get("size"));

    Map<String, Object> query = (Map<String, Object>) queryJson.get("query");
    Map<String, Object> multiMatchMap = (Map<String, Object>) query.get("multi_match");
    assertEquals(someKeyword, multiMatchMap.get("query"));
  }

  @Test
  public void testAssembleFindUserRequest() throws Exception {
    String someUserId = "someUser";
    String anotherUserId = "anotherUser";
    List<String> userIds = Lists.newArrayList(someUserId, anotherUserId);

    CtripUserService.UserServiceRequest request = ctripUserService.assembleFindUserRequest(userIds);

    assertEquals(someUserServiceToken, request.getAccess_token());

    CtripUserService.UserServiceRequestBody requestBody = request.getRequest_body();

    assertEquals("itdb_emloyee", requestBody.getIndexAlias());

    Map<String, Object> queryJson = requestBody.getQueryJson();
    assertEquals(0, queryJson.get("from"));
    assertEquals(2, queryJson.get("size"));

    Map<String, Object> query = (Map<String, Object>) queryJson.get("query");
    Map<String, Object> terms =
        getMapFromMap(getMapFromMap(getMapFromMap(query, "filtered"), "filter"), "terms");

    List<String> userIdTerms = (List<String>) terms.get("empaccount");

    assertTrue(userIdTerms.containsAll(userIds));

  }

  private Map<String, Object> getMapFromMap(Map<String, Object> map, String key) {
    return (Map<String, Object>) map.get(key);
  }

  @Test
  public void testSearchUsers() throws Exception {
    String someUserId = "someUserId";
    String someName = "someName";
    String someEmail = "someEmail";
    String anotherUserId = "anotherUserId";
    String anotherName = "anotherName";
    String anotherEmail = "anotherEmail";
    String someKeyword = "someKeyword";
    int someOffset = 0;
    int someLimit = 10;

    CtripUserService.UserServiceResponse someUserResponse =
        assembleUserServiceResponse(someUserId, someName, someEmail);

    CtripUserService.UserServiceResponse anotherUserResponse =
        assembleUserServiceResponse(anotherUserId, anotherName, anotherEmail);

    Map<String, List<CtripUserService.UserServiceResponse>> resultMap =
        ImmutableMap.of("result", Lists.newArrayList(someUserResponse, anotherUserResponse));
    ResponseEntity<Map<String, List<CtripUserService.UserServiceResponse>>> someResponse
        = new ResponseEntity<>(resultMap, HttpStatus.OK);

    when(restTemplate.exchange(eq(someUserServiceUrl), eq(HttpMethod.POST), any(HttpEntity.class),
        eq(someResponseType))).thenReturn(someResponse);

    List<UserInfo> users = ctripUserService.searchUsers(someKeyword, someOffset, someLimit);

    assertEquals(2, users.size());
    compareUserInfoAndUserServiceResponse(someUserResponse, users.get(0));
    compareUserInfoAndUserServiceResponse(anotherUserResponse, users.get(1));
  }

  @Test(expected = HttpClientErrorException.class)
  public void testSearchUsersWithError() throws Exception {
    when(restTemplate.exchange(eq(someUserServiceUrl), eq(HttpMethod.POST), any(HttpEntity.class),
        eq(someResponseType)))
        .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    String someKeyword = "someKeyword";
    int someOffset = 0;
    int someLimit = 10;

    ctripUserService.searchUsers(someKeyword, someOffset, someLimit);
  }

  @Test
  public void testFindByUserId() throws Exception {
    String someUserId = "someUserId";
    String someName = "someName";
    String someEmail = "someEmail";

    CtripUserService.UserServiceResponse someUserResponse =
        assembleUserServiceResponse(someUserId, someName, someEmail);

    Map<String, List<CtripUserService.UserServiceResponse>> resultMap =
        ImmutableMap.of("result", Lists.newArrayList(someUserResponse));
    ResponseEntity<Map<String, List<CtripUserService.UserServiceResponse>>> someResponse
        = new ResponseEntity<>(resultMap, HttpStatus.OK);

    when(restTemplate.exchange(eq(someUserServiceUrl), eq(HttpMethod.POST), any(HttpEntity.class),
        eq(someResponseType))).thenReturn(someResponse);

    UserInfo user = ctripUserService.findByUserId(someUserId);
    compareUserInfoAndUserServiceResponse(someUserResponse, user);
  }

  @Test
  public void testFindByUserIds() throws Exception {
    String someUserId = "someUserId";
    String someName = "someName";
    String someEmail = "someEmail";
    String anotherUserId = "anotherUserId";
    String anotherName = "anotherName";
    String anotherEmail = "anotherEmail";
    String someKeyword = "someKeyword";

    CtripUserService.UserServiceResponse someUserResponse =
        assembleUserServiceResponse(someUserId, someName, someEmail);

    CtripUserService.UserServiceResponse anotherUserResponse =
        assembleUserServiceResponse(anotherUserId, anotherName, anotherEmail);

    Map<String, List<CtripUserService.UserServiceResponse>> resultMap =
        ImmutableMap.of("result", Lists.newArrayList(someUserResponse, anotherUserResponse));
    ResponseEntity<Map<String, List<CtripUserService.UserServiceResponse>>> someResponse
        = new ResponseEntity<>(resultMap, HttpStatus.OK);

    when(restTemplate.exchange(eq(someUserServiceUrl), eq(HttpMethod.POST), any(HttpEntity.class),
        eq(someResponseType))).thenReturn(someResponse);

    List<UserInfo> users =
        ctripUserService.findByUserIds(Lists.newArrayList(someUserId, anotherUserId));

    assertEquals(2, users.size());
    compareUserInfoAndUserServiceResponse(someUserResponse, users.get(0));
    compareUserInfoAndUserServiceResponse(anotherUserResponse, users.get(1));
  }

  private void compareUserInfoAndUserServiceResponse(
      CtripUserService.UserServiceResponse userServiceReponse, UserInfo userInfo) {
    assertEquals(userServiceReponse.getEmpaccount(), userInfo.getUserId());
    assertEquals(userServiceReponse.getDisplayname(), userInfo.getName());
    assertEquals(userServiceReponse.getEmail(), userInfo.getEmail());
  }

  private CtripUserService.UserServiceResponse assembleUserServiceResponse(String userId,
                                                                           String name,
                                                                           String email) {
    CtripUserService.UserServiceResponse response = new CtripUserService.UserServiceResponse();
    response.setEmpaccount(userId);
    response.setDisplayname(name);
    response.setEmail(email);
    return response;
  }
}
