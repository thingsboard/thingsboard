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

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class CallbackUrlSchemeValidator {

    // RFC 3986 scheme grammar, plus '_': mobile apps derive the scheme from their package name, which may contain one
    private static final Pattern SCHEME_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.\\-_]*");
    private static final Set<String> FORBIDDEN_SCHEMES = Set.of("http", "https", "javascript", "data", "file", "vbscript");
    private static final int MAX_LOGGED_LENGTH = 128;

    /**
     * The redirect carrying the access token is built as callbackUrlScheme + ':', so only a mobile app scheme may
     * pass: a web scheme would send the token to whatever host follows it.
     */
    public static boolean isValid(String callbackUrlScheme) {
        return !StringUtils.isEmpty(callbackUrlScheme)
                && SCHEME_PATTERN.matcher(callbackUrlScheme).matches()
                && !FORBIDDEN_SCHEMES.contains(callbackUrlScheme.toLowerCase(Locale.ROOT));
    }

    /**
     * The attribute is restored from the oauth2_auth_request cookie, which the client can replace, so the scheme is
     * checked again on read and not only when the authorization request is built.
     */
    public static String getCallbackUrlScheme(OAuth2AuthorizationRequest authorizationRequest) {
        String callbackUrlScheme = authorizationRequest != null ?
                authorizationRequest.getAttribute(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME) : null;
        if (StringUtils.isEmpty(callbackUrlScheme)) {
            return null;
        }
        if (!isValid(callbackUrlScheme)) {
            log.warn("Ignoring invalid callback url scheme: [{}]", forLog(callbackUrlScheme));
            return null;
        }
        return callbackUrlScheme;
    }

    // a rejected value is attacker-controlled: it must not be able to forge log lines
    private static String forLog(String value) {
        return value.substring(0, Math.min(value.length(), MAX_LOGGED_LENGTH)).replaceAll("[^\\x20-\\x7E]", "?");
    }

}
