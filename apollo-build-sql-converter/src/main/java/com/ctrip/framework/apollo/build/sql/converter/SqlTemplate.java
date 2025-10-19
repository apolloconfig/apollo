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
package com.ctrip.framework.apollo.build.sql.converter;

import freemarker.template.Template;

public class SqlTemplate {

  private final String srcPath;

  private final Template template;

  public SqlTemplate(String srcPath, Template template) {
    this.srcPath = srcPath;
    this.template = template;
  }

  public String getSrcPath() {
    return this.srcPath;
  }

  public Template getTemplate() {
    return this.template;
  }
}
