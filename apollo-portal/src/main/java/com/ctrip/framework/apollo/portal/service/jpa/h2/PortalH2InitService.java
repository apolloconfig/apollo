/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.portal.service.jpa.h2;

import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
@Profile("h2")
public class PortalH2InitService {

    @Autowired
    private PortalDBPropertySource dbPropertySource;

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void runSqlScript() throws Exception {
        Resource resource = new ClassPathResource("jpa/init.h2.sql");
        DatabasePopulatorUtils.execute(new ResourceDatabasePopulator(resource), dataSource);
        dbPropertySource.refresh();
    }
}
