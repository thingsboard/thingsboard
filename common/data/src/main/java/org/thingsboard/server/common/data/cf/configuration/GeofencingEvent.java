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
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum GeofencingEvent {

    ENTERED(0), LEFT(1), INSIDE(2), OUTSIDE(3);

    private final int protoNumber; // Corresponds to GeofencingEvent

    GeofencingEvent(int protoNumber) {
        this.protoNumber = protoNumber;
    }

    private static final GeofencingEvent[] BY_PROTO;

    static {
        BY_PROTO = new GeofencingEvent[Arrays.stream(values()).mapToInt(GeofencingEvent::getProtoNumber).max().orElse(0) + 1];
        for (var event : values()) {
            BY_PROTO[event.getProtoNumber()] = event;
        }
    }

    public static GeofencingEvent fromProtoNumber(int protoNumber) {
        if (protoNumber < 0 || protoNumber >= BY_PROTO.length) {
            throw new IllegalArgumentException("Invalid GeofencingEvent proto number " + protoNumber);
        }
        return BY_PROTO[protoNumber];
    }

}
