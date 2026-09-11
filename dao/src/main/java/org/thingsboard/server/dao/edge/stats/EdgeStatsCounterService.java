// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge.stats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ConcurrentHashMap;

@ConditionalOnProperty(prefix = "edges.stats", name = "enabled", havingValue = "true", matchIfMissing = false)
@Service
@Slf4j
@Getter
public class EdgeStatsCounterService {

    private final ConcurrentHashMap<EdgeId, MsgCounters> counterByEdge = new ConcurrentHashMap<>();

    public void recordEvent(EdgeStatsKey type, TenantId tenantId, EdgeId edgeId, long value) {
        MsgCounters counters = getOrCreateCounters(tenantId, edgeId);
        switch (type) {
            case DOWNLINK_MSGS_ADDED -> counters.getMsgsAdded().addAndGet(value);
            case DOWNLINK_MSGS_PUSHED -> counters.getMsgsPushed().addAndGet(value);
            case DOWNLINK_MSGS_PERMANENTLY_FAILED -> counters.getMsgsPermanentlyFailed().addAndGet(value);
            case DOWNLINK_MSGS_TMP_FAILED -> counters.getMsgsTmpFailed().addAndGet(value);
        }
    }

    public void setDownlinkMsgsLag(TenantId tenantId, EdgeId edgeId, long value) {
        getOrCreateCounters(tenantId, edgeId).getMsgsLag().set(value);
    }

    public void clear(EdgeId edgeId) {
        counterByEdge.remove(edgeId);
    }

    public MsgCounters getOrCreateCounters(TenantId tenantId, EdgeId edgeId) {
        return counterByEdge.computeIfAbsent(edgeId, id -> new MsgCounters(tenantId));
    }

}
