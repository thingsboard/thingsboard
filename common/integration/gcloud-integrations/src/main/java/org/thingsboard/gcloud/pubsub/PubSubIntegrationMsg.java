// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.gcloud.pubsub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PubSubIntegrationMsg {

    private byte[] payload;
    private Map<String, String> deviceMetadata;

}
