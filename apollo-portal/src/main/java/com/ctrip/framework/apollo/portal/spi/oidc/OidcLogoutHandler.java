package com.ctrip.framework.apollo.portal.spi.oidc;

import com.ctrip.framework.apollo.portal.spi.LogoutHandler;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class OidcLogoutHandler implements LogoutHandler {

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.sendRedirect(request.getContextPath() + "/logout");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
