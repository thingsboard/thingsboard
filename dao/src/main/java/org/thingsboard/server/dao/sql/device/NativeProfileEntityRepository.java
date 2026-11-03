// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.page.PageData;

import java.util.UUID;

public interface NativeProfileEntityRepository {

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(Pageable pageable);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, Pageable pageable);

}
