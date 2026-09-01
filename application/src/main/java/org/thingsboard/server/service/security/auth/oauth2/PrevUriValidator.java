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
package org.thingsboard.server.service.security.auth.oauth2;

import org.thingsboard.server.common.data.StringUtils;

import java.util.Locale;

public class PrevUriValidator {

    private static final int MAX_LENGTH = 2048;

    /**
     * prevUri is appended to the platform base URL, which ends right after the authority, so the single leading '/'
     * is what keeps the redirect on this host - it closes the authority before any of the value is read. The rest
     * keeps an accepted value usable: it has to survive the cookie round trip (RFC 6265 allows neither control
     * characters nor '"', ',', ';', '\' or non-ASCII) and to pass StrictHttpFirewall, which rejects '//', '%2f'
     * and '%5c' in the path; a fragment would swallow the access token.
     */
    public static boolean isValid(String prevUri) {
        if (StringUtils.isEmpty(prevUri) || prevUri.length() > MAX_LENGTH || prevUri.charAt(0) != '/') {
            return false;
        }
        for (int i = 0; i < prevUri.length(); i++) {
            char c = prevUri.charAt(i);
            if (c <= ' ' || c >= 127 || c == '"' || c == ',' || c == ';' || c == '\\' || c == '#') {
                return false;
            }
        }
        String path = StringUtils.substringBefore(prevUri, "?").toLowerCase(Locale.ROOT);
        return !path.contains("//") && !path.contains("%2f") && !path.contains("%5c");
    }

}
