// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
public enum WebSocketSessionType {
    GENERAL(),
    TELEMETRY("telemetry"), // deprecated
    NOTIFICATIONS("notifications"); // deprecated

    private String name;

    public static Optional<WebSocketSessionType> forName(String name) {
        return Arrays.stream(values())
                .filter(sessionType -> Objects.equals(sessionType.name, name))
                .findFirst();
    }

}
