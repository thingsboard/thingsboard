// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.page.PageData;

public interface NativeDeviceRepository extends NativeProfileEntityRepository {

    PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable);

}
