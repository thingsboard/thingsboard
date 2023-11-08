/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MediaTypeUtils {

    private static final Map<String, String> mappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg"
    );

    public static String mediaTypeToFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mappings.getOrDefault(subtype, subtype);
    }

    public static String fileExtensionToMediaType(String type, String extension) {
        String subtype = mappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType(type, subtype).toString();
    }

}
