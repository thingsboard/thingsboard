// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SimpleCalculatedFieldConfigurationTest {

    @Test
    void validateShouldThrowWhenTsRollingArgumentIsUsed() {
        var cfg = new SimpleCalculatedFieldConfiguration();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("key", ArgumentType.TS_ROLLING, null));
        cfg.setArguments(Map.of("arg", argument));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculated field with type: '" + CalculatedFieldType.SIMPLE + "' doesn't support TS_ROLLING arguments.");
    }

    @ParameterizedTest
    @EnumSource(value = ArgumentType.class, names = {"ATTRIBUTE", "TS_LATEST"})
    void validateShouldPassWhenNonRollingArgumentIsUsed(ArgumentType argumentType) {
        var cfg = new SimpleCalculatedFieldConfiguration();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("key", argumentType, null));
        cfg.setArguments(Map.of("arg", argument));

        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

}
