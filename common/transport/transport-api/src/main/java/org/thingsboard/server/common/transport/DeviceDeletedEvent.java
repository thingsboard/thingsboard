// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

import java.io.Serial;

public final class DeviceDeletedEvent extends TbApplicationEvent {

    @Serial
    private static final long serialVersionUID = -7453664970966733857L;

    @Getter
    private final DeviceId deviceId;

    public DeviceDeletedEvent(DeviceId deviceId) {
        super(new Object());
        this.deviceId = deviceId;
    }

}
