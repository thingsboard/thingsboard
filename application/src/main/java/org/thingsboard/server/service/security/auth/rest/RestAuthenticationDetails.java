// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.thingsboard.server.config.ClientAddressCaptureFilter;
import ua_parser.Client;
import ua_parser.Parser;

import java.io.Serializable;
import java.net.InetAddress;

@Data
public class RestAuthenticationDetails implements Serializable {

    private final String clientAddress;
    private final Client userAgent;
    private final InetAddress resolvedAddress;
    private final InetAddress rawSocketAddress;

    public RestAuthenticationDetails(HttpServletRequest request) {
        this.clientAddress = request.getRemoteAddr();
        this.userAgent = getUserAgent(request);
        this.resolvedAddress = ClientAddressCaptureFilter.resolvedClientAddress(request);
        this.rawSocketAddress = ClientAddressCaptureFilter.rawSocketAddress(request);
    }

    private static Client getUserAgent(HttpServletRequest request) {
        Parser uaParser = new Parser();
        return uaParser.parse(request.getHeader("User-Agent"));
    }

}
