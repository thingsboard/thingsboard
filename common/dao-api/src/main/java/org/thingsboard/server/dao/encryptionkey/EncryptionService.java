// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.encryptionkey;

import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;

public interface EncryptionService {

    void createEncryptionKey(TenantId tenantId);

    byte[] encrypt(TenantId tenantId, SecretType secretType, byte[] value);

    String decryptToString(TenantId tenantId, SecretType secretType, byte[] encryptedValue);

    void deleteEncryptionKeyByTenantId(TenantId tenantId);

    EncryptionKey findByTenantId(TenantId tenantId);

}
