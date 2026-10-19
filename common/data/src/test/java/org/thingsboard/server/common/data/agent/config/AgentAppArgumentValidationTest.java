// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentAppArgumentValidationTest {

    @Test
    void acceptsValidArguments() {
        DockerComposeConfig config = configWith(
                argument("device_uuid", AgentAppArgumentSource.RELATED_ENTITY, AgentAppArgumentValueType.ATTRIBUTE, "id"));
        assertThatNoException().isThrownBy(config::validateArguments);
    }

    @Test
    void rejectsDuplicateNames() {
        DockerComposeConfig config = configWith(
                argument("dup", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "a"),
                argument("dup", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "b"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void rejectsInvalidName() {
        DockerComposeConfig config = configWith(
                argument("bad name", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "a"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void rejectsMissingKey() {
        DockerComposeConfig config = configWith(
                argument("name", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, null));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("key");
    }

    @Test
    void acceptsConcreteSourceWithMatchingEntityId() {
        AgentAppArgument argument = argument("dev", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE, "k");
        argument.setSourceEntityId(new DeviceId(UUID.randomUUID()));
        DockerComposeConfig config = configWith(argument);
        assertThatNoException().isThrownBy(config::validateArguments);
    }

    @Test
    void rejectsConcreteSourceWithoutEntityId() {
        DockerComposeConfig config = configWith(
                argument("dev", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE, "k"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("source entity id");
    }

    @Test
    void rejectsEntityIdTypeMismatch() {
        AgentAppArgument argument = argument("dev", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE, "k");
        argument.setSourceEntityId(new AssetId(UUID.randomUUID()));
        DockerComposeConfig config = configWith(argument);
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("type must be");
    }

    @Test
    void rejectsNullName() {
        DockerComposeConfig config = configWith(
                argument(null, AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "a"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Custom argument name must be specified!");
    }

    @Test
    void rejectsBlankName() {
        DockerComposeConfig config = configWith(
                argument("   ", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "a"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Custom argument name must be specified!");
    }

    @Test
    void rejectsMissingSourceType() {
        DockerComposeConfig config = configWith(
                argument("name", null, AgentAppArgumentValueType.ATTRIBUTE, "a"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Custom argument 'name' source type must be specified!");
    }

    @Test
    void rejectsMissingValueType() {
        DockerComposeConfig config = configWith(
                argument("name", AgentAppArgumentSource.AGENT, null, "a"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Custom argument 'name' value type must be specified!");
    }

    @Test
    void rejectsBlankKey() {
        DockerComposeConfig config = configWith(
                argument("name", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "  "));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Custom argument 'name' key must be specified!");
    }

    @Test
    void acceptsNullAndEmptyArgumentList() {
        assertThatNoException().isThrownBy(new DockerComposeConfig()::validateArguments);
        assertThatNoException().isThrownBy(configWith()::validateArguments);
    }

    private DockerComposeConfig configWith(AgentAppArgument... arguments) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(arguments));
        return config;
    }

    private AgentAppArgument argument(String name, AgentAppArgumentSource source,
                                      AgentAppArgumentValueType valueType, String key) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setSourceType(source);
        argument.setValueType(valueType);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey(key);
        return argument;
    }

}
