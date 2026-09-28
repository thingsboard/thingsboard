// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record TrendzHealthcheckResult(
        @JsonProperty("version") String version,
        @JsonProperty("type") TrendzSynchronizationResultType type,
        @JsonProperty("status") TrendzSynchronizationStatus status,
        @JsonProperty("message") String message
) implements Serializable {}
