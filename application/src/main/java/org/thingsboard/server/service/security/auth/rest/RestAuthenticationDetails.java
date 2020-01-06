/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

package org.thingsboard.server.service.security.auth.rest;

import lombok.Data;
import ua_parser.Client;
import ua_parser.Parser;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;

@Data
public class RestAuthenticationDetails implements Serializable {

    private final String clientAddress;
    private final Client userAgent;

    public RestAuthenticationDetails(HttpServletRequest request) {
        this.clientAddress = getClientIP(request);
        this.userAgent = getUserAgent(request);
    }

    private static String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private static Client getUserAgent(HttpServletRequest request) {
        try {
            Parser uaParser = new Parser();
            return uaParser.parse(request.getHeader("User-Agent"));
        } catch (IOException e) {
            return new Client(null, null, null);
        }
    }
}
