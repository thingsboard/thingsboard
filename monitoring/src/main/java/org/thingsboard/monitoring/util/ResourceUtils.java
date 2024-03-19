/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.monitoring.util;

import lombok.SneakyThrows;
import org.thingsboard.common.util.JacksonUtil;

import java.io.InputStream;

public class ResourceUtils {

    @SneakyThrows
    public static <T> T getResource(String path, Class<T> type) {
        InputStream resource = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found for path " + path);
        }
        return JacksonUtil.OBJECT_MAPPER.readValue(resource, type);
    }

    public static InputStream getResourceAsStream(String path) {
        InputStream resource = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found for path " + path);
        }
        return resource;
    }

}
