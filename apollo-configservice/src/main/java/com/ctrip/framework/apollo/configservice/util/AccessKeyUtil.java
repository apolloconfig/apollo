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
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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
      appId = extractAppIdFromConfigFileRequest(servletPath);
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

  private String extractAppIdFromConfigFileRequest(String servletPath) {
    String remainingPath = StringUtils.removeStart(servletPath, URL_CONFIGFILES_PREFIX);
    String[] pathSegments = StringUtils.split(remainingPath, URL_SEPARATOR);

    if (pathSegments == null || pathSegments.length < GENERIC_CONFIG_FILE_PATH_SEGMENT_COUNT) {
      return null;
    }

    // "/configfiles/raw/default/application" hits the generic mapping with appId "raw",
    // while "/configfiles/raw/someAppId/default/application" hits the dedicated raw endpoint.
    if (pathSegments.length == SPECIAL_CONFIG_FILE_PATH_SEGMENT_COUNT
        && isConfigFileFormatPathSegment(pathSegments[0])) {
      return pathSegments[1];
    }

    return pathSegments[0];
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
