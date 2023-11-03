package org.thingsboard.server.common.data.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MediaTypeUtils {

    private static final Map<String, String> mappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg"
    );

    public static String getFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mappings.getOrDefault(subtype, subtype);
    }

}
