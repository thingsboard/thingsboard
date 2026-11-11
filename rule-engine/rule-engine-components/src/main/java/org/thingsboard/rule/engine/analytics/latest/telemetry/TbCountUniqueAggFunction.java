// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TbCountUniqueAggFunction implements TbAggFunction {

    private final Set<String> items = new HashSet<>();

    @Override
    public void update(Optional<KvEntry> entry, double defaultValue) {
        entry.map(KvEntry::getValueAsString).ifPresent(items::add);
    }

    @Override
    public Optional<JsonElement> result() {
        return Optional.of(new JsonPrimitive(items.size()));
    }

}
