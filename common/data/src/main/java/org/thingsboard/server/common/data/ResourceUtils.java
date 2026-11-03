// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    public static final String CLASSPATH_URL_PREFIX = "classpath:";

    public static boolean resourceExists(Object classLoaderSource, String filePath) {
        return resourceExists(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static boolean resourceExists(ClassLoader classLoader, String filePath) {
        boolean classPathResource = false;
        String path = filePath;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            path = path.substring(CLASSPATH_URL_PREFIX.length());
            classPathResource = true;
        }
        if (!classPathResource) {
            File resourceFile = new File(path);
            if (resourceFile.exists()) {
                return true;
            }
        }
        InputStream classPathStream = classLoader.getResourceAsStream(path);
        if (classPathStream != null) {
            return true;
        } else {
            try {
                Resources.getResource(path);
                return true;
            } catch (IllegalArgumentException ignored) {}
        }
        return false;
    }

    public static InputStream getInputStream(Object classLoaderSource, String filePath) {
        return getInputStream(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static InputStream getInputStream(ClassLoader classLoader, String filePath) {
        boolean classPathResource = false;
        String path = filePath;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            path = path.substring(CLASSPATH_URL_PREFIX.length());
            classPathResource = true;
        }
        try {
            if (!classPathResource) {
                File resourceFile = new File(path);
                if (resourceFile.exists()) {
                    log.info("Reading resource data from file {}", filePath);
                    return new FileInputStream(resourceFile);
                }
            }
            InputStream classPathStream = classLoader.getResourceAsStream(path);
            if (classPathStream != null) {
                log.info("Reading resource data from class path {}", filePath);
                return classPathStream;
            } else {
                URL url = Resources.getResource(path);
                if (url != null) {
                    URI uri = url.toURI();
                    log.info("Reading resource data from URI {}", filePath);
                    return new FileInputStream(new File(uri));
                }
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: {}", filePath);
            } else {
                log.warn("Unable to find resource: {}", filePath, e);
            }
        }
        throw new RuntimeException("Unable to find resource: " + filePath);
    }

    public static String getUri(Object classLoaderSource, String filePath) {
        return getUri(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static String getUri(ClassLoader classLoader, String filePath) {
        try {
            File resourceFile = new File(filePath);
            if (resourceFile.exists()) {
                log.info("Reading resource data from file {}", filePath);
                return resourceFile.getAbsolutePath();
            } else {
                URL url = classLoader.getResource(filePath);
                if (url == null) {
                    throw new RuntimeException("Unable to find resource: " + filePath);
                }
                return url.toURI().toString();
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: {}", filePath);
            } else {
                log.warn("Unable to find resource: {}", filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }

}
