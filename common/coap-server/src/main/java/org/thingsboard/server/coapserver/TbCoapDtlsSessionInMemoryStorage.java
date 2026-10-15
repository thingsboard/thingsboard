// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class TbCoapDtlsSessionInMemoryStorage {

    // A handshake gets the answer of the core about its certificate within seconds, see TbCoapDtlsCertificateVerifier
    private static final long CERTIFICATE_CHECK_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

    private final ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> dtlsSessionsMap = new ConcurrentHashMap<>();
    // Revocation time by certificate: a handshake whose certificate was checked before it doesn't store its session
    private final ConcurrentMap<RevokedCertificate, Long> revocations = new ConcurrentHashMap<>();
    private long dtlsSessionInactivityTimeout;
    private long dtlsSessionReportTimeout;


    public TbCoapDtlsSessionInMemoryStorage(long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout) {
        this.dtlsSessionInactivityTimeout = dtlsSessionInactivityTimeout;
        this.dtlsSessionReportTimeout = dtlsSessionReportTimeout;
    }

    /**
     * @return false if the certificate was revoked after checkedTime, in which case the session is not stored
     */
    public boolean put(TbCoapDtlsSessionKey tbCoapDtlsSessionKey, TbCoapDtlsSessionInfo dtlsSessionInfo, long checkedTime) {
        log.trace("DTLS session added to in-memory store: [{}] timestamp: [{}]", tbCoapDtlsSessionKey, dtlsSessionInfo.getLastActivityTime());
        dtlsSessionsMap.putIfAbsent(tbCoapDtlsSessionKey, dtlsSessionInfo);
        // Checked after the put, while evictSessions records the revocation before it removes sessions: one of them sees the other
        Long revokedTime = revocations.get(new RevokedCertificate(getTenantId(dtlsSessionInfo), dtlsSessionInfo.getCertificateHash()));
        if (revokedTime != null && revokedTime >= checkedTime) {
            log.debug("DTLS session of a certificate revoked during the handshake was not stored: [{}]", tbCoapDtlsSessionKey);
            dtlsSessionsMap.remove(tbCoapDtlsSessionKey, dtlsSessionInfo);
            return false;
        }
        return true;
    }

    public void evictSessions(TenantId tenantId, String certificateHash) {
        revocations.put(new RevokedCertificate(tenantId, certificateHash), System.currentTimeMillis());
        dtlsSessionsMap.entrySet().removeIf(entry -> {
            TbCoapDtlsSessionInfo sessionInfo = entry.getValue();
            if (certificateHash.equals(sessionInfo.getCertificateHash()) && tenantId.equals(getTenantId(sessionInfo))) {
                log.debug("[{}] DTLS session of revoked certificate was removed from in-memory store: [{}]", tenantId, entry.getKey());
                return true;
            } else {
                return false;
            }
        });
    }

    public void evictTimeoutSessions() {
        long currentTime = System.currentTimeMillis();
        long expTime = currentTime - dtlsSessionInactivityTimeout;
        dtlsSessionsMap.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastActivityTime() < expTime) {
                log.trace("DTLS session was removed from in-memory store: [{}]", entry.getKey());
                return true;
            } else {
                return false;
            }
        });
        // Kept while a handshake checked before the revocation can still be reported
        long revocationExpTime = currentTime - dtlsSessionReportTimeout - CERTIFICATE_CHECK_TIMEOUT;
        revocations.values().removeIf(revokedTime -> revokedTime < revocationExpTime);
    }

    private static TenantId getTenantId(TbCoapDtlsSessionInfo sessionInfo) {
        return sessionInfo.getMsg().getDeviceInfo().getTenantId();
    }

    private record RevokedCertificate(TenantId tenantId, String certificateHash) {}

}
