// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.page.PageData;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface NativeProfileEntityRepository {

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(Pageable pageable);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, Pageable pageable);

    void findProfileEntityIdInfosByTenantId(UUID tenantId, int pageSize, Consumer<List<ProfileEntityIdInfo>> pageConsumer);

}
