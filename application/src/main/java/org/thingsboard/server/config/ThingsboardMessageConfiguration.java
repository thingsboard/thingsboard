/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Configuration
public class ThingsboardMessageConfiguration {

    @Bean
    @Primary
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:/templates/";

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Bean
    public VelocityEngine velocityEngine() {
        VelocityEngine velocityEngine = new VelocityEngine();
        try {
            Resource resource = resourceLoader.getResource(DEFAULT_RESOURCE_LOADER_PATH);
            File file = resource.getFile();
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
            velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
            velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, file.getAbsolutePath());
        } catch (IOException e) {
            initSpringResourceLoader(velocityEngine, DEFAULT_RESOURCE_LOADER_PATH);
        }
        velocityEngine.init();
        return velocityEngine;
    }

    private void initSpringResourceLoader(VelocityEngine velocityEngine, String resourceLoaderPath) {
        velocityEngine.setProperty(
                RuntimeConstants.RESOURCE_LOADER, SpringResourceLoader.NAME);
        velocityEngine.setProperty(
                SpringResourceLoader.SPRING_RESOURCE_LOADER_CLASS, SpringResourceLoader.class.getName());
        velocityEngine.setProperty(
                SpringResourceLoader.SPRING_RESOURCE_LOADER_CACHE, "true");
        velocityEngine.setApplicationAttribute(
                SpringResourceLoader.SPRING_RESOURCE_LOADER, resourceLoader);
        velocityEngine.setApplicationAttribute(
                SpringResourceLoader.SPRING_RESOURCE_LOADER_PATH, resourceLoaderPath);
    }

    @Slf4j
    static class SpringResourceLoader extends org.apache.velocity.runtime.resource.loader.ResourceLoader {

        public static final String NAME = "spring";

        public static final String SPRING_RESOURCE_LOADER_CLASS = "spring.resource.loader.class";

        public static final String SPRING_RESOURCE_LOADER_CACHE = "spring.resource.loader.cache";

        public static final String SPRING_RESOURCE_LOADER = "spring.resource.loader";

        public static final String SPRING_RESOURCE_LOADER_PATH = "spring.resource.loader.path";

        private org.springframework.core.io.ResourceLoader resourceLoader;

        private String[] resourceLoaderPaths;


        @Override
        public void init(ExtendedProperties configuration) {
            this.resourceLoader = (org.springframework.core.io.ResourceLoader)
                    this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER);
            String resourceLoaderPath = (String) this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER_PATH);
            if (this.resourceLoader == null) {
                throw new IllegalArgumentException(
                        "'resourceLoader' application attribute must be present for SpringResourceLoader");
            }
            if (resourceLoaderPath == null) {
                throw new IllegalArgumentException(
                        "'resourceLoaderPath' application attribute must be present for SpringResourceLoader");
            }
            this.resourceLoaderPaths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
            for (int i = 0; i < this.resourceLoaderPaths.length; i++) {
                String path = this.resourceLoaderPaths[i];
                if (!path.endsWith("/")) {
                    this.resourceLoaderPaths[i] = path + "/";
                }
            }
            if (log.isInfoEnabled()) {
                log.info("SpringResourceLoader for Velocity: using resource loader [" + this.resourceLoader +
                        "] and resource loader paths " + Arrays.asList(this.resourceLoaderPaths));
            }
        }

        @Override
        public InputStream getResourceStream(String source) throws ResourceNotFoundException {
            if (log.isDebugEnabled()) {
                log.debug("Looking for Velocity resource with name [" + source + "]");
            }
            for (String resourceLoaderPath : this.resourceLoaderPaths) {
                org.springframework.core.io.Resource resource =
                        this.resourceLoader.getResource(resourceLoaderPath + source);
                try {
                    return resource.getInputStream();
                }
                catch (IOException ex) {
                    if (log.isDebugEnabled()) {
                        log.debug("Could not find Velocity resource: " + resource);
                    }
                }
            }
            throw new ResourceNotFoundException(
                    "Could not find resource [" + source + "] in Spring resource loader path");
        }

        @Override
        public boolean isSourceModified(org.apache.velocity.runtime.resource.Resource resource) {
            return false;
        }

        @Override
        public long getLastModified(org.apache.velocity.runtime.resource.Resource resource) {
            return 0;
        }

    }
}
