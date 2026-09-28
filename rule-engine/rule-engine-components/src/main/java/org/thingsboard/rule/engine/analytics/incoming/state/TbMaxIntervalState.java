// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbMaxIntervalState extends TbBaseIntervalState {

    private BigDecimal max = null;

    public TbMaxIntervalState(JsonElement stateJson) {
        this.max = stateJson.getAsJsonObject().get("max").getAsBigDecimal();
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        BigDecimal value = data.getAsBigDecimal();
        if (max == null || value.compareTo(max) > 0) {
            max = value;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, max.doubleValue());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("max", max);
        return gson.toJson(object);
    }
}
