// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.ota;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;

import java.util.List;

public interface OtaPackageStateService {

    void update(TenantId tenantId, DeviceGroupOtaPackage newDeviceGroupOtaPackage, DeviceGroupOtaPackage oldDeviceGroupOtaPackage);

    void update(TenantId tenantId, List<DeviceId> deviceIds, boolean isFirmware, boolean isSoftware);

    void update(Device device);

    void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged);

    boolean process(ToOtaPackageStateServiceMsg msg);
}
