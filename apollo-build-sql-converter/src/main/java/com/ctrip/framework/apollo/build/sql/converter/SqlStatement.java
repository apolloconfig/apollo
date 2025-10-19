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

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class SqlStatement {

  private final String rawText;

  private final String singleLineText;

  private final List<String> textLines;

  SqlStatement(Builder builder) {
    this.rawText = builder.rawText;
    this.singleLineText = builder.singleLineText;
    this.textLines = builder.textLines;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.rawText = this.rawText;
    builder.singleLineText = this.singleLineText;
    builder.textLines = this.textLines;
    return builder;
  }

  public String getRawText() {
    return this.rawText;
  }

  public String getSingleLineText() {
    return this.singleLineText;
  }

  public List<String> getTextLines() {
    return this.textLines;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SqlStatement.class.getSimpleName() + "[", "]")
        // fields
        .add("rawText='" + this.rawText + "'").toString();
  }

  public static final class Builder {

    private String rawText;
    private String singleLineText;
    private List<String> textLines;

    Builder() {}

    public Builder rawText(String rawText) {
      this.rawText = rawText;
      return this;
    }

    public Builder singleLineText(String singleLineText) {
      this.singleLineText = singleLineText;
      return this;
    }

    public Builder textLines(List<String> textLines) {
      this.textLines = textLines == null ? null :
      // nonnull
          (textLines.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(textLines));
      return this;
    }

    public SqlStatement build() {
      return new SqlStatement(this);
    }
  }
}
