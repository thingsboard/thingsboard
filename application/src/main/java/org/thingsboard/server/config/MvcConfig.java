/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${spring.mvc.cache.enabled}")
    private Boolean cacheEnabled;
    @Value("${spring.mvc.cache.public}")
    private Boolean publicCache;
    @Value("${spring.mvc.cache.period}")
    private Integer period;

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
        "classpath:/static/",
        "classpath:/public/"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (cacheEnabled) {
            CacheControl cacheControl = CacheControl.maxAge(period, TimeUnit.SECONDS);
            if (publicCache) {
                cacheControl.cachePublic();
            } else {
                cacheControl.cachePrivate();
            }
            registry.addResourceHandler("/**")
                    .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS)
                    .setCacheControl(cacheControl);
        }
    }
}
