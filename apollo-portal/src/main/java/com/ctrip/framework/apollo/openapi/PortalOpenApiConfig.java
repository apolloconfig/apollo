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
package com.ctrip.framework.apollo.openapi;

import com.ctrip.framework.apollo.common.controller.WebMvcConfig;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@EnableAutoConfiguration
@Configuration
@ComponentScan(basePackageClasses = PortalOpenApiConfig.class)
public class PortalOpenApiConfig {

	@Component
	static class PortalWebMvcConfig extends WebMvcConfig {
		@Override
		public void customize(TomcatServletWebServerFactory factory) {
			final String relaxedChars = "<>[\\]^`{|}";
			final String tomcatRelaxedPathCharsProperty = "relaxedPathChars";
			final String tomcatRelaxedQueryCharsProperty = "relaxedQueryChars";
			factory.addConnectorCustomizers(connector -> {
				connector.setProperty(tomcatRelaxedPathCharsProperty, relaxedChars);
				connector.setProperty(tomcatRelaxedQueryCharsProperty, relaxedChars);
			});
		}
	}
}
