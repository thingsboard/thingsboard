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
public class TbSumIntervalState extends TbBaseIntervalState {

    private BigDecimal sum = BigDecimal.ZERO;

    public TbSumIntervalState(JsonElement stateJson) {
        this.sum = new BigDecimal(stateJson.getAsJsonObject().get("sum").getAsString());
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        BigDecimal value = data.getAsBigDecimal();
        if (value.compareTo(BigDecimal.ZERO) != 0) {
            sum = sum.add(value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, sum.doubleValue());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("sum", sum.toString());
        return gson.toJson(object);
    }
}
