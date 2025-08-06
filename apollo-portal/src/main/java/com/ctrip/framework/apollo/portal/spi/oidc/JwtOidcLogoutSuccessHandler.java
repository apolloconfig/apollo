package com.ctrip.framework.apollo.portal.spi.oidc;

import com.ctrip.framework.apollo.portal.util.CookieUtils;
import com.ctrip.framework.apollo.portal.util.JWTUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ctrip.framework.apollo.portal.constant.JWTConstant.*;

@Component
public class JwtOidcLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JWTUtils jwtUtils;
    @Value("${oidc.base-url:http://localhost:8070/}")
    private  String baseUrl;

    public JwtOidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository,
                                       JWTUtils jwtUtils) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.jwtUtils = jwtUtils;

    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {

        String jwt = jwtUtils.extractAccessTokenFromRequest(request);if (jwt == null || !jwtUtils.isValidToken(jwt)) {
            response.sendRedirect("/");
            return;
        }

        String idToken = jwtUtils.getClaimValue(jwt, ID_TOKEN, String.class);
        String provider = jwtUtils.getClaimValue(jwt, PROVIDER, String.class);

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider);
        if (registration == null || !StringUtils.hasText(idToken)) {
            response.sendRedirect("/");
            return;
        }

        // post logout to oidc provider
        String logoutUrl = UriComponentsBuilder
                .fromHttpUrl(registration.getProviderDetails().getConfigurationMetadata()
                        .get("end_session_endpoint").toString())
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", baseUrl)
                .build()
                .toUriString();
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN);
        response.sendRedirect(logoutUrl);
    }


}