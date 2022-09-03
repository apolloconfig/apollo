/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.client.url;

import com.ctrip.framework.apollo.common.utils.UrlUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenApiPathBuilder {

  private static final String ENV_PATH = "env";
  private static final String ENVS_PATH = "envs";
  private static final String APPS_PATH = "apps";
  private static final String CLUSTERS_PATH = "clusters";
  private static final String NAMESPACES_PATH = "namespaces";
  private static final String ITEMS_PATH = "items";
  private static final String ENCODE_ITEMS_PATH = "encodeItems";
  private static final String RELEASE_PATH = "releases";

  private final static List<String> SORTED_PATH_KEYS = Arrays.asList(ENVS_PATH, ENV_PATH, APPS_PATH,
      CLUSTERS_PATH,
      NAMESPACES_PATH, ITEMS_PATH, ENCODE_ITEMS_PATH, RELEASE_PATH);

  private static final Escaper PATH_ESCAPER = UrlEscapers.urlPathSegmentEscaper();
  private static final Escaper QUERY_PARAM_ESCAPER = UrlEscapers.urlFormParameterEscaper();
  private static final Joiner PATH_JOIN = Joiner.on("/");

  private final Map<String, String> pathVariable;
  private final Map<String, String> params;

  private String customResource;

  public static OpenApiPathBuilder newBuilder() {
    return new OpenApiPathBuilder();
  }

  private OpenApiPathBuilder() {
    this.pathVariable = new HashMap<>();
    this.params = new HashMap<>();
  }

  public OpenApiPathBuilder envPathVal(String env) {
    pathVariable.put(ENV_PATH, escapePath(env));
    return this;
  }

  public OpenApiPathBuilder envsPathVal(String envs) {
    pathVariable.put(ENVS_PATH, escapePath(envs));
    return this;
  }

  public OpenApiPathBuilder appsPathVal(String apps) {
    pathVariable.put(APPS_PATH, escapePath(apps));
    return this;
  }

  public OpenApiPathBuilder clustersPathVal(String clusters) {
    pathVariable.put(CLUSTERS_PATH, escapePath(clusters));
    return this;
  }

  public OpenApiPathBuilder namespacesPathVal(String namespaces) {
    pathVariable.put(NAMESPACES_PATH, escapePath(namespaces));
    return this;
  }

  public OpenApiPathBuilder itemsPathVal(String items) {
    if (UrlUtils.hasIllegalChar(items)) {
      pathVariable.put(ENCODE_ITEMS_PATH, UrlUtils.encode(items));
    } else {
      pathVariable.put(ITEMS_PATH, escapePath(items));
    }

    return this;
  }

  public OpenApiPathBuilder releasesPathVal(String releases) {
    pathVariable.put(RELEASE_PATH, escapePath(releases));
    return this;
  }

  public OpenApiPathBuilder customResource(String customResource) {
    this.customResource = customResource;
    return this;
  }

  public OpenApiPathBuilder addParam(String key, Object value) {
    if (Strings.isNullOrEmpty(key)) {
      throw new IllegalArgumentException("Param key should not be null or empty");
    }
    this.params.put(key, escapeParam(String.valueOf(value)));
    return this;
  }

  public String buildPath(String baseUrl) {
    if (Strings.isNullOrEmpty(baseUrl)) {
      throw new IllegalArgumentException("Base url should not be null or empty");
    }
    List<String> parts = new ArrayList<>();
    parts.add(baseUrl);

    for (String k : SORTED_PATH_KEYS) {
      if (pathVariable.containsKey(k)) {
        parts.add(k);
        String v = pathVariable.get(k);
        if (!Strings.isNullOrEmpty(v)) {
          parts.add(v);
        }
      }
    }

    if (!Strings.isNullOrEmpty(this.customResource)) {
      parts.add(this.customResource);
    }

    String path = PATH_JOIN.join(parts);

    if (!params.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> kv : params.entrySet()) {
        if (sb.length() > 0) {
          sb.append("&");
        }
        sb.append(kv.getKey()).append("=").append(kv.getValue());
      }
      path += "?" + sb;
    }
    return path;
  }

  protected String escapePath(String path) {
    return PATH_ESCAPER.escape(path);
  }

  protected String escapeParam(String param) {
    return QUERY_PARAM_ESCAPER.escape(param);
  }

}
