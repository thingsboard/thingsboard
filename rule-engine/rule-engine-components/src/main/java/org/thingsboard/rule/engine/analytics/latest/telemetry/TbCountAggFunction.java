// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.Optional;

public class TbCountAggFunction implements TbAggFunction {

    private long count = 0L;

    @Override
    public void update(Optional<KvEntry> entry, double defaultValue) {
        count++;
    }

    @Override
    public Optional<JsonElement> result() {
        return Optional.of(new JsonPrimitive(count));
    }

    @Override
    public boolean fetchAttrValue() {
        return false;
    }

}
