// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.Optional;

public interface TbAggFunction {

    void update(Optional<KvEntry> entry, double defaultValue);

    Optional<JsonElement> result();

    default boolean fetchAttrValue() { return true; }

}
