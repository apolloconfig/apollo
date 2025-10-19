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

import java.util.StringJoiner;

public class SqlTemplateContext {

  /** sql gist */
  private final SqlTemplateGist gists;

  SqlTemplateContext(Builder builder) {
    this.gists = builder.gists;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.gists = this.gists;
    return builder;
  }

  public SqlTemplateGist getGists() {
    return this.gists;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SqlTemplateContext.class.getSimpleName() + "[", "]")
        // fields
        .add("gists=" + this.gists).toString();
  }

  public static final class Builder {

    private SqlTemplateGist gists;

    Builder() {}

    public Builder gists(SqlTemplateGist gists) {
      this.gists = gists;
      return this;
    }

    public SqlTemplateContext build() {
      return new SqlTemplateContext(this);
    }
  }
}
