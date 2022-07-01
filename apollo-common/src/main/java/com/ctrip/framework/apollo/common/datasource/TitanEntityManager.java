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
package com.ctrip.framework.apollo.common.datasource;

import com.ctrip.framework.apollo.tracer.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Method;

@Component
@Conditional(TitanCondition.class)
public class TitanEntityManager {

  private final TitanSettings settings;

  public TitanEntityManager(final TitanSettings settings) {
    this.settings = settings;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  public DataSource datasource() throws Exception {
    Class clazz = Class.forName("com.ctrip.datasource.configure.DalDataSourceFactory");
    Object obj = clazz.newInstance();
    Method method = clazz.getMethod("createDataSource", String.class, String.class);
    DataSource ds = ((DataSource) method.invoke(obj,
        new Object[] {settings.getTitanDbname(), settings.getTitanUrl()}));
    Tracer.logEvent("Apollo.Datasource.Titan", settings.getTitanDbname());
    return ds;
  }

}
