// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbAvgIntervalState extends TbBaseIntervalState {

    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;

    public TbAvgIntervalState(JsonElement stateJson) {
        JsonObject jsonObject = stateJson.getAsJsonObject();
        this.sum = new BigDecimal(jsonObject.get("sum").getAsString());
        this.count = jsonObject.get("count").getAsLong();
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        BigDecimal value = data.getAsBigDecimal();
        if (value.compareTo(BigDecimal.ZERO) != 0) {
            sum = sum.add(value);
        }
        this.count++;
        return true;
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("sum", sum.toString());
        object.addProperty("count", Long.toString(count));
        return gson.toJson(object);
    }
}
