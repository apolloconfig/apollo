package com.ctrip.framework.apollo.core.signature;

import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author nisiyong
 */
public class Signature {

  /**
   * Authorization=Apollo {appId}:{sign}
   */
  private static final String AUTHORIZATION_FORMAT = "Apollo %s:%s";
  private static final String DELIMITER = "\n";

  public static final String HTTP_HEADER_TIMESTAMP = "Timestamp";

  public static String signature(String timestamp, String pathWithQuery, String secret) {
    String stringToSign = timestamp + DELIMITER + pathWithQuery;
    return HmacSha1Utils.signString(stringToSign, secret);
  }

  public static Map<String, String> buildHttpHeaders(String url, String appId, String secret) {
    long currentTimeMillis = System.currentTimeMillis();
    String timestamp = String.valueOf(currentTimeMillis);

    String pathWithQuery = url2PathWithQuery(url);
    String signature = signature(timestamp, pathWithQuery, secret);

    Map<String, String> headers = Maps.newHashMap();
    headers.put(HttpHeaders.AUTHORIZATION, String.format(AUTHORIZATION_FORMAT, appId, signature));
    headers.put(HTTP_HEADER_TIMESTAMP, timestamp);
    return headers;
  }

  private static String url2PathWithQuery(String urlString) {
    try {
      URL url = new URL(urlString);
      String path = url.getPath();
      String query = url.getQuery();

      String pathWithQuery = path;
      if (query != null && query.length() > 0) {
        pathWithQuery += "?" + query;
      }
      return pathWithQuery;
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url pattern: " + urlString, e);
    }
  }
}
