// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import ua_parser.Client;
import ua_parser.Parser;

import java.io.Serializable;

@Data
public class RestAuthenticationDetails implements Serializable {

    private final String clientAddress;
    private final Client userAgent;

    public RestAuthenticationDetails(HttpServletRequest request) {
        this.clientAddress = request.getRemoteAddr();
        this.userAgent = getUserAgent(request);
    }

    private static Client getUserAgent(HttpServletRequest request) {
        Parser uaParser = new Parser();
        return uaParser.parse(request.getHeader("User-Agent"));
    }
}
