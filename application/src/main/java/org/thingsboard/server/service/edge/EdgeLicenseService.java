// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

public interface EdgeLicenseService {

    ResponseEntity<JsonNode> checkInstance(JsonNode request);

    ResponseEntity<JsonNode> activateInstance(String licenseSecret, String releaseDate);

    ResponseEntity<JsonNode> checkInstanceV2(JsonNode request);

    ResponseEntity<JsonNode> activateInstanceV2(String licenseSecret, String releaseDate, String clusterId, String protocolVersion);
}
