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
package com.ctrip.framework.apollo.portal.controller;

import com.google.common.base.Strings;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrefixPathController {

  private final ServletContext servletContext;

  // We suggest users use server.servlet.context-path to configure the prefix path instead
  @Deprecated
  @Value("${prefix.path:}")
  private String prefixPath;

  public PrefixPathController(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @GetMapping("/prefix-path")
  public String getPrefixPath() {
    if (Strings.isNullOrEmpty(prefixPath)) {
      return servletContext.getContextPath();
    }
    return prefixPath;
  }

}
