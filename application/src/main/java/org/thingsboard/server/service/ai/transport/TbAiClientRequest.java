// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TbAiClientRequest(String clientOrigin, Map<String, String> forwardedHeaders) {

    private static final List<String> FORWARDED_HEADERS = List.of(
            HttpHeaders.HOST, "X-Forwarded-Host", "X-Forwarded-Proto", "X-Forwarded-Port", "X-Forwarded-For");

    public static TbAiClientRequest of(String clientOrigin, HttpServletRequest request) {
        var headers = new LinkedHashMap<String, String>();
        for (String name : FORWARDED_HEADERS) {
            String value = request.getHeader(name);
            if (value != null) {
                headers.put(name, value);
            }
        }
        return new TbAiClientRequest(clientOrigin, Map.copyOf(headers));
    }

}
