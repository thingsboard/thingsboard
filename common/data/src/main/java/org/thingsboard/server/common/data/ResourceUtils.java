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
package org.thingsboard.server.common.data;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Slf4j
public class ResourceUtils {

    public static boolean resourceExists(Object classLoaderSource, String filePath) {
        return resourceExists(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static boolean resourceExists(ClassLoader classLoader, String filePath) {
        File resourceFile = new File(filePath);
        if (resourceFile.exists()) {
            return true;
        } else {
            InputStream classPathStream = classLoader.getResourceAsStream(filePath);
            if (classPathStream != null) {
                return true;
            } else {
                try {
                    URL url = Resources.getResource(filePath);
                    if (url != null) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {}
            }
        }
        return false;
    }

    public static InputStream getInputStream(Object classLoaderSource, String filePath) {
        return getInputStream(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static InputStream getInputStream(ClassLoader classLoader, String filePath) {
        try {
            InputStream keyStoreInputStream;
            File keyStoreFile = new File(filePath);
            if (keyStoreFile.exists()) {
                log.info("Reading key store from file {}", filePath);
                keyStoreInputStream = new FileInputStream(keyStoreFile);
            } else {
                InputStream classPathStream = classLoader.getResourceAsStream(filePath);
                if (classPathStream != null) {
                    log.info("Reading key store from class path {}", filePath);
                    keyStoreInputStream = classPathStream;
                } else {
                    URI uri = Resources.getResource(filePath).toURI();
                    log.info("Reading key store from URI {}", filePath);
                    keyStoreInputStream = new FileInputStream(new File(uri));
                }
            }
            return keyStoreInputStream;
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }

    public static String getUri(Object classLoaderSource, String filePath) {
        return getUri(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static String getUri(ClassLoader classLoader, String filePath) {
        try {
            File keyStoreFile = new File(filePath);
            if (keyStoreFile.exists()) {
                log.info("Reading key store from file {}", filePath);
                return keyStoreFile.getAbsolutePath();
            } else {
                URL url = classLoader.getResource(filePath);
                return url.toURI().toString();
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }
}
