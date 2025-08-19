package com.ctrip.framework.apollo.portal.filter;

import com.ctrip.framework.apollo.portal.component.AuthContextHolder;
import com.ctrip.framework.apollo.portal.constant.AuthConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthTypeResolverFilter extends OncePerRequestFilter {
    private static final String PORTAL_TOKEN_HEADER = "Apollo-Portal-Token";
    private static final String PORTAL_TOKEN_PARAM  = "accessToken";
    static final String CONSUMER_ID = "ApolloConsumerId";
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authType = resolve(request);
        AuthContextHolder.setAuthType(authType);
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
        }
    }

    private String resolve(HttpServletRequest req) {
        if (req.getHeader(CONSUMER_ID) != null){
            return AuthConstants.CONSUMER;
        }

        return AuthConstants.USER;
    }
}
