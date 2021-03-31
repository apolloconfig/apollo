package com.ctrip.framework.apollo.util.http;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import java.lang.reflect.Type;

/**
 * @author Jason Song(song_s@ctrip.com)
 * @deprecated in favor of the interface {@link HttpClient} and it's default implementation {@link
 * DefaultHttpClient}
 */
@Deprecated
public class HttpUtil implements HttpClient {

  private HttpClient m_httpClient;

  /**
   * Constructor.
   */
  public HttpUtil() {
    m_httpClient = new DefaultHttpClient();
  }

  /**
   * Do get operation for the http request.
   *
   * @param httpRequest  the request
   * @param responseType the response type
   * @return the response
   * @throws ApolloConfigException if any error happened or response code is neither 200 nor 304
   */
  @Override
  public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Class<T> responseType) {
    return m_httpClient.doGet(httpRequest, responseType);
  }

  /**
   * Do get operation for the http request.
   *
   * @param httpRequest  the request
   * @param responseType the response type
   * @return the response
   * @throws ApolloConfigException if any error happened or response code is neither 200 nor 304
   */
  @Override
  public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Type responseType) {
    return m_httpClient.doGet(httpRequest, responseType);
  }
}
