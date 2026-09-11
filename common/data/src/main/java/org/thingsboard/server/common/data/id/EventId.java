// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class EventId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public EventId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static EventId fromString(String eventId) {
        return new EventId(UUID.fromString(eventId));
    }
}
