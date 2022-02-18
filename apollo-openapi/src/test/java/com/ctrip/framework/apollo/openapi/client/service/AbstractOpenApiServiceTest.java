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
package com.ctrip.framework.apollo.openapi.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.openapi.client.constant.ApolloOpenApiConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
abstract class AbstractOpenApiServiceTest {
  @Mock
  protected CloseableHttpClient httpClient;
  @Mock
  protected CloseableHttpResponse someHttpResponse;
  @Mock
  protected StatusLine statusLine;

  protected Gson gson;

  protected String someBaseUrl;

  @Before
  public void setUp() throws Exception {
    gson = new GsonBuilder().setDateFormat(ApolloOpenApiConstants.JSON_DATE_FORMAT).create();
    someBaseUrl = "http://someBaseUrl";

    when(someHttpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(someHttpResponse);
  }

}
