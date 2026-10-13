// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ota;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.util.UUID;

@Data
public class DeviceGroupOtaPackage {
    private UUID id;
    private EntityGroupId groupId;
    private OtaPackageType otaPackageType;
    private OtaPackageId otaPackageId;
    private long otaPackageUpdateTime;
}
