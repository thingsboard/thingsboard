// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import lombok.NonNull;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

record DeviceCertificateRevocationCacheKey(UUID tenantId, String certificateHash) implements Serializable {

    DeviceCertificateRevocationCacheKey {
        requireNonNull(tenantId);
        requireNonNull(certificateHash);
    }

    static DeviceCertificateRevocationCacheKey of(TenantId tenantId, String certificateHash) {
        return new DeviceCertificateRevocationCacheKey(tenantId.getId(), certificateHash);
    }

    @NonNull
    @Override
    public String toString() {
        return tenantId + "_" + certificateHash;
    }

}
