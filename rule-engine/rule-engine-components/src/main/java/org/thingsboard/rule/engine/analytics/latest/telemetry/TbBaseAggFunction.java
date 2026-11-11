// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.Optional;

public abstract class TbBaseAggFunction implements TbAggFunction {

    private boolean hasResult = false;

    @Override
    public void update(Optional<KvEntry> entry, double defaultValue) {
        double value = extractDoubleValue(entry, defaultValue);
        doUpdate(value);
        hasResult = true;
    }

    @Override
    public Optional<JsonElement> result() {
        if (hasResult) {
            return Optional.of(new JsonPrimitive(prepareResult()));
        } else {
            return Optional.empty();
        }
    }

    protected abstract void doUpdate(double value);

    protected abstract double prepareResult();

    private double extractDoubleValue(Optional<KvEntry> entry, double defaultValue) {
        double result = defaultValue;
        if (entry.isPresent() && entry.get().getValue() != null) {
            KvEntry kvEntry = entry.get();
            switch (kvEntry.getDataType()) {
                case LONG:
                    result = kvEntry.getLongValue().get();
                    break;
                case DOUBLE:
                    result = kvEntry.getDoubleValue().get();
                    break;
                case BOOLEAN:
                    result = kvEntry.getBooleanValue().get() ? 1 : 0;
                    break;
                case STRING:
                    String str = kvEntry.getStrValue().get();
                    try {
                        result = Double.parseDouble(str);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Aggregation failed. Unable to parse value [" + str + "] of attribute [" + kvEntry.getKey() + "] to Double");
                    }
                    break;
            }
        }
        return result;
    }

}
