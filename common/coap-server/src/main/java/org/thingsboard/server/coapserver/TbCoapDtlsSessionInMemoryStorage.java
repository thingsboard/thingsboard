// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Data
public class TbCoapDtlsSessionInMemoryStorage {

    private final ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> dtlsSessionsMap = new ConcurrentHashMap<>();
    private long dtlsSessionInactivityTimeout;
    private long dtlsSessionReportTimeout;


    public TbCoapDtlsSessionInMemoryStorage(long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout) {
        this.dtlsSessionInactivityTimeout = dtlsSessionInactivityTimeout;
        this.dtlsSessionReportTimeout = dtlsSessionReportTimeout;
    }

    public void put(TbCoapDtlsSessionKey tbCoapDtlsSessionKey, TbCoapDtlsSessionInfo dtlsSessionInfo) {
        log.trace("DTLS session added to in-memory store: [{}] timestamp: [{}]", tbCoapDtlsSessionKey, dtlsSessionInfo.getLastActivityTime());
        dtlsSessionsMap.putIfAbsent(tbCoapDtlsSessionKey, dtlsSessionInfo);
    }

    /**
     * Requests of an evicted session fall back to access token authentication and are rejected, so a device whose
     * certificate was revoked stops working without waiting for the inactivity timeout it would otherwise never reach.
     */
    public void evictSessions(TenantId tenantId, String certificateHash) {
        dtlsSessionsMap.entrySet().removeIf(entry -> {
            TbCoapDtlsSessionInfo sessionInfo = entry.getValue();
            if (certificateHash.equals(sessionInfo.getCertificateHash())
                    && tenantId.equals(sessionInfo.getMsg().getDeviceInfo().getTenantId())) {
                log.debug("[{}] DTLS session of revoked certificate was removed from in-memory store: [{}]", tenantId, entry.getKey());
                return true;
            } else {
                return false;
            }
        });
    }

    public void evictTimeoutSessions() {
        long expTime = System.currentTimeMillis() - dtlsSessionInactivityTimeout;
        dtlsSessionsMap.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastActivityTime() < expTime) {
                log.trace("DTLS session was removed from in-memory store: [{}]", entry.getKey());
                return true;
            } else {
                return false;
            }
        });
    }

}