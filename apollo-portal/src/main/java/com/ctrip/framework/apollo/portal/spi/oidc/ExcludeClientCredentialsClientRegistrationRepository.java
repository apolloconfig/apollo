package com.ctrip.framework.apollo.portal.spi.oidc;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ExcludeClientCredentialsClientRegistrationRepository implements
    ClientRegistrationRepository, Iterable<ClientRegistration> {

  /**
   * origin clientRegistrationRepository
   */
  private final InMemoryClientRegistrationRepository delegate;

  /**
   * exclude client_credentials
   */
  private final List<ClientRegistration> clientRegistrationList;

  public ExcludeClientCredentialsClientRegistrationRepository(
      InMemoryClientRegistrationRepository delegate) {
    Objects.requireNonNull(delegate, "clientRegistrationRepository cannot be null");
    this.delegate = delegate;
    this.clientRegistrationList = Collections.unmodifiableList(StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(delegate.iterator(), Spliterator.ORDERED),
            false)
        .filter(clientRegistration -> !AuthorizationGrantType.CLIENT_CREDENTIALS
            .equals(clientRegistration.getAuthorizationGrantType()))
        .collect(Collectors.toList()));
  }

  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    ClientRegistration clientRegistration = this.delegate.findByRegistrationId(registrationId);
    if (clientRegistration == null) {
      return null;
    }
    if (AuthorizationGrantType.CLIENT_CREDENTIALS
        .equals(clientRegistration.getAuthorizationGrantType())) {
      return null;
    }
    return clientRegistration;
  }

  @Override
  public Iterator<ClientRegistration> iterator() {
    return this.clientRegistrationList.iterator();
  }
}
