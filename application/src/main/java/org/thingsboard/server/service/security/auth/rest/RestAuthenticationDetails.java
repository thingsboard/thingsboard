// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.rest;

import com.google.common.net.InetAddresses;
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
        this.resolvedAddress = addressAttribute(request, ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR);
        this.rawSocketAddress = addressAttribute(request, ClientAddressCaptureFilter.RAW_SOCKET_ADDRESS_ATTR);
    }

    private static InetAddress addressAttribute(HttpServletRequest request, String attribute) {
        if (request.getAttribute(attribute) instanceof InetAddress address) {
            return address;
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return null;
        }
        try {
            return InetAddresses.forString(remoteAddr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Client getUserAgent(HttpServletRequest request) {
        Parser uaParser = new Parser();
        return uaParser.parse(request.getHeader("User-Agent"));
    }

}
