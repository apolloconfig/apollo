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
package com.ctrip.framework.apollo.configservice.util;

import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.configservice.service.AccessKeyServiceWithCache;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.google.common.base.Strings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * @author nisiyong
 */
@Component
public class AccessKeyUtil {

  private static final String URL_SEPARATOR = "/";
  private static final String URL_CONFIGS_PREFIX = "/configs/";
  private static final String URL_CONFIGFILES_PREFIX = "/configfiles/";
  private static final String URL_NOTIFICATIONS_PREFIX = "/notifications";
  private static final int GENERIC_CONFIG_FILE_PATH_SEGMENT_COUNT = 3;
  private static final int SPECIAL_CONFIG_FILE_PATH_SEGMENT_COUNT = 4;
  private static final String CONFIG_FILE_OUTPUT_FORMAT_JSON = "json";
  private static final String CONFIG_FILE_OUTPUT_FORMAT_RAW = "raw";

  private final AccessKeyServiceWithCache accessKeyServiceWithCache;

  public AccessKeyUtil(AccessKeyServiceWithCache accessKeyServiceWithCache) {
    this.accessKeyServiceWithCache = accessKeyServiceWithCache;
  }

  public List<String> findAvailableSecret(String appId) {
    return accessKeyServiceWithCache.getAvailableSecrets(appId);
  }

  public List<String> findObservableSecrets(String appId) {
    return accessKeyServiceWithCache.getObservableSecrets(appId);
  }

  public String extractAppIdFromRequest(HttpServletRequest request) {
    String appId = null;
    String servletPath = request.getServletPath();

    if (StringUtils.startsWith(servletPath, URL_CONFIGS_PREFIX)) {
      appId = StringUtils.substringBetween(servletPath, URL_CONFIGS_PREFIX, URL_SEPARATOR);
    } else if (StringUtils.startsWith(servletPath, URL_CONFIGFILES_PREFIX)) {
      appId = extractAppIdFromConfigFileRequest(request);
    } else if (isNotificationRequest(servletPath)) {
      appId = request.getParameter("appId");
    }

    return validateAppId(appId);
  }

  public String buildSignature(String path, String query, String timestampString, String secret) {
    String pathWithQuery = path;
    if (!Strings.isNullOrEmpty(query)) {
      pathWithQuery += "?" + query;
    }

    return Signature.signature(timestampString, pathWithQuery, secret);
  }

  private String validateAppId(String appId) {
    if (!InputValidator.isValidClusterNamespace(appId)) {
      return null;
    }
    return appId;
  }

  /**
   * Extract the appId from a {@code /configfiles/} request.
   *
   * <p>GHSA-v8jm: the appId must be derived from the same path Spring MVC binds the
   * route variables from, otherwise the filter can check the AccessKey of a
   * different app than the one {@code ConfigFileController} serves. Spring MVC
   * builds its lookup path with {@code UrlPathHelper}: it strips {@code ;} matrix
   * content from the raw path and then percent-decodes it, so the three
   * config-file routes are matched here against the path normalized the same way:
   * strip matrix params first, then decode the whole path, then split into
   * segments. Matching on the raw text instead lets an encoded format word
   * (e.g. {@code %6a%73%6f%6e} for {@code json}) or a matrix-suffixed one
   * (e.g. {@code json;v=1}) slip past the format-word check while MVC binds the
   * real appId.
   *
   * <p>The routes are matched exactly, the way the controller declares them:
   * {@code /{appId}/{clusterName}/{namespace:.+}} (3 segments) and
   * {@code /json/{appId}/{clusterName}/{namespace:.+}} /
   * {@code /raw/{appId}/{clusterName}/{namespace:.+}} (4 segments, format word
   * first). {@code {namespace:.+}} never spans a {@code /}, so any other shape
   * matches no route and is rejected (the filter turns a null appId into
   * 400 InvalidAppId) before any key is looked up. A cluster segment that
   * decodes to {@code .} or {@code ..} is never a legitimate cluster (the
   * default cluster is named {@code default}), so it is rejected as well.
   *
   * <p>An encoded separator ({@code %2F} or {@code %5C}) is rejected before
   * decoding: Spring MVC's {@code PathPatternParser} preserves segment
   * boundaries during decoding, so a separator that only appears after
   * decoding could let the filter and MVC split the path into different
   * segments. A legitimate appId, cluster or namespace never contains a
   * separator.
   *
   * <p>The {@code getRequestURI()} fallback to the servlet path only applies when
   * the raw URI is blank, which does not happen on a real servlet container; it
   * exists so callers that only mock the servlet path (existing unit tests) keep
   * working.
   */
  private String extractAppIdFromConfigFileRequest(HttpServletRequest request) {
    String rawPath = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (StringUtils.isNotBlank(rawPath) && StringUtils.isNotBlank(contextPath)
        && rawPath.startsWith(contextPath)) {
      rawPath = rawPath.substring(contextPath.length());
    }
    if (StringUtils.isBlank(rawPath)) {
      rawPath = request.getServletPath();
    }
    if (StringUtils.isBlank(rawPath)) {
      return null;
    }

    String strippedPath = stripMatrixParams(rawPath);
    if (hasEncodedSeparator(strippedPath)) {
      return null;
    }

    String decodedPath = decodePath(strippedPath);
    if (decodedPath == null || !StringUtils.startsWith(decodedPath, URL_CONFIGFILES_PREFIX)) {
      return null;
    }

    String remainingPath = decodedPath.substring(URL_CONFIGFILES_PREFIX.length());
    // Keep empty segments so the segment count matches MVC exactly: a trailing
    // slash or an empty segment means no route matches ({namespace:.+} needs at
    // least one character, and a variable never matches an empty segment).
    String[] pathSegments = remainingPath.split(URL_SEPARATOR, -1);

    String appId;
    String clusterName;
    String namespace;
    if (pathSegments.length == SPECIAL_CONFIG_FILE_PATH_SEGMENT_COUNT
        && isConfigFileFormatPathSegment(pathSegments[0])) {
      appId = pathSegments[1];
      clusterName = pathSegments[2];
      namespace = pathSegments[3];
    } else if (pathSegments.length == GENERIC_CONFIG_FILE_PATH_SEGMENT_COUNT) {
      appId = pathSegments[0];
      clusterName = pathSegments[1];
      namespace = pathSegments[2];
    } else {
      return null;
    }

    if (StringUtils.isBlank(appId) || StringUtils.isBlank(clusterName)
        || StringUtils.isBlank(namespace) || isDotSegment(clusterName)) {
      return null;
    }

    return validateAppId(appId);
  }

