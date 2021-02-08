/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yyh on 2017/5/2.
 * CORS configuration
 */
@Configuration
@ConfigurationProperties(prefix = "spring.mvc.cors")
public class MvcCorsProperties {

    private Map<String, CorsConfiguration> mappings = new HashMap<>();

    public MvcCorsProperties() {
        super();
    }

    public Map<String, CorsConfiguration> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, CorsConfiguration> mappings) {
        this.mappings = mappings;
    }
}
