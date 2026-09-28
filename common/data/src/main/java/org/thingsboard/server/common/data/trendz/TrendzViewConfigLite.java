// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.UUID;

public record TrendzViewConfigLite(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name
) implements Serializable {}
