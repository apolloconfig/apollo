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
package com.ctrip.framework.apollo.common.controller;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.Instant;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;

import java.util.List;

/**
 * Created by Jason on 5/11/16.
 */
@Configuration
public class HttpMessageConverterConfiguration {
  @Bean
  public HttpMessageConverters messageConverters() {
    // Custom Gson TypeAdapter for Instant
    JsonSerializer<Instant> instantJsonSerializer = (src, typeOfSrc, context) ->
        src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()); // Serialize Instant as ISO-8601 string

    JsonDeserializer<Instant> instantJsonDeserializer = (json, typeOfT, context) -> {
      if (json == null || json.isJsonNull()) {
        return null;
      }
      return Instant.parse(json.getAsString()); // Deserialize from ISO-8601 string
    };

    GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
    gsonHttpMessageConverter.setGson(
            new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .registerTypeAdapter(Instant.class, instantJsonSerializer)
                .registerTypeAdapter(Instant.class, instantJsonDeserializer)
                .create());
    final List<HttpMessageConverter<?>> converters = Lists.newArrayList(
            new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
            new AllEncompassingFormHttpMessageConverter(), gsonHttpMessageConverter);
    return new HttpMessageConverters() {
      @Override
      public List<HttpMessageConverter<?>> getConverters() {
        return converters;
      }
    };
  }
}
