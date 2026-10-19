// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.gen.transport.TransportProtos;

public interface TransportDeviceProfileCache {

    DeviceProfile getOrCreate(DeviceProfileId id, TransportProtos.DeviceProfileProto proto);

    DeviceProfile get(DeviceProfileId id);

    void put(DeviceProfile profile);

    DeviceProfile put(TransportProtos.DeviceProfileProto proto);

    void evict(DeviceProfileId id);

}
