// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.dao.Dao;

public interface DeviceCertificateRevocationDao extends Dao<DeviceCertificateRevocation> {

    DeviceCertificateRevocation findByTenantIdAndCertificateHash(TenantId tenantId, String certificateHash);

    PageData<DeviceCertificateRevocation> findByTenantId(TenantId tenantId, PageLink pageLink);

    void removeByTenantId(TenantId tenantId);

    int deleteAllByNotAfterBefore(long ts);

}
