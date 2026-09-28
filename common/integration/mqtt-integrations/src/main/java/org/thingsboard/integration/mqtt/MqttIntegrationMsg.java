// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.integration.api.data.ContentType;

public interface MqttIntegrationMsg {

    String getTopic();

    JsonNode toJson();

    ContentType getContentType();
}
