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

package com.ctrip.framework.apollo.portal.spi.configuration;

import com.ctrip.framework.apollo.common.condition.ConditionalOnMissingProfile;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.filter.JwtAuthenticationFilter;
import com.ctrip.framework.apollo.portal.repository.AuthorityRepository;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import com.ctrip.framework.apollo.portal.spi.LogoutHandler;
import com.ctrip.framework.apollo.portal.spi.SsoHeartbeatHandler;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultLogoutHandler;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultSsoHeartbeatHandler;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultUserService;
import com.ctrip.framework.apollo.portal.spi.ldap.LdapUserDetailsService;
import com.ctrip.framework.apollo.portal.spi.ldap.LdapUserService;

import com.ctrip.framework.apollo.portal.filter.JwtOidcAuthenticationFilter;
import com.ctrip.framework.apollo.portal.spi.oidc.JwtOidcLogoutSuccessHandler;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcLogoutHandler;
import com.ctrip.framework.apollo.portal.spi.oidc.JwtAuthenticationSuccessHandler;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcLocalUserServiceImpl;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcAuthenticationSuccessEventListener;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcLocalUserService;
import com.ctrip.framework.apollo.portal.spi.oidc.HttpCookieOAuth2AuthorizationRequestRepository;
import com.ctrip.framework.apollo.portal.spi.oidc.ExcludeClientCredentialsClientRegistrationRepository;

import com.ctrip.framework.apollo.portal.spi.springsecurity.ApolloPasswordEncoderFactory;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;

