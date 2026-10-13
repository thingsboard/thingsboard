// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class TbSumIntervalStateTest {

    private TbSumIntervalState state;

    @BeforeEach
    public void init() {
        state = new TbSumIntervalState();
    }


    @Test
    public void testDoUpdate() {
        state.doUpdate(new JsonPrimitive("1"));
        state.doUpdate(new JsonPrimitive(2));
        state.doUpdate(new JsonPrimitive(0x3));
        state.doUpdate(new JsonPrimitive('4'));
        TbIntervalStateUtil.assertEquals(
                new BigDecimal(10),
                state.getSum(),
                "TbSumIntervalState SUM"
        );
    }

    @Test
    public void testDoUpdateBigNumbers() {
        BigDecimal num1 = new BigDecimal("11111111111111111111111111111111111111111111111111111");
        BigDecimal num2 = new BigDecimal("22222222222222222222222222222222222222222222222222222");
        BigDecimal num3 = new BigDecimal("33333333333333333333333333333333333333333333333333333");
        BigDecimal num4 = new BigDecimal("44444444444444444444444444444444444444444444444444444");

        BigDecimal expected = num1.add(num2).add(num3).add(num4);

        TbIntervalStateUtil.doUpdateForValues(state, num1, num2, num3, num4);
        TbIntervalStateUtil.assertEquals(expected, state.getSum(), "TbSumIntervalState SUM");
    }

    @Test
    public void testDoUpdateSmallNumbers() {
        BigDecimal num1 = new BigDecimal("0.11111111111111111111111111111111111111111111111111111");
        BigDecimal num2 = new BigDecimal("0.22222222222222222222222222222222222222222222222222222");
        BigDecimal num3 = new BigDecimal("0.33333333333333333333333333333333333333333333333333333");
        BigDecimal num4 = new BigDecimal("0.44444444444444444444444444444444444444444444444444444");

        BigDecimal expected = num1.add(num2).add(num3).add(num4);

        TbIntervalStateUtil.doUpdateForValues(state, num1, num2, num3, num4);
        TbIntervalStateUtil.assertEquals(expected, state.getSum(), "TbSumIntervalState SUM");
    }
}