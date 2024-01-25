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
package com.ctrip.framework.apollo.biz.entity;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
class JpaMapFieldJsonConverter implements AttributeConverter<Map<String, String>, String> {

  private static final Gson GSON = new Gson();

  private static final TypeToken<HashMap<String, String>> TYPE_TOKEN = new TypeToken<HashMap<String, String>>() {
  };

  @SuppressWarnings("unchecked")
  private static final Type TYPE = TYPE_TOKEN.getType();

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    return GSON.toJson(attribute);
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    return GSON.fromJson(dbData, TYPE);
  }
}
