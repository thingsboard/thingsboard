// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppArgumentReferenceValidatorTest {

    @Mock
    private AgentAppArgumentSourceResolver sourceEntityResolver;
    @Mock
    private EntityServiceRegistry entityServiceRegistry;
    @Mock
    private EntityDaoService entityDaoService;

    @InjectMocks
    private AgentAppArgumentReferenceValidator validator;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final AgentApplication application = new AgentApplication();

    @Test
    void passesWhenReferencedEntityExists() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Optional<HasId<?>> found = Optional.of(new Device(deviceId));
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(found);

        assertThatNoException().isThrownBy(() ->
                validator.validate(tenantId, configWith(deviceId), null));
    }

    @Test
    void rejectsWhenReferencedEntityMissingOrFromOtherTenant() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(tenantId, configWith(deviceId), null))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent");
    }

    @Test
    void rejectsEntityTypeWithNoRegisteredService() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE))
                .thenThrow(new IllegalArgumentException("Unsupported entity type: DEVICE"));

        assertThatThrownBy(() -> validator.validate(tenantId, configWith(deviceId), null))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Custom argument 'ref' references an unsupported entity type DEVICE!");
    }

    @Test
    void rejectsEntityBelongingToAnotherTenant() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device foreign = new Device(deviceId);
        foreign.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        Optional<HasId<?>> found = Optional.of(foreign);
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(found);

        assertThatThrownBy(() -> validator.validate(tenantId, configWith(deviceId), null))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent");
    }

    @Test
    void skipsContextDerivedSourcesForProfile() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.AGENT, null)));

        assertThatNoException().isThrownBy(() -> validator.validate(tenantId, config, null));
    }

    @Test
    void passesWhenContextDerivedSourceResolvesToExistingEntity() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Optional<HasId<?>> found = Optional.of(new Device(deviceId));
        when(sourceEntityResolver.resolveContextSource(any(), any(), eq(AgentAppArgumentSource.RELATED_ENTITY))).thenReturn(deviceId);
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(found);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.RELATED_ENTITY, null)));

        assertThatNoException().isThrownBy(() -> validator.validate(tenantId, config, application));
    }

    @Test
    void rejectsWhenContextDerivedResolvedEntityMissing() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        when(sourceEntityResolver.resolveContextSource(any(), any(), eq(AgentAppArgumentSource.RELATED_ENTITY))).thenReturn(deviceId);
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(Optional.empty());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.RELATED_ENTITY, null)));

        assertThatThrownBy(() -> validator.validate(tenantId, config, application))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent");
    }

    @Test
    void rejectsUnresolvableContextDerivedSourceWithoutDefault() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.RELATED_ENTITY, null)));

        assertThatThrownBy(() -> validator.validate(tenantId, config, application))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("could not resolve");
    }

    @Test
    void passesUnresolvableContextDerivedSourceWhenDefaultPresent() {
        AgentAppArgument arg = argument(AgentAppArgumentSource.RELATED_ENTITY, null);
        arg.setDefaultValue("fallback");
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(arg));

        assertThatNoException().isThrownBy(() -> validator.validate(tenantId, config, application));
    }

    private DockerComposeConfig configWith(DeviceId deviceId) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.DEVICE, deviceId)));
        return config;
    }

    private AgentAppArgument argument(AgentAppArgumentSource source, DeviceId entityId) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName("ref");
        argument.setSourceType(source);
        argument.setSourceEntityId(entityId);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey("k");
        return argument;
    }

}
