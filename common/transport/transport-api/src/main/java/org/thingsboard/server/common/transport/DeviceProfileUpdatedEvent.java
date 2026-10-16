// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

public final class DeviceProfileUpdatedEvent extends TbApplicationEvent {

    @Getter
    private final DeviceProfile deviceProfile;

    public DeviceProfileUpdatedEvent(DeviceProfile deviceProfile) {
        super(new Object());
        this.deviceProfile = deviceProfile;
    }
}
