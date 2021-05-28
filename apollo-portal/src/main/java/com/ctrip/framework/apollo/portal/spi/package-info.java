/*
 * Copyright 2021 Apollo Authors
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
/**
 * This package defines common interfaces so that each company could provide their own implementations.<br/>
 * Currently we provide 2 implementations: Ctrip and Default.<br/>
 * Ctrip implementation will be activated only when spring.profiles.active = ctrip.
 * So if spring.profiles.active is not ctrip, the default implementation will be activated.
 * You may refer com.ctrip.framework.apollo.portal.spi.configuration.AuthConfiguration when providing your own implementation.
 *
 * @see com.ctrip.framework.apollo.portal.spi.configuration.AuthConfiguration
 */
package com.ctrip.framework.apollo.portal.spi;
