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
