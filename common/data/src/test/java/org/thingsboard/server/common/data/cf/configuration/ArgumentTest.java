// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArgumentTest {

    @Test
    void validateShouldReturnFalseIfDynamicSourceConfigurationIsNull() {
        var argument = new Argument();
        assertThat(argument.hasDynamicSource()).isFalse();
    }

    @Test
    void validateWhenRelationQuerySourceConfigurationIsNotNull() {
        var argument = new Argument();
        argument.setRefDynamicSourceConfiguration(new RelationPathQueryDynamicSourceConfiguration());
        assertThat(argument.hasDynamicSource()).isTrue();
        assertThat(argument.hasRelationQuerySource()).isTrue();
        assertThat(argument.hasOwnerSource()).isFalse();
    }

    @Test
    void validateWhenCurrentOwnerSourceConfigurationIsNotNull() {
        var argument = new Argument();
        argument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        assertThat(argument.hasDynamicSource()).isTrue();
        assertThat(argument.hasOwnerSource()).isTrue();
        assertThat(argument.hasRelationQuerySource()).isFalse();
    }

}
