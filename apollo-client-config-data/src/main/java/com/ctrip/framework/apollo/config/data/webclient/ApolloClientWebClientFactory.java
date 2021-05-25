package com.ctrip.framework.apollo.config.data.webclient;

import com.ctrip.framework.apollo.config.data.authentication.ApolloClientPropertiesFactory;
import com.ctrip.framework.apollo.config.data.authentication.oauth2.ApolloClientAuthorizedClientManagerFactory;
import com.ctrip.framework.apollo.config.data.authentication.oauth2.ApolloClientReactiveAuthorizedClientManagerFactory;
import com.ctrip.framework.apollo.config.data.authentication.properties.ApolloClientAuthenticationProperties;
import com.ctrip.framework.apollo.config.data.authentication.properties.ApolloClientHttpBasicAuthenticationProperties;
import com.ctrip.framework.apollo.config.data.authentication.properties.ApolloClientOauth2AuthenticationProperties;
import com.ctrip.framework.apollo.config.data.authentication.properties.ApolloClientProperties;
import com.ctrip.framework.apollo.config.data.enums.ApolloClientAuthenticationType;
import com.ctrip.framework.apollo.config.data.util.Slf4jLogMessageFormatter;
import com.ctrip.framework.apollo.config.data.webclient.customizer.ApolloClientHttpBasicAuthenticationWebClientCustomizer;
import com.ctrip.framework.apollo.config.data.webclient.customizer.ApolloClientOauth2AuthenticationWebClientCustomizer;
import com.ctrip.framework.apollo.config.data.webclient.customizer.ApolloClientOauth2ReactiveAuthenticationWebClientCustomizer;
import com.ctrip.framework.apollo.config.data.webclient.filter.ApolloClientHttpBasicAuthenticationExchangeFilterFunction;
import org.apache.commons.logging.Log;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientWebClientFactory {

  private final Log log;

  private final ApolloClientPropertiesFactory apolloClientPropertiesFactory;

  private final ApolloClientAuthorizedClientManagerFactory apolloClientAuthorizedClientManagerFactory;

  private final ApolloClientReactiveAuthorizedClientManagerFactory apolloClientReactiveAuthorizedClientManagerFactory;

  public ApolloClientWebClientFactory(Log log) {
    this.log = log;
    this.apolloClientPropertiesFactory = new ApolloClientPropertiesFactory();
    this.apolloClientAuthorizedClientManagerFactory = new ApolloClientAuthorizedClientManagerFactory();
    this.apolloClientReactiveAuthorizedClientManagerFactory = new ApolloClientReactiveAuthorizedClientManagerFactory();
  }

  public WebClient.Builder createWebClient(ApolloClientProperties apolloClientProperties, Binder binder,
      BindHandler bindHandler) {
    ApolloClientAuthenticationProperties properties = apolloClientProperties.getAuthentication();
    if (properties == null) {
      log.debug("apollo client authentication properties is empty, authentication disabled");
      return WebClient.builder();
    }
    ApolloClientAuthenticationType authenticationType = properties.getAuthenticationType();
    log.debug(Slf4jLogMessageFormatter
        .format("apollo client authentication type: {}", authenticationType));
    switch (authenticationType) {
      case NONE:
        return WebClient.builder();
      case OAUTH2:
        return this.createOauth2WebClient(properties, binder, bindHandler);
      case HTTP_BASIC:
        return this.createHttpBasicWebClient(properties, binder, bindHandler);
      default:
        throw new IllegalStateException("Unexpected value: " + authenticationType);
    }
  }

  private WebClient.Builder createOauth2WebClient(ApolloClientAuthenticationProperties properties,
      Binder binder,
      BindHandler bindHandler) {
    ApolloClientOauth2AuthenticationProperties oauth2AuthenticationProperties = properties
        .getOauth2();
    if (oauth2AuthenticationProperties == null) {
      throw new IllegalArgumentException("oauth2AuthenticationProperties must not be null");
    }
    OAuth2ClientProperties oauth2ClientProperties = this.apolloClientPropertiesFactory
        .createOauth2ClientProperties(binder, bindHandler);
    if (oauth2ClientProperties == null) {
      throw new IllegalArgumentException("oauth2ClientProperties must not be null");
    }
    WebApplicationType webApplicationType = oauth2AuthenticationProperties.getWebApplicationType();
    if (WebApplicationType.REACTIVE.equals(webApplicationType)) {
      log.debug("apollo client reactive oauth2 client enabled");
      return this
          .getReactiveOauth2WebClient(oauth2ClientProperties, properties, binder, bindHandler);
    }
    log.debug("apollo client imperative oauth2 client enabled");
    return this.getOauth2WebClient(oauth2ClientProperties, properties, binder, bindHandler);
  }

  /**
   * reactive oauth2 authentication webclient
   */
  private WebClient.Builder getReactiveOauth2WebClient(OAuth2ClientProperties oauth2ClientProperties,
      ApolloClientAuthenticationProperties properties, Binder binder,
      BindHandler bindHandler) {
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager = this.apolloClientReactiveAuthorizedClientManagerFactory
        .createAuthorizedClientManager(oauth2ClientProperties);
    return this.reactiveOauth2WebClient(authorizedClientManager, properties);
  }

  private WebClient.Builder reactiveOauth2WebClient(
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
      ApolloClientAuthenticationProperties properties) {
    ApolloClientOauth2ReactiveAuthenticationWebClientCustomizer customizer = this
        .reactiveOauth2AuthenticationWebClientCustomizer(authorizedClientManager, properties);
    WebClient.Builder webClientBuilder = WebClient.builder();
    customizer.customize(webClientBuilder);
    return webClientBuilder;
  }

  private ApolloClientOauth2ReactiveAuthenticationWebClientCustomizer reactiveOauth2AuthenticationWebClientCustomizer(
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
      ApolloClientAuthenticationProperties properties) {
    ServerOAuth2AuthorizedClientExchangeFilterFunction filterFunction =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    filterFunction.setDefaultOAuth2AuthorizedClient(true);
    filterFunction
        .setDefaultClientRegistrationId(properties.getOauth2().getDefaultClientRegistrationId());
    return new ApolloClientOauth2ReactiveAuthenticationWebClientCustomizer(filterFunction);
  }

  /**
   * oauth2 authentication webclient
   */
  private WebClient.Builder getOauth2WebClient(OAuth2ClientProperties oauth2ClientProperties,
      ApolloClientAuthenticationProperties properties, Binder binder,
      BindHandler bindHandler) {
    OAuth2AuthorizedClientManager authorizedClientManager = this.apolloClientAuthorizedClientManagerFactory
        .createAuthorizedClientManager(oauth2ClientProperties);
    return this.oauth2WebClient(authorizedClientManager, properties);
  }

  private WebClient.Builder oauth2WebClient(OAuth2AuthorizedClientManager authorizedClientManager,
      ApolloClientAuthenticationProperties properties) {
    ApolloClientOauth2AuthenticationWebClientCustomizer customizer = this
        .oauth2AuthenticationWebClientCustomizer(authorizedClientManager, properties);
    WebClient.Builder webClientBuilder = WebClient.builder();
    customizer.customize(webClientBuilder);
    return webClientBuilder;
  }

  private ApolloClientOauth2AuthenticationWebClientCustomizer oauth2AuthenticationWebClientCustomizer(
      OAuth2AuthorizedClientManager authorizedClientManager,
      ApolloClientAuthenticationProperties properties) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction filterFunction =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    filterFunction.setDefaultOAuth2AuthorizedClient(true);
    filterFunction
        .setDefaultClientRegistrationId(properties.getOauth2().getDefaultClientRegistrationId());
    return new ApolloClientOauth2AuthenticationWebClientCustomizer(filterFunction);
  }

  /**
   * http basic authentication webclient
   */
  private WebClient.Builder createHttpBasicWebClient(ApolloClientAuthenticationProperties properties,
      Binder binder,
      BindHandler bindHandler) {
    properties.getHttpBasic().validate();
    return this.httpBasicWebClient(properties);
  }

  private WebClient.Builder httpBasicWebClient(ApolloClientAuthenticationProperties properties) {
    ApolloClientHttpBasicAuthenticationWebClientCustomizer customizer = this
        .httpBasicAuthenticationWebClientCustomizer(properties);
    WebClient.Builder webClientBuilder = WebClient.builder();
    customizer.customize(webClientBuilder);
    return webClientBuilder;
  }

  private ApolloClientHttpBasicAuthenticationWebClientCustomizer httpBasicAuthenticationWebClientCustomizer(
      ApolloClientAuthenticationProperties properties) {
    ApolloClientHttpBasicAuthenticationProperties httpBasic = properties.getHttpBasic();
    if (httpBasic.validateUsernameAndPassword()) {
      return new ApolloClientHttpBasicAuthenticationWebClientCustomizer(
          new ApolloClientHttpBasicAuthenticationExchangeFilterFunction(httpBasic.getUsername(),
              httpBasic.getPassword()));
    }
    if (httpBasic.validateEncodedCredentials()) {
      return new ApolloClientHttpBasicAuthenticationWebClientCustomizer(
          new ApolloClientHttpBasicAuthenticationExchangeFilterFunction(
              httpBasic.getEncodedCredentials()));
    }
    throw new IllegalStateException("username password pair or encodedCredentials expected");
  }
}
