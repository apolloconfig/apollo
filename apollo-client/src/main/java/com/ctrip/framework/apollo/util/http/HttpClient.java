package com.ctrip.framework.apollo.util.http;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import java.lang.reflect.Type;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface HttpClient {

  /**
   * Do get operation for the http request.
   *
   * @param httpRequest  the request
   * @param responseType the response type
   * @return the response
   * @throws ApolloConfigException if any error happened or response code is neither 200 nor 304
   */
  <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Class<T> responseType)
      throws ApolloConfigException;

  /**
   * Do get operation for the http request.
   *
   * @param httpRequest  the request
   * @param responseType the response type
   * @return the response
   * @throws ApolloConfigException if any error happened or response code is neither 200 nor 304
   */
  <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Type responseType)
      throws ApolloConfigException;
}
