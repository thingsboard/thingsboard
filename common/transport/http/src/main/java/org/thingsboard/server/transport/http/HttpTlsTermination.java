// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.http;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Where TLS of a device HTTP request is terminated, used for transport usage statistics only.
 * Forwarded headers are trusted here because the result is never used for any security decision.
 */
public enum HttpTlsTermination {

    NONE,
    TRANSPORT,
    PROXY;

    static final String CIPHER_SUITE_ATTRIBUTE = "jakarta.servlet.request.cipher_suite";

    public static HttpTlsTermination resolve(HttpServletRequest request) {
        // Set by the servlet container only for the TLS connection it terminates itself, survives forwarded-headers request wrappers
        if (request.getAttribute(CIPHER_SUITE_ATTRIBUTE) != null) {
            return TRANSPORT;
        }
        // Secure without own TLS means the scheme was already resolved from forwarded headers (server.forward_headers_strategy)
        if (request.isSecure() || isForwardedHttps(request)) {
            return PROXY;
        }
        return NONE;
    }

    static boolean isForwardedHttps(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(firstValue(forwardedProto));
        }
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null) {
            for (String pair : firstValue(forwarded).split(";")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "proto".equalsIgnoreCase(kv[0].trim())) {
                    return "https".equalsIgnoreCase(kv[1].trim().replace("\"", ""));
                }
            }
        }
        return "on".equalsIgnoreCase(request.getHeader("X-Forwarded-Ssl"));
    }

    // The leftmost value is the one set by the proxy closest to the client
    private static String firstValue(String header) {
        int comma = header.indexOf(',');
        return (comma >= 0 ? header.substring(0, comma) : header).trim();
    }

}
