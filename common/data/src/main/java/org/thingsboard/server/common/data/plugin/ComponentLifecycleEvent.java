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
package org.thingsboard.server.common.data.plugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;

@RequiredArgsConstructor
public enum ComponentLifecycleEvent implements Serializable {

    CREATED(0),
    STARTED(1),
    ACTIVATED(2),
    SUSPENDED(3),
    UPDATED(4),
    STOPPED(5),
    DELETED(6),
    FAILED(7),
    DEACTIVATED(8),
    RELATION_UPDATED(9),
    RELATION_DELETED(10);

    @Getter
    private final int protoNumber; // corresponds to ComponentLifecycleEvent proto

    private static final ComponentLifecycleEvent[] BY_PROTO;

    static {
        BY_PROTO = new ComponentLifecycleEvent[Arrays.stream(values()).mapToInt(ComponentLifecycleEvent::getProtoNumber).max().orElse(0) + 1];
        for (ComponentLifecycleEvent event : values()) {
            BY_PROTO[event.getProtoNumber()] = event;
        }
    }

    public static ComponentLifecycleEvent forProtoNumber(int protoNumber) {
        if (protoNumber < 0 || protoNumber >= BY_PROTO.length) {
            throw new IllegalArgumentException("Invalid ComponentLifecycleEvent proto number " + protoNumber);
        }
        return BY_PROTO[protoNumber];
    }

}
