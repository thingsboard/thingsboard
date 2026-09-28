// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TbAvgIntervalStateTest {

    private TbAvgIntervalState state;
    private Gson gson;


    @BeforeEach
    public void init() {
        state = new TbAvgIntervalState();
        gson = new Gson();
    }


    @Test
    public void testDoUpdate() {
        state.doUpdate(new JsonPrimitive("1"));
        state.doUpdate(new JsonPrimitive(2));
        state.doUpdate(new JsonPrimitive(0x3));
        state.doUpdate(new JsonPrimitive('4'));
        assertAvg(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3), new BigDecimal(4));
    }

    @Test
    public void testDoUpdateBigNumbers() {
        BigDecimal num1 = new BigDecimal("11111111111111111111111111111111111111111111111111111");
        BigDecimal num2 = new BigDecimal("22222222222222222222222222222222222222222222222222222");
        BigDecimal num3 = new BigDecimal("33333333333333333333333333333333333333333333333333333");
        BigDecimal num4 = new BigDecimal("44444444444444444444444444444444444444444444444444444");

        TbIntervalStateUtil.doUpdateForValues(state, num1, num2, num3, num4);
        assertAvg(num1, num2, num3, num4);
    }

    @Test
    public void testDoUpdateSmallNumbers() {
        BigDecimal num1 = new BigDecimal("0.11111111111111111111111111111111111111111111111111111");
        BigDecimal num2 = new BigDecimal("0.22222222222222222222222222222222222222222222222222222");
        BigDecimal num3 = new BigDecimal("0.33333333333333333333333333333333333333333333333333333");
        BigDecimal num4 = new BigDecimal("0.44444444444444444444444444444444444444444444444444444");

        TbIntervalStateUtil.doUpdateForValues(state, num1, num2, num3, num4);
        assertAvg(num1, num2, num3, num4);
    }

    private void assertAvg(BigDecimal ... updateValues) {
        BigDecimal expectedSum = new BigDecimal(0);
        BigDecimal expectedCount = new BigDecimal(updateValues.length);

        for (BigDecimal value : updateValues) {
            expectedSum = expectedSum.add(value);
        }

        BigDecimal expectedAvg = expectedSum.divide(expectedCount, 2, RoundingMode.HALF_UP);

        TbIntervalStateUtil.assertEquals(
                expectedSum,
                state.getSum(),
                "TbAvgIntervalState SUM"
        );
        TbIntervalStateUtil.assertEquals(
                expectedCount,
                new BigDecimal(state.getCount()),
                "TbAvgIntervalState COUNT"
        );

        assertEquals(expectedAvg.doubleValue(), getAvgFromState().doubleValue());
    }

    private BigDecimal getAvgFromState() {
        return gson.fromJson(
                        state.toValueJson(gson, "result"),
                        JsonObject.class)
                .get("result").getAsBigDecimal();
    }
}