/*
 * Copyright 2024 Apollo Authors
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
