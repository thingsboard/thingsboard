// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbCountIntervalState extends TbBaseIntervalState {

    private long count = 0L;

    public TbCountIntervalState(JsonElement stateJson) {
        this.count = stateJson.getAsJsonObject().get("count").getAsLong();
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        this.count++;
        return true;
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, count);
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("count", Long.toString(count));
        return gson.toJson(object);
    }
}
