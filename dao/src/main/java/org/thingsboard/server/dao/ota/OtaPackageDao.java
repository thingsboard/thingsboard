// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ota;

import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityWithDataDao;

import java.util.UUID;

public interface OtaPackageDao extends Dao<OtaPackage>, TenantEntityWithDataDao, ExportableEntityDao<OtaPackageId, OtaPackage> {

    Long sumDataSizeByTenantId(TenantId tenantId);

    OtaPackage findOtaPackageByTenantIdAndTitleAndVersion(TenantId tenantId, String title, String version);

    Long getDataOidById(UUID id);

    Integer unlinkLargeObject(Long dataOid);

}
