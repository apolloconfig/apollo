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
package com.ctrip.framework.apollo.util.http;

import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class HttpRequest {
  private String m_url;
  private Map<String, String> headers;
  private int m_connectTimeout;
  private int m_readTimeout;

  /**
   * Create the request for the url.
   * @param url the url
   */
  public HttpRequest(String url) {
    this.m_url = url;
    m_connectTimeout = -1;
    m_readTimeout = -1;
  }

  public String getUrl() {
    return m_url;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public int getConnectTimeout() {
    return m_connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.m_connectTimeout = connectTimeout;
  }

  public int getReadTimeout() {
    return m_readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.m_readTimeout = readTimeout;
  }
}
