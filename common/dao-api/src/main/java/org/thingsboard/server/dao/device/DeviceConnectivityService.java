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

    JsonNode getConnectivityInfo(String baseUrl) throws URISyntaxException;

    Resource getPemCertFile(String protocol);

    Resource createGatewayDockerComposeFile(String baseUrl, Device device) throws URISyntaxException;

    Resource createGatewayDockerComposeFile(String baseUrl, Device device, DockerComposeParams params) throws URISyntaxException;

    String resolveGatewayHost(String baseUrl);

    String resolveGatewayPort();
}
