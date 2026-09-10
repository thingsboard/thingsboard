/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response object for device ping endpoint
 * Contains device reachability information including device ID, status, and last seen timestamp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Device ping response containing reachability status and last seen timestamp")
public class DevicePingResponse {

    @Schema(description = "Device UUID", required = true, example = "784f394c-42b6-435a-983c-b7beff2784f9")
    @JsonProperty("deviceId")
    private String deviceId;

    @Schema(description = "Device reachability status", required = true, example = "true")
    @JsonProperty("reachable")
    private boolean reachable;

    @Schema(description = "Last seen timestamp in milliseconds", example = "1733507400000")
    @JsonProperty("lastSeen")
    private Long lastSeen;

    /**
     * Constructor with deviceId string and lastSeen timestamp
     * Calculates reachability based on last seen time
     * Device is considered reachable if it was seen in the last 5 minutes
     * 
     * @param deviceId Device UUID as string
     * @param lastSeen Last seen timestamp in milliseconds (can be null if device never seen)
     */
    public DevicePingResponse(String deviceId, Long lastSeen) {
        this.deviceId = deviceId;
        this.lastSeen = lastSeen;
        // Device is reachable if last seen within 5 minutes (300000 ms)
        this.reachable = lastSeen != null && 
                         (System.currentTimeMillis() - lastSeen) < 300000;
    }

    /**
     * Constructor with UUID object and lastSeen timestamp
     * Converts UUID to string and calculates reachability
     * 
     * @param deviceId Device UUID object
     * @param lastSeen Last seen timestamp in milliseconds (can be null if device never seen)
     */
    public DevicePingResponse(UUID deviceId, Long lastSeen) {
        this(deviceId != null ? deviceId.toString() : null, lastSeen);
    }
}