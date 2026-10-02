// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @param exactOrigin true when {@code clientOrigin} comes from the browser request: TB AI shows it to the user (device
 *                    connectivity commands, audit), so the call needs a channel opened with exactly this origin. False
 *                    for a default origin, which any channel of the same user may serve.
 */
public record TbAiClientRequest(String clientOrigin, Map<String, String> forwardedHeaders, boolean exactOrigin) {

    public TbAiClientRequest(String clientOrigin, Map<String, String> forwardedHeaders) {
        this(clientOrigin, forwardedHeaders, true);
    }

    public static TbAiClientRequest withDefaultOrigin(String clientOrigin) {
        return new TbAiClientRequest(clientOrigin, Map.of(), false);
    }

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
