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
package com.ctrip.framework.apollo.portal.component.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;

/**
 * Spring-session JSON serialization mode configuration
 *
 * @author kl (http://kailing.pub)
 * @since 2022/7/26
 */
@Configuration
public class SpringSessionConfig implements BeanClassLoaderAware {

  private ClassLoader loader;

  @Bean("springSessionConversionService")
  @ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "jdbc")
  public ConversionService springSessionConversionService() {
    GenericConversionService conversionService = new GenericConversionService();
    ObjectMapper objectMapper = this.objectMapper();
    conversionService.addConverter(Object.class, byte[].class, source -> {
      try {
        return objectMapper.writeValueAsBytes(source);
      } catch (IOException e) {
        throw new RuntimeException(
            "Spring-session JSON serializing error, This is usually caused by the system upgrade, please clear the browser cookies and try again.",
            e);
      }
    });

    conversionService.addConverter(byte[].class, Object.class, source -> {
      try {
        return objectMapper.readValue(source, Object.class);
      } catch (IOException e) {
        throw new RuntimeException(
            "Spring-session JSON deserializing error, This is usually caused by the system upgrade, please clear the browser cookies and try again.",
            e);
      }
    });
    return conversionService;
  }

  @Bean("springSessionDefaultRedisSerializer")
  @ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "redis")
  public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
    return new GenericJackson2JsonRedisSerializer(objectMapper());
  }

  /**
   * Customized {@link ObjectMapper} to add mix-in for class that doesn't have default constructors
   *
   * @return the {@link ObjectMapper} to use
   */
  private ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
    return mapper;
  }

  /*
   * @see
   * org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang
   * .ClassLoader)
   */
  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.loader = classLoader;
  }
}
