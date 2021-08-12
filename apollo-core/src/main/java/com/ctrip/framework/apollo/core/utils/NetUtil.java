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
package com.ctrip.framework.apollo.core.utils;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by gl49 on 2018/6/8.
 */
public class NetUtil {

  private static final int DEFAULT_TIMEOUT_IN_SECONDS = 5000;

  /**
   * ping the url, return true if ping ok, false otherwise
   */
  public static boolean pingUrl(String address) {
    try {
      URL urlObj = new URL(address);
      HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
      connection.setRequestMethod("GET");
      connection.setUseCaches(false);
      connection.setConnectTimeout(DEFAULT_TIMEOUT_IN_SECONDS);
      connection.setReadTimeout(DEFAULT_TIMEOUT_IN_SECONDS);
      int statusCode = connection.getResponseCode();
      cleanUpConnection(connection);
      return (200 <= statusCode && statusCode <= 399);
    } catch (Throwable ignore) {
    }
    return false;
  }

  /**
   * according to https://docs.oracle.com/javase/7/docs/technotes/guides/net/http-keepalive.html, we should clean up the
   * connection by reading the response body so that the connection could be reused.
   */
  private static void cleanUpConnection(HttpURLConnection conn) {
    InputStreamReader isr = null;
    InputStreamReader esr = null;
    try {
      isr = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
      CharStreams.toString(isr);
    } catch (IOException e) {
      InputStream errorStream = conn.getErrorStream();

      if (errorStream != null) {
        esr = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
        try {
          CharStreams.toString(esr);
        } catch (IOException ioe) {
          //ignore
        }
      }
    } finally {
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ex) {
          // ignore
        }
      }

      if (esr != null) {
        try {
          esr.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    }
  }
}
