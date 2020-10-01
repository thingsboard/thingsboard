/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class WebUtils {
    private static final String X_FORWARDED_HOST_HEADER_KEY = "x-forwarded-host";
    private static final String X_FORWARDED_PORT_HEADER_KEY = "x-forwarded-port";
    private static final String X_FORWARDED_PROTO_HEADER_KEY = "x-forwarded-proto";

    public static String getHost(HttpServletRequest request) {
        String forwardedHost = request.getHeader(X_FORWARDED_HOST_HEADER_KEY);
        log.trace("Forwarded host - {}.", forwardedHost);
        if (!StringUtils.isEmpty(forwardedHost)) {
            if (forwardedHost.contains(":")) {
                return forwardedHost.substring(0, forwardedHost.indexOf(":"));
            } else {
                return forwardedHost;
            }
        } else {
            return request.getServerName();
        }
    }

    public static String getScheme(HttpServletRequest request) {
        String forwardedProto = request.getHeader(X_FORWARDED_PROTO_HEADER_KEY);
        log.trace("Forwarded proto - {}.", forwardedProto);
        if (!StringUtils.isEmpty(forwardedProto)) {
            return forwardedProto;
        } else {
            return request.getServerName();
        }
    }

    public static String getPort(HttpServletRequest request) {
        String forwardedPort = request.getHeader(X_FORWARDED_PORT_HEADER_KEY);
        log.trace("Forwarded port - {}.", forwardedPort);
        if (!StringUtils.isEmpty(forwardedPort)) {
            return forwardedPort;
        }
        String forwardedHost = request.getHeader(X_FORWARDED_HOST_HEADER_KEY);
        if (!StringUtils.isEmpty(forwardedHost)) {
            if (forwardedHost.contains(":")) {
                return forwardedHost.substring(forwardedHost.indexOf(":"));
            } else {
                return "HTTP".equals(getScheme(request).toUpperCase()) ?
                        "80" : "443";
            }
        }
        return Integer.toString(request.getServerPort());
    }
}
