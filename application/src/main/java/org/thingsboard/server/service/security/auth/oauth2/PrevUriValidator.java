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

public class PrevUriValidator {

    /**
     * prevUri is appended to the platform base URL, so only a relative in-app path is accepted,
     * e.g. /dashboards/{id}?state=... . Everything a browser could resolve to another host - a
     * protocol-relative path (// or the /\ variant), userinfo, an absolute URI - is rejected,
     * as are whitespace and control characters, which browsers strip and which would allow
     * splitting the redirect header.
     */
    public static boolean isValid(String prevUri) {
        if (StringUtils.isEmpty(prevUri) || prevUri.charAt(0) != '/') {
            return false;
        }
        if (prevUri.length() > 1 && (prevUri.charAt(1) == '/' || prevUri.charAt(1) == '\\')) {
            return false;
        }
        for (int i = 0; i < prevUri.length(); i++) {
            char c = prevUri.charAt(i);
            if (c <= ' ' || c == '\\' || c == 127) {
                return false;
            }
        }
        return true;
    }

}
