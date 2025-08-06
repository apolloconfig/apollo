/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.util;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.ctrip.framework.apollo.portal.constant.JWTConstant.ACCESS_TOKEN_HEADER;
import static com.ctrip.framework.apollo.portal.constant.JWTConstant.ACCESS_TOKEN;
import static com.ctrip.framework.apollo.portal.constant.JWTConstant.ISSUER;

@Component
public class JWTUtils {

    // HS512 requires minimum 512-bit (64-byte) secret
    @Value("${jwt.secret:your-64-bytes-secret-key-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx}")
    private String secret;
    private  SecretKey SECRET_KEY;
    // Token validity periods (milliseconds)
    @Value("${jwt.token.access-validity:10800000 }")
    private  long ACCESS_TOKEN_VALIDITY;
    @Value("${jwt.token.refresh-validity:604800000}")
    private  long REFRESH_TOKEN_VALIDITY;
    @PostConstruct
    public void init() {
        SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Get username from token (subject claim)
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Generic claim resolver
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Parse and validate token, return claims
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validate token signature and expiration (fast version)
    public boolean isValidToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return isNotExpired(claims.getExpiration());
        } catch (JwtException | IllegalArgumentException e) {
            return false;  // Invalid token
        }
    }

    // Validate token with additional username check
    public boolean isValidToken(String token, UserDetails userDetails) {
        if (!isValidToken(token)) return false;
        String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername());
    }

    // Check expiration with boundary-safe comparison
    private boolean isNotExpired(Date expiration) {
        return expiration != null &&
                expiration.getTime() > System.currentTimeMillis();
    }

    private String buildToken(String subject, long validity, Map<String, Object> customClaims) {
        Map<String, Object> claims = customClaims != null ? new HashMap<>(customClaims) : new HashMap<>();

        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(ISSUER)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))  // 过期时间
                .addClaims(claims)
                .signWith(SECRET_KEY)
                .compact();
    }


    private String buildToken(String subject, long validity) {
        return buildToken(subject, validity, null);
    }


    public String generateAccessToken(String userIdentifier, Map<String, Object> customClaims) {
        return buildToken(userIdentifier, ACCESS_TOKEN_VALIDITY, customClaims);
    }


    public String generateAccessToken(String userIdentifier) {
        return generateAccessToken(userIdentifier, null);
    }


    public String generateRefreshToken(String userIdentifier, Map<String, Object> customClaims) {
        return buildToken(userIdentifier, REFRESH_TOKEN_VALIDITY, customClaims);
    }


    public String generateRefreshToken(String userIdentifier) {
        return generateRefreshToken(userIdentifier, null);
    }


    public String generateAccessToken(String userIdentifier, long validDuration, Map<String, Object> claims) {
        return buildToken(userIdentifier, validDuration, claims);
    }


    public String generateRefreshToken(String userIdentifier, long validDuration, Map<String, Object> claims) {
        return buildToken(userIdentifier, validDuration, claims);
    }

    public <T> T getClaimValue(String token, String claimKey, Class<T> clazz) {
        try {
            if (!isValidToken(token)) {
                return null;
            }

            Claims claims = getAllClaimsFromToken(token);
            return claims.get(claimKey, clazz);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }


    public Object getRawClaimValue(String token, String claimKey) {
        return getClaimValue(token, claimKey, Object.class);
    }

    public String extractAccessTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader(ACCESS_TOKEN_HEADER);

        if (token == null){
            token = request.getParameter(ACCESS_TOKEN);
        }
        if (token == null){
            token = request.getHeader("Authorization");
        }
        return token;
    }
}