// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * X.509 certificates that LwM2M clients presented in DTLS handshakes on this node, by endpoint.
 * <p>
 * Lets the transport find the clients of a revoked certificate. In trust mode the stored device credentials hold the
 * endpoint only, so the certificate is known solely from the handshake. Kept per node on purpose: the DTLS connection that
 * must be terminated lives on the node that performed the handshake.
 */
@Component
@TbLwM2mTransportComponent
public class LwM2mClientCertificateRegistry {

    private final Map<String, ClientCertificate> certificates = new ConcurrentHashMap<>();
    // Certificates of handshakes in progress, by connection: registered once the client proved that it holds the private key
    private final Map<ConnectionId, ClientCertificate> pendingCertificates = new ConcurrentHashMap<>();

    public void registerPending(ConnectionId cid, String endpoint, TenantId tenantId, String certificateHash) {
        pendingCertificates.put(cid, new ClientCertificate(endpoint, tenantId, certificateHash));
    }

    public void commitPending(ConnectionId cid) {
        ClientCertificate certificate = pendingCertificates.remove(cid);
        if (certificate != null) {
            certificates.put(certificate.endpoint(), certificate);
        }
    }

    public void dropPending(ConnectionId cid) {
        pendingCertificates.remove(cid);
    }

    public void unregister(String endpoint) {
        certificates.remove(endpoint);
    }

    public List<ClientCertificate> findByCertificate(TenantId tenantId, String certificateHash) {
        return certificates.values().stream()
                .filter(certificate -> certificate.certificateHash().equals(certificateHash) && certificate.tenantId().equals(tenantId))
                .toList();
    }

    public record ClientCertificate(String endpoint, TenantId tenantId, String certificateHash) {
    }

}
