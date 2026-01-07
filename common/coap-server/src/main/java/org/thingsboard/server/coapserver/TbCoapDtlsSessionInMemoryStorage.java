/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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