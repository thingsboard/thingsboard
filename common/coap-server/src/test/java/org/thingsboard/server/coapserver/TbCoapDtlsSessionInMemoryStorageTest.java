// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.coapserver;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TbCoapDtlsSessionInMemoryStorageTest {

    private final TbCoapDtlsSessionInMemoryStorage storage = new TbCoapDtlsSessionInMemoryStorage(86400000, 1800000);

    @Test
    public void givenRevokedCertificate_whenEvictSessions_thenOnlyItsSessionsOfThatTenantAreRemoved() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantId otherTenantId = TenantId.fromUUID(UUID.randomUUID());
        TbCoapDtlsSessionKey revoked = put(5683, tenantId, "revoked-hash");
        TbCoapDtlsSessionKey revokedReconnected = put(5684, tenantId, "revoked-hash");
        TbCoapDtlsSessionKey otherCertificate = put(5685, tenantId, "other-hash");
        TbCoapDtlsSessionKey otherTenant = put(5686, otherTenantId, "revoked-hash");

        storage.evictSessions(tenantId, "revoked-hash");

        assertThat(storage.getDtlsSessionsMap()).doesNotContainKeys(revoked, revokedReconnected);
        assertThat(storage.getDtlsSessionsMap()).containsKeys(otherCertificate, otherTenant);
    }

    private TbCoapDtlsSessionKey put(int port, TenantId tenantId, String certificateHash) {
        TransportDeviceInfo deviceInfo = new TransportDeviceInfo();
        deviceInfo.setTenantId(tenantId);
        ValidateDeviceCredentialsResponse msg = ValidateDeviceCredentialsResponse.builder().deviceInfo(deviceInfo).build();
        TbCoapDtlsSessionKey key = new TbCoapDtlsSessionKey(new InetSocketAddress("127.0.0.1", port), certificateHash + port);
        storage.put(key, new TbCoapDtlsSessionInfo(msg, new DeviceProfile(), certificateHash));
        return key;
    }

}
