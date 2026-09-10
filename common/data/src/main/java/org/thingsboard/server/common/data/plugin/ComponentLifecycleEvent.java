// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    DEACTIVATED(8);

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
