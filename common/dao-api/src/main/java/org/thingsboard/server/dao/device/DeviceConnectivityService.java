// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.Resource;
import org.thingsboard.server.common.data.Device;

import java.net.URISyntaxException;

public interface DeviceConnectivityService {

    JsonNode findDevicePublishTelemetryCommands(String baseUrl, Device device) throws URISyntaxException;

    /**
     * Resolved host and port of each enabled transport, with the source of the host.
     * A blank port is replaced with the standard port of the protocol.
     */
    JsonNode getConnectivityInfo(String baseUrl, DeviceConnectivityHostSource baseUrlSource) throws URISyntaxException;

    /**
     * Same as {@link #getConnectivityInfo(String, DeviceConnectivityHostSource)}, with a base URL
     * built from the request.
     */
    default JsonNode getConnectivityInfo(String baseUrl) throws URISyntaxException {
        return getConnectivityInfo(baseUrl, DeviceConnectivityHostSource.REQUEST);
    }

    Resource getPemCertFile(String protocol);

    Resource createGatewayDockerComposeFile(String baseUrl, Device device) throws URISyntaxException;

    Resource createGatewayDockerComposeFile(String baseUrl, Device device, DockerComposeParams params) throws URISyntaxException;

    String resolveGatewayHost(String baseUrl);

    String resolveGatewayPort();
}
