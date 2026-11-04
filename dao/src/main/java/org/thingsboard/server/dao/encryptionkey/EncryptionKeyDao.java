// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.encryptionkey;

import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;

public interface EncryptionKeyDao extends Dao<EncryptionKey> {

    EncryptionKey findByTenantId(TenantId tenantId);

    void deleteByTenantId(TenantId tenantId);

}
