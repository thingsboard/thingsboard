/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.rule.engine.util;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DigestAuthUtil {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("(\\w+)\\s*=\\s*(?:\"([^\"]*)\"|([^,\\s]+))");

    private DigestAuthUtil() {}

    // RFC 7616: request-target is path(+query) only
    public static String requestUriForDigest(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) path = "/";  // ensure at least "/"
        String query = uri.getRawQuery();
        return (query == null || query.isEmpty()) ? path : (path + "?" + query);
    }

    public static Map<String, String> parseWwwAuthenticate(String header) {
        // "Digest realm="...", nonce="...", qop="auth,auth-int", opaque="...", algorithm=MD5"
        if (header == null) return Collections.emptyMap();
        int i = header.indexOf("Digest");
        if (i < 0) return Collections.emptyMap();
        String params = header.substring(i + "Digest".length()).trim();

        Map<String, String> map = new HashMap<>();
        Matcher m = TOKEN_PATTERN.matcher(params);
        while (m.find()) {
            String k = m.group(1);
            String v = m.group(2) != null ? m.group(2) : m.group(3);
            map.put(k.toLowerCase(Locale.ROOT), v);
        }
        return map;
    }

    public static String buildDigestAuthHeader(
            String username,
            String password,
            String method,
            String requestUri,             // path + query
            String wwwAuthenticateHeader,
            byte[] entityBodyOrNull        // required only when qop=auth-int
    ) {
        Map<String, String> chal = parseWwwAuthenticate(wwwAuthenticateHeader);
        String realm = chal.getOrDefault("realm", "");
        String nonce = chal.getOrDefault("nonce", "");
        String qopRaw = chal.getOrDefault("qop", "");          // e.g. "auth,auth-int"
        String opaque = chal.get("opaque");
        String algorithm = Optional.ofNullable(chal.get("algorithm")).orElse("MD5");

        String qop = selectQop(qopRaw); // prefer "auth"; null if unsupported
        String cnonce = randomCnonce();
        String nc = "00000001";

        // HA1
        String ha1 = md5Hex(username + ":" + realm + ":" + password);
        if ("MD5-sess".equalsIgnoreCase(algorithm)) {
            ha1 = md5Hex(ha1 + ":" + nonce + ":" + cnonce);
        }

        // HA2
        String ha2;
        if ("auth-int".equals(qop)) {
            String entityMd5 = md5Hex(entityBodyOrNull == null ? new byte[0] : entityBodyOrNull);
            ha2 = md5Hex(method + ":" + requestUri + ":" + entityMd5);
        } else {
            ha2 = md5Hex(method + ":" + requestUri);
        }

        // response
        String response;
        if (qop != null) {
            response = md5Hex(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
        } else {
            response = md5Hex(ha1 + ":" + nonce + ":" + ha2);
        }

        List<String> parts = new ArrayList<>();
        parts.add(kv("username", username, true));
        parts.add(kv("realm", realm, true));
        parts.add(kv("nonce", nonce, true));
        parts.add(kv("uri", requestUri, true));
        parts.add(kv("response", response, true));
        if (algorithm != null) parts.add(kv("algorithm", algorithm, false));
        if (opaque != null) parts.add(kv("opaque", opaque, true));
        if (qop != null) {
            parts.add(kv("qop", qop, false));
            parts.add(kv("nc", nc, false));
            parts.add(kv("cnonce", cnonce, true));
        }

        return "Digest " + String.join(", ", parts);
    }

    private static String selectQop(String qopRaw) {
        if (qopRaw == null || qopRaw.isEmpty()) return null;
        String[] arr = qopRaw.split(",");
        for (String q : arr) {
            if ("auth".equalsIgnoreCase(q.trim())) return "auth";
        }
        for (String q : arr) {
            if ("auth-int".equalsIgnoreCase(q.trim())) return "auth-int";
        }
        return null; // unrecognized qop — omit from header
    }

    private static String kv(String k, String v, boolean quote) {
        if (quote) return k + "=\"" + v + "\"";
        return k + "=" + v;
    }

    public static String md5Hex(String s) {
        return md5Hex(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String md5Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String randomCnonce() {
        byte[] buf = new byte[16];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
