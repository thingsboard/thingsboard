// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class TbIntervalStateUtil {
    public static void assertEquals(BigDecimal expected, BigDecimal actual, String alias) {
        assertThat(actual).as(alias).isEqualTo(expected);
    }

    public static void doUpdateForValues(TbBaseIntervalState state, BigDecimal ... values) {
        for (BigDecimal value : values) {
            state.doUpdate(new JsonPrimitive(value));
        }
    }
}
