package com.ctrip.framework.apollo.portal.spi.oidc;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class OidcUserInfoHolder implements UserInfoHolder {

  private static final Logger log = LoggerFactory.getLogger(OidcUserInfoHolder.class);

  @Override
  public UserInfo getUser() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof OidcUser) {
      UserInfo userInfo = new UserInfo();
      OidcUser oidcUser = (OidcUser) principal;
      userInfo.setUserId(oidcUser.getSubject());
      userInfo.setName(oidcUser.getPreferredUsername());
      userInfo.setEmail(oidcUser.getEmail());
      return userInfo;
    }
    if (principal instanceof Jwt) {
      Jwt jwt = (Jwt) principal;
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(jwt.getSubject());
      return userInfo;
    }
    log.debug("principal is neither oidcUser nor jwt, principal=[{}]", principal);
    if (principal instanceof OAuth2User) {
      UserInfo userInfo = new UserInfo();
      OAuth2User oAuth2User = (OAuth2User) principal;
      userInfo.setUserId(oAuth2User.getName());
      userInfo.setName(oAuth2User.getAttribute(StandardClaimNames.PREFERRED_USERNAME));
      userInfo.setEmail(oAuth2User.getAttribute(StandardClaimNames.EMAIL));
      return userInfo;
    }
    if (principal instanceof Principal) {
      UserInfo userInfo = new UserInfo();
      Principal userPrincipal = (Principal) principal;
      userInfo.setUserId(userPrincipal.getName());
      return userInfo;
    }
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(String.valueOf(principal));
    return userInfo;
  }
}
