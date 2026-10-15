// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.page.PageData;

public interface NativeDeviceRepository extends NativeProfileEntityRepository {

    PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable);

}
