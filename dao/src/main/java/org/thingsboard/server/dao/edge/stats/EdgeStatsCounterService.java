/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.edge.stats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ConcurrentHashMap;

@ConditionalOnProperty(prefix = "edges.stats", name = "enabled", havingValue = "true")
@Service
@Slf4j
@Getter
public class EdgeStatsCounterService {

    private final ConcurrentHashMap<EdgeId, MsgCounters> msgCountersByEdge = new ConcurrentHashMap<>();

    public void recordEvent(EdgeStatsKey type, TenantId tenantId, EdgeId edgeId, long value) {
        MsgCounters counters = getOrCreateCounters(tenantId, edgeId);
        switch (type) {
            case DOWNLINK_MSGS_ADDED -> counters.getMsgsAdded().addAndGet(value);
            case DOWNLINK_MSGS_PUSHED -> counters.getMsgsPushed().addAndGet(value);
            case DOWNLINK_MSGS_PERMANENTLY_FAILED -> counters.getMsgsPermanentlyFailed().addAndGet(value);
            case DOWNLINK_MSGS_TMP_FAILED -> counters.getMsgsTmpFailed().addAndGet(value);
            case DOWNLINK_MSGS_LAG -> counters.getMsgsLag().set(value);
        }
    }

    public MsgCounters getOrCreateCounters(TenantId tenantId, EdgeId edgeId) {
        return msgCountersByEdge.computeIfAbsent(edgeId, id -> new MsgCounters(tenantId));
    }

    public void clear(EdgeId edgeId) {
        msgCountersByEdge.remove(edgeId);
    }

}
