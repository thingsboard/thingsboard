// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.monitoring.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.thingsboard.common.util.JacksonUtil;

import java.io.InputStream;

public class ResourceUtils {

    @SneakyThrows
    public static <T> T getResource(String path, Class<T> type) {
        InputStream resource = getResourceStream(path);
        return JacksonUtil.OBJECT_MAPPER.readValue(resource, type);
    }

    @SneakyThrows
    public static JsonNode getResource(String path) {
        InputStream resource = getResourceStream(path);
        return JacksonUtil.OBJECT_MAPPER.readTree(resource);
    }

    @SneakyThrows
    public static byte[] getResourceBytes(String path) {
        try (InputStream resource = getResourceStream(path)) {
            return resource.readAllBytes();
        }
    }

    private static InputStream getResourceStream(String path) {
        InputStream resource = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found for path " + path);
        }
        return resource;
    }

}
