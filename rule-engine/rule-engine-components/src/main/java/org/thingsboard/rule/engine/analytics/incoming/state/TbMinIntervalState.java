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
public class TbMinIntervalState extends TbBaseIntervalState {

    private BigDecimal min = null;

    public TbMinIntervalState(JsonElement stateJson) {
        this.min = stateJson.getAsJsonObject().get("min").getAsBigDecimal();
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        BigDecimal value = data.getAsBigDecimal();
        if (min == null || value.compareTo(min) < 0) {
            min = value;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, min.doubleValue());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("min", min);
        return gson.toJson(object);
    }
}