  /**
   * Strip {@code ;} matrix content from every raw path segment, mirroring
   * {@code UrlPathHelper}: it removes semicolon content from the raw (still
   * encoded) path before decoding, so an encoded semicolon ({@code %3B}) is
   * decoded afterwards and is NOT treated as a matrix separator.
   */
  private String stripMatrixParams(String rawPath) {
    String[] segments = rawPath.split(URL_SEPARATOR, -1);
    StringBuilder stripped = new StringBuilder(rawPath.length());
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        stripped.append(URL_SEPARATOR);
      }
      String segment = segments[i];
      int semicolon = segment.indexOf(';');
      stripped.append(semicolon >= 0 ? segment.substring(0, semicolon) : segment);
    }
    return stripped.toString();
  }

  /**
   * Whether the raw (still encoded) path contains an encoded separator
   * ({@code %2F} or {@code %5C}). The check runs on the matrix-stripped raw
   * path, so a {@code %2F} inside matrix params (which MVC strips anyway)
   * does not cause a false rejection, and a double-encoded {@code %252F} is
   * left alone: it decodes to the literal text {@code %2F}, which is not a
   * separator for MVC either.
   */
  private boolean hasEncodedSeparator(String rawPath) {
    return rawPath.contains("%2f") || rawPath.contains("%2F") || rawPath.contains("%5c")
        || rawPath.contains("%5C");
  }

  /**
   * Percent-decode a path the way {@code UrlPathHelper} does: {@code %XX} sequences
   * are decoded (including {@code %2F} to {@code /}), while {@code +} stays a
   * literal plus (unlike {@link java.net.URLDecoder}, which is for form data).
   */
  private String decodePath(String path) {
    try {
      return UriUtils.decode(path, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Whether a decoded cluster segment is a dot segment. The part before the first
   * {@code ;} is checked as well, so a matrix-suffixed dot (e.g. {@code .;x})
   * is rejected too.
   */
  private boolean isDotSegment(String segment) {
    String base = segment.split(";", 2)[0];
    return ".".equals(base) || "..".equals(base);
  }

  private boolean isConfigFileFormatPathSegment(String pathSegment) {
    return StringUtils.equals(pathSegment, CONFIG_FILE_OUTPUT_FORMAT_JSON)
        || StringUtils.equals(pathSegment, CONFIG_FILE_OUTPUT_FORMAT_RAW);
  }

  private boolean isNotificationRequest(String servletPath) {
    return StringUtils.equals(servletPath, URL_NOTIFICATIONS_PREFIX)
        || StringUtils.startsWith(servletPath, URL_NOTIFICATIONS_PREFIX + URL_SEPARATOR);
  }
}
