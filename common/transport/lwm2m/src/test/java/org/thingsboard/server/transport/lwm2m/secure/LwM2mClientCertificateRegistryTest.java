// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mClientCertificateRegistry.ClientCertificate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LwM2mClientCertificateRegistryTest {

    private final LwM2mClientCertificateRegistry registry = new LwM2mClientCertificateRegistry();
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Test
    public void givenCertificateReplacedByNewOne_whenOldOneIsRevoked_thenItsClientsAreStillFound() {
        commit(1, new ClientCertificate("endpoint", tenantId, "old-hash", Long.MAX_VALUE));
        commit(2, new ClientCertificate("endpoint", tenantId, "new-hash", Long.MAX_VALUE));

        // The endpoint authenticated with the new certificate last: its registration most likely uses that one
        assertThat(registry.revoke(tenantId, "old-hash")).isEqualTo(Map.of("endpoint", false));
        assertThat(registry.findByCertificate(tenantId, "old-hash")).isEmpty();
        assertThat(registry.findByCertificate(tenantId, "new-hash")).hasSize(1);
        assertThat(registry.revoke(tenantId, "new-hash")).isEqualTo(Map.of("endpoint", true));
    }

    @Test
    public void givenCertificateOfAnotherTenant_whenRevoked_thenItsClientsAreNotAffected() {
        commit(1, new ClientCertificate("endpoint", tenantId, "hash", Long.MAX_VALUE));

        assertThat(registry.revoke(TenantId.fromUUID(UUID.randomUUID()), "hash")).isEmpty();
        assertThat(registry.findByCertificate(tenantId, "hash")).hasSize(1);
    }

    @Test
    public void givenRevokedCertificate_whenHandshakeCheckedItBeforeOrAfterTheRevocation_thenOnlyTheLaterOneCompletes() {
        ClientCertificate certificate = new ClientCertificate("endpoint", tenantId, "hash", Long.MAX_VALUE);
        registry.registerPending(cid(1), certificate, System.currentTimeMillis() - 1);
        registry.revoke(tenantId, "hash");
        // E.g. the certificate was restored, and the core saw that when the device connected again
        registry.registerPending(cid(2), certificate, System.currentTimeMillis() + 1);

        assertThat(registry.commitPending(cid(1))).isFalse();
        assertThat(registry.commitPending(cid(2))).isTrue();
        assertThat(registry.findByCertificate(tenantId, "hash")).hasSize(1);
    }

    @Test
    public void givenExpiredCertificate_whenEndpointAuthenticatesAgain_thenItIsForgotten() {
        commit(1, new ClientCertificate("endpoint", tenantId, "expired-hash", System.currentTimeMillis() - 1));
        commit(2, new ClientCertificate("endpoint", tenantId, "hash", Long.MAX_VALUE));

        assertThat(registry.findByCertificate(tenantId, "expired-hash")).isEmpty();
        assertThat(registry.findByCertificate(tenantId, "hash")).hasSize(1);
    }

    private void commit(int connection, ClientCertificate certificate) {
        registry.registerPending(cid(connection), certificate, System.currentTimeMillis());
        assertThat(registry.commitPending(cid(connection))).isTrue();
    }

    private static ConnectionId cid(int connection) {
        return new ConnectionId(new byte[]{(byte) connection, 0, 0, 0});
    }

}
