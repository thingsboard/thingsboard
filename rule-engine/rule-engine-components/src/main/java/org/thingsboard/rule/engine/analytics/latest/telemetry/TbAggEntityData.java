// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.SneakyThrows;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public class TbAggEntityData {

    private final EntityId entityId;
    private final Map<String, AttributeKvEntry> clientAttributes = new HashMap<>();
    private final Map<String, AttributeKvEntry> serverAttributes = new HashMap<>();
    private final Map<String, AttributeKvEntry> sharedAttributes = new HashMap<>();
    private final Map<String, TsKvEntry> latestTs = new HashMap<>();
    private final Map<String, KvEntry> filterMap = new HashMap<>();

    private volatile ListenableFuture<List<TsKvEntry>> tsFuture;
    private volatile ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
    private volatile ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
    private volatile ListenableFuture<List<AttributeKvEntry>> serverAttributesFuture;

    public void prepare() {
        putToMap(latestTs, tsFuture);
        putToMap(clientAttributes, clientAttributesFuture);
        putToMap(serverAttributes, serverAttributesFuture);
        putToMap(sharedAttributes, sharedAttributesFuture);
        putToMap(filterMap, clientAttributesFuture, null);
        putToMap(filterMap, sharedAttributesFuture, null);
        putToMap(filterMap, serverAttributesFuture, null);
        putToMap(filterMap, tsFuture, null);
        putToMap(filterMap, clientAttributesFuture, "cs_");
        putToMap(filterMap, sharedAttributesFuture, "shared_");
        putToMap(filterMap, serverAttributesFuture, "ss_");
    }

    @SneakyThrows
    private static <T extends KvEntry> void putToMap(Map<String, T> map, ListenableFuture<List<T>> future) {
        if (future == null) {
            return;
        }
        List<T> kvEntries = future.get();
        if (kvEntries != null) {
            kvEntries.forEach(e -> map.put(e.getKey(), e));
        }
    }

    @SneakyThrows
    private static <T extends KvEntry> void putToMap(Map<String, KvEntry> map, ListenableFuture<List<T>> future, String prefix) {
        if (future == null) {
            return;
        }
        List<T> kvEntries = future.get();
        if (kvEntries != null) {
            kvEntries.forEach(e -> map.put(StringUtils.isNotBlank(prefix) ? prefix + e.getKey() : e.getKey(), e));
        }
    }

    public TsKvEntry getLatestTs(String source) {
        return latestTs.get(source);
    }

    public Optional<KvEntry> getValue(String sourceScope, String source) {
        KvEntry dataPoint;
        switch (sourceScope) {
            case "LATEST_TELEMETRY":
                dataPoint = latestTs.get(source);
                break;
            case DataConstants.CLIENT_SCOPE:
                dataPoint = clientAttributes.get(source);
                break;
            case DataConstants.SHARED_SCOPE:
                dataPoint = sharedAttributes.get(source);
                break;
            case DataConstants.SERVER_SCOPE:
                dataPoint = serverAttributes.get(source);
                break;
            default:
                dataPoint = null;
        }
        return Optional.ofNullable(dataPoint);
    }
}
