// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * X.509 certificates that LwM2M clients presented in DTLS handshakes on this node.
 * <p>
 * Lets the transport find the clients of a revoked certificate. In trust mode the stored device credentials hold the
 * endpoint only, so the certificate is known solely from the handshake. Kept per node on purpose: the DTLS connection that
 * must be terminated lives on the node that performed the handshake.
 * <p>
 * An endpoint can have connections with several certificates, e.g. while a device rotates its certificate: revoking the
 * old certificate after the rotation must still find the connections that use it.
 */
@Component
@TbLwM2mTransportComponent
public class LwM2mClientCertificateRegistry {

    // Longer than a handshake takes from the core's answer about its certificate to the client's Finished message
    private static final long REVOCATION_RETENTION = TimeUnit.MINUTES.toMillis(10);

    // By endpoint, then by certificate hash. The inner maps are immutable and replaced on every change.
    private final Map<String, Map<String, CommittedCertificate>> certificates = new ConcurrentHashMap<>();
    // Certificates of handshakes in progress, by connection: registered once the client proved that it holds the private key
    private final Map<ConnectionId, PendingCertificate> pendingCertificates = new ConcurrentHashMap<>();
    // When revocations were received: a handshake whose certificate the core checked before that must not complete
    private final Map<RevokedCertificate, Long> revocations = new ConcurrentHashMap<>();
    private final AtomicLong commitSequence = new AtomicLong();

    /**
     * @param checkedTime when the core was asked about the certificate
     */
    public void registerPending(ConnectionId cid, ClientCertificate certificate, long checkedTime) {
        pendingCertificates.put(cid, new PendingCertificate(certificate, checkedTime));
    }

    /**
     * @return false if the certificate of the handshake was revoked after the core checked it: the handshake must fail
     */
    public boolean commitPending(ConnectionId cid) {
        PendingCertificate pending = pendingCertificates.remove(cid);
        if (pending == null) {
            return true;
        }
        ClientCertificate certificate = pending.certificate();
        add(certificate);
        // Checked after the add, while revoke records the revocation before it removes certificates: one of them sees the other
        Long revokedTime = revocations.get(new RevokedCertificate(certificate.tenantId(), certificate.certificateHash()));
        if (revokedTime != null && revokedTime >= pending.checkedTime()) {
            remove(certificate);
            return false;
        }
        return true;
    }

    public void dropPending(ConnectionId cid) {
        pendingCertificates.remove(cid);
    }

    /**
     * Forgets the certificate and refuses handshakes with it that the core checked before.
     *
     * @return endpoints that authenticated with the certificate for the tenant on this node, each mapped to whether the
     *         certificate is the one the endpoint authenticated with last: the one its registration most likely uses
     */
    public Map<String, Boolean> revoke(TenantId tenantId, String certificateHash) {
        long currentTime = System.currentTimeMillis();
        long oldestPendingCheck = pendingCertificates.values().stream().mapToLong(PendingCertificate::checkedTime).min().orElse(currentTime);
        revocations.values().removeIf(revokedTime -> revokedTime < currentTime - REVOCATION_RETENTION && revokedTime < oldestPendingCheck);
        revocations.put(new RevokedCertificate(tenantId, certificateHash), currentTime);
        Map<String, Boolean> endpoints = new HashMap<>();
        certificates.forEach((endpoint, byHash) -> {
            CommittedCertificate revoked = byHash.get(certificateHash);
            if (revoked != null && revoked.certificate().tenantId().equals(tenantId)) {
                boolean last = byHash.values().stream().allMatch(other -> other.sequence() <= revoked.sequence());
                if (remove(revoked.certificate())) {
                    endpoints.put(endpoint, last);
                }
            }
        });
        return endpoints;
    }

    public List<ClientCertificate> findByCertificate(TenantId tenantId, String certificateHash) {
        return certificates.values().stream()
                .flatMap(byHash -> byHash.values().stream())
                .map(CommittedCertificate::certificate)
                .filter(certificate -> certificate.certificateHash().equals(certificateHash) && certificate.tenantId().equals(tenantId))
                .toList();
    }

    private void add(ClientCertificate certificate) {
        long currentTime = System.currentTimeMillis();
        certificates.compute(certificate.endpoint(), (endpoint, byHash) -> {
            Map<String, CommittedCertificate> result = byHash != null ? new HashMap<>(byHash) : new HashMap<>();
            // An expired certificate can't connect again: its entries would otherwise pile up with every rotation
            result.values().removeIf(other -> other.certificate().expirationTime() < currentTime);
            result.put(certificate.certificateHash(), new CommittedCertificate(certificate, commitSequence.incrementAndGet()));
            return Map.copyOf(result);
        });
    }

    private boolean remove(ClientCertificate certificate) {
        boolean[] removed = new boolean[1];
        certificates.computeIfPresent(certificate.endpoint(), (endpoint, byHash) -> {
            CommittedCertificate committed = byHash.get(certificate.certificateHash());
            if (committed == null || !committed.certificate().equals(certificate)) {
                return byHash;
            }
            removed[0] = true;
            Map<String, CommittedCertificate> result = new HashMap<>(byHash);
            result.remove(certificate.certificateHash());
            return result.isEmpty() ? null : Map.copyOf(result);
        });
        return removed[0];
    }

    /**
     * @param expirationTime when the certificate expires, or {@link Long#MAX_VALUE} if the transport accepts expired certificates
     */
    public record ClientCertificate(String endpoint, TenantId tenantId, String certificateHash, long expirationTime) {
    }

    private record PendingCertificate(ClientCertificate certificate, long checkedTime) {
    }

    private record CommittedCertificate(ClientCertificate certificate, long sequence) {
    }

    private record RevokedCertificate(TenantId tenantId, String certificateHash) {
    }

}