import java.text.MessageFormat;
import java.util.Collections;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.ctrip.framework.apollo.portal.util.JWTUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class AuthConfiguration {

  private static final String[] BY_PASS_URLS = {"/prometheus/**", "/metrics/**", "/openapi/**",
      "/vendor/**", "/styles/**", "/scripts/**", "/views/**", "/img/**", "/i18n/**", "/prefix-path",
      "/health","/landing.html","/sso_heartbeat","default_sso_heartbeat.html","/refreshToken","/login",};

  /**
   * spring.profiles.active = auth
   */
  @Configuration
  @Profile("auth")
  static class SpringSecurityAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SsoHeartbeatHandler.class)
    public SsoHeartbeatHandler defaultSsoHeartbeatHandler() {
      return new DefaultSsoHeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public static PasswordEncoder passwordEncoder() {
      return ApolloPasswordEncoderFactory.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(UserInfoHolder.class)
    public UserInfoHolder springSecurityUserInfoHolder(UserService userService) {
      return new SpringSecurityUserInfoHolder(userService);
    }

    @Bean
    @ConditionalOnMissingBean(LogoutHandler.class)
    public LogoutHandler logoutHandler() {
      return new DefaultLogoutHandler();
    }

    @Bean
    public static JdbcUserDetailsManager jdbcUserDetailsManager(
            PasswordEncoder passwordEncoder,
            AuthenticationManagerBuilder auth,
            DataSource datasource,
            EntityManagerFactory entityManagerFactory) throws Exception {
      char openQuote = '`';
      char closeQuote = '`';
      try {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(
                SessionFactoryImplementor.class);
        Dialect dialect = sessionFactory.getJdbcServices().getDialect();
        openQuote = dialect.openQuote();
        closeQuote = dialect.closeQuote();
      } catch (Throwable ex) {
        //ignore
      }
      JdbcUserDetailsManager jdbcUserDetailsManager = auth.jdbcAuthentication()
              .passwordEncoder(passwordEncoder).dataSource(datasource)
              .usersByUsernameQuery(MessageFormat.format("SELECT {0}Username{1}, {0}Password{1}, {0}Enabled{1} FROM {0}Users{1} WHERE {0}Username{1} = ?", openQuote, closeQuote))
              .authoritiesByUsernameQuery(MessageFormat.format("SELECT {0}Username{1}, {0}Authority{1} FROM {0}Authorities{1} WHERE {0}Username{1} = ?", openQuote, closeQuote))
              .getUserDetailsService();

      jdbcUserDetailsManager.setUserExistsSql(MessageFormat.format("SELECT {0}Username{1} FROM {0}Users{1} WHERE {0}Username{1} = ?", openQuote, closeQuote));
      jdbcUserDetailsManager.setCreateUserSql(MessageFormat.format("INSERT INTO {0}Users{1} ({0}Username{1}, {0}Password{1}, {0}Enabled{1}) values (?,?,?)", openQuote, closeQuote));
      jdbcUserDetailsManager.setUpdateUserSql(MessageFormat.format("UPDATE {0}Users{1} SET {0}Password{1} = ?, {0}Enabled{1} = ? WHERE {0}Id{1} = (SELECT u.{0}Id{1} FROM (SELECT {0}Id{1} FROM {0}Users{1} WHERE {0}Username{1} = ?) AS u)", openQuote, closeQuote));
      jdbcUserDetailsManager.setDeleteUserSql(MessageFormat.format("DELETE FROM {0}Users{1} WHERE {0}Id{1} = (SELECT u.{0}Id{1} FROM (SELECT {0}Id{1} FROM {0}Users{1} WHERE {0}Username{1} = ?) AS u)", openQuote, closeQuote));
      jdbcUserDetailsManager.setCreateAuthoritySql(MessageFormat.format("INSERT INTO {0}Authorities{1} ({0}Username{1}, {0}Authority{1}) values (?,?)", openQuote, closeQuote));
      jdbcUserDetailsManager.setDeleteUserAuthoritiesSql(MessageFormat.format("DELETE FROM {0}Authorities{1} WHERE {0}Id{1} in (SELECT a.{0}Id{1} FROM (SELECT {0}Id{1} FROM {0}Authorities{1} WHERE {0}Username{1} = ?) AS a)", openQuote, closeQuote));
      jdbcUserDetailsManager.setChangePasswordSql(MessageFormat.format("UPDATE {0}Users{1} SET {0}Password{1} = ? WHERE {0}Id{1} = (SELECT u.{0}Id{1} FROM (SELECT {0}Id{1} FROM {0}Users{1} WHERE {0}Username{1} = ?) AS u)", openQuote, closeQuote));

      return jdbcUserDetailsManager;
    }

    @Bean
    @DependsOn("jdbcUserDetailsManager")
    @ConditionalOnMissingBean(UserService.class)
    public UserService springSecurityUserService(PasswordEncoder passwordEncoder,
        UserRepository userRepository, AuthorityRepository authorityRepository) {
      return new SpringSecurityUserService(passwordEncoder, userRepository, authorityRepository);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
      return authenticationConfiguration.getAuthenticationManager();
    }

  }

  @Order(99)
  @Profile("auth")
  @Configuration
  @EnableWebSecurity
  @EnableGlobalMethodSecurity(prePostEnabled = true)
  static class SpringSecurityConfigurer extends WebSecurityConfigurerAdapter {

    public static final String USER_ROLE = "user";
    private final JWTUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    public SpringSecurityConfigurer(JWTUtils jwtUtils, UserDetailsService userDetailsService,  HandlerExceptionResolver handlerExceptionResolver) {
      this.jwtUtils = jwtUtils;
      this.userDetailsService = userDetailsService;
      this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
            .csrf().disable().sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
              .and()
            .cors()
              .and()
            .authorizeRequests()
            .antMatchers(BY_PASS_URLS).permitAll()
            .anyRequest().authenticated()
            .and()
              .logout().disable()
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils,userDetailsService,handlerExceptionResolver),
                    UsernamePasswordAuthenticationFilter.class);
    }
  }

  /**
   * spring.profiles.active = ldap
   */
  @Configuration
  @Profile("ldap")
  @EnableConfigurationProperties({LdapProperties.class,LdapExtendProperties.class})
  static class SpringSecurityLDAPAuthAutoConfiguration {

    private final LdapProperties properties;
    private final Environment environment;

    public SpringSecurityLDAPAuthAutoConfiguration(final LdapProperties properties,
        final Environment environment) {
      this.properties = properties;
      this.environment = environment;
    }

    @Bean
    @ConditionalOnMissingBean(SsoHeartbeatHandler.class)
    public SsoHeartbeatHandler defaultSsoHeartbeatHandler() {
      return new DefaultSsoHeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean(UserInfoHolder.class)
    public UserInfoHolder springSecurityUserInfoHolder(UserService userService) {
      return new SpringSecurityUserInfoHolder(userService);
    }

    @Bean
    @ConditionalOnMissingBean(LogoutHandler.class)
    public LogoutHandler logoutHandler() {
      return new DefaultLogoutHandler();
    }

    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService springSecurityUserService(LdapTemplate ldapTemplate) {
      return new LdapUserService(ldapTemplate);
    }

    @Bean
    @DependsOn("userSearch")
    public LdapUserDetailsService ldapUserDetailsService(
            LdapUserSearch userSearch,
            LdapAuthoritiesPopulator authoritiesPopulator,
            LdapExtendProperties properties
    ) {
      UserDetailsContextMapper contextMapper = new LdapUserDetailsMapper();

      return new LdapUserDetailsService(
              userSearch,
              contextMapper,
              authoritiesPopulator,
              properties
      );
    }

    @Bean
    @ConditionalOnMissingBean
    public ContextSource ldapContextSource() {
      LdapContextSource source = new LdapContextSource();
      source.setUserDn(this.properties.getUsername());
      source.setPassword(this.properties.getPassword());
      source.setAnonymousReadOnly(this.properties.getAnonymousReadOnly());
      source.setBase(this.properties.getBase());
      source.setUrls(this.properties.determineUrls(this.environment));
      source.setBaseEnvironmentProperties(
          Collections.unmodifiableMap(this.properties.getBaseEnvironment()));
      return source;
    }

    @Bean
    @ConditionalOnMissingBean(LdapOperations.class)
    public LdapTemplate ldapTemplate(ContextSource contextSource) {
      LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
      ldapTemplate.setIgnorePartialResultException(true);
      return ldapTemplate;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
      return authenticationConfiguration.getAuthenticationManager();
    }


  }

  @Order(99)
  @Profile("ldap")
  @Configuration
  @EnableWebSecurity
  @EnableGlobalMethodSecurity(prePostEnabled = true)
  static class SpringSecurityLDAPConfigurer extends WebSecurityConfigurerAdapter {

      private final JWTUtils jwtUtils;
      private final UserDetailsService userDetailsService;
      private final HandlerExceptionResolver handlerExceptionResolver;
      private final LdapAuthenticationProvider ldapAuthProvider;


    public SpringSecurityLDAPConfigurer(JWTUtils jwtUtils, UserDetailsService userDetailsService,
                                         HandlerExceptionResolver handlerExceptionResolver, LdapAuthenticationProvider ldapAuthProvider) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.ldapAuthProvider = ldapAuthProvider;
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .csrf().disable()
          .cors()
          .and()
          .authorizeRequests()
          .antMatchers(BY_PASS_URLS).permitAll()
          .anyRequest().authenticated()
          .and()
              .logout().disable()
          .addFilterBefore(new JwtAuthenticationFilter(jwtUtils,userDetailsService,handlerExceptionResolver),
                  UsernamePasswordAuthenticationFilter.class);

    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.authenticationProvider(ldapAuthProvider);
    }
  }

  @Profile("oidc")
  @EnableConfigurationProperties({OAuth2ClientProperties.class,
      OAuth2ResourceServerProperties.class, OidcExtendProperties.class})
  @Configuration
  static class OidcAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SsoHeartbeatHandler.class)
    public SsoHeartbeatHandler defaultSsoHeartbeatHandler() {
      return new DefaultSsoHeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean(UserInfoHolder.class)
    public UserInfoHolder oidcUserInfoHolder(UserService userService,
        OidcExtendProperties oidcExtendProperties) {
      return new OidcUserInfoHolder(userService, oidcExtendProperties);
    }

    @Bean
    @ConditionalOnMissingBean(LogoutHandler.class)
    public LogoutHandler oidcLogoutHandler() {
      return new OidcLogoutHandler();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
      return SpringSecurityAuthAutoConfiguration.passwordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(JdbcUserDetailsManager.class)
    public JdbcUserDetailsManager jdbcUserDetailsManager(
            PasswordEncoder passwordEncoder,
            AuthenticationManagerBuilder auth,
            DataSource datasource,
            EntityManagerFactory entityManagerFactory) throws Exception {
      return SpringSecurityAuthAutoConfiguration
          .jdbcUserDetailsManager(passwordEncoder, auth, datasource, entityManagerFactory);
    }

    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public OidcLocalUserService oidcLocalUserService(JdbcUserDetailsManager userDetailsManager,
        UserRepository userRepository) {
      return new OidcLocalUserServiceImpl(userDetailsManager, userRepository);
    }

    @Bean
    public OidcAuthenticationSuccessEventListener oidcAuthenticationSuccessEventListener(
            OidcLocalUserService oidcLocalUserService, OidcExtendProperties oidcExtendProperties) {
      return new OidcAuthenticationSuccessEventListener(oidcLocalUserService, oidcExtendProperties);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
    }
  }

  @Profile("oidc")
  @EnableWebSecurity
  @EnableGlobalMethodSecurity(prePostEnabled = true)
  @Configuration
  static class OidcWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    private final InMemoryClientRegistrationRepository clientRegistrationRepository;

    private final OAuth2ResourceServerProperties oauth2ResourceServerProperties;
      private final JwtOidcAuthenticationFilter jwtOidcAuthenticationFilter;
    private final JwtOidcLogoutSuccessHandler jwtOidcLogoutSuccessHandler;

    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    public OidcWebSecurityConfigurerAdapter(
            InMemoryClientRegistrationRepository clientRegistrationRepository,
            OAuth2ResourceServerProperties oauth2ResourceServerProperties, JwtOidcAuthenticationFilter jwtOidcAuthenticationFilter, JwtOidcLogoutSuccessHandler jwtOidcLogoutSuccessHandler, JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler, HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
      this.clientRegistrationRepository = clientRegistrationRepository;
      this.oauth2ResourceServerProperties = oauth2ResourceServerProperties;
        this.jwtOidcAuthenticationFilter = jwtOidcAuthenticationFilter;
        this.jwtOidcLogoutSuccessHandler = jwtOidcLogoutSuccessHandler;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
              .csrf().disable()
              .sessionManagement()
              .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
      http.authorizeRequests(requests -> requests.antMatchers(BY_PASS_URLS).permitAll());
      http.authorizeRequests(requests -> requests.anyRequest().authenticated());

      http.oauth2Login(oauth2 -> oauth2
              .clientRegistrationRepository(new ExcludeClientCredentialsClientRegistrationRepository(this.clientRegistrationRepository))
              .successHandler(jwtAuthenticationSuccessHandler)
              .authorizationEndpoint(auth -> auth
                      .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
              )
      );
      http.addFilterBefore(
              jwtOidcAuthenticationFilter,
              OAuth2AuthorizationRequestRedirectFilter.class
      );
      http.oauth2Client();
      http.logout(configure -> {
        configure.logoutUrl("/logout")
                .logoutSuccessHandler(jwtOidcLogoutSuccessHandler);
      });
      // make jwt optional
      String jwtIssuerUri = this.oauth2ResourceServerProperties.getJwt().getIssuerUri();
      if (!StringUtils.isBlank(jwtIssuerUri)) {
        http.oauth2ResourceServer().jwt();
      }
    }
  }

  /**
   * default profile
   */
  @Configuration
  @ConditionalOnMissingProfile({"ctrip", "auth", "ldap", "oidc"})
  static class DefaultAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SsoHeartbeatHandler.class)
    public SsoHeartbeatHandler defaultSsoHeartbeatHandler() {
      return new DefaultSsoHeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean(UserInfoHolder.class)
    public DefaultUserInfoHolder defaultUserInfoHolder() {
      return new DefaultUserInfoHolder();
    }

    @Bean
    @ConditionalOnMissingBean(LogoutHandler.class)
    public DefaultLogoutHandler logoutHandler() {
      return new DefaultLogoutHandler();
    }

    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService defaultUserService() {
      return new DefaultUserService();
    }
  }

  @ConditionalOnMissingProfile({"auth", "ldap", "oidc"})
  @Configuration
  @EnableWebSecurity
  @EnableGlobalMethodSecurity(prePostEnabled = true)
  static class DefaultWebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable();
      http.headers().frameOptions().sameOrigin();
    }
  }
}
