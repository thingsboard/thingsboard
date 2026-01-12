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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class CookieUtils {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        ClassLoader loader = CookieUtils.class.getClassLoader();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(SecurityJackson2Modules.getModules(loader));
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    public static String serialize(Object object) {
        try {
            return Base64.getUrlEncoder()
                    .encodeToString(OBJECT_MAPPER.writeValueAsBytes(object));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + object + " cannot be transformed to a String", e);
        }
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
        try {
            return OBJECT_MAPPER.readValue(decodedBytes, cls);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + Arrays.toString(decodedBytes) + " cannot be transformed to Json object", e);
        }
    }
}
