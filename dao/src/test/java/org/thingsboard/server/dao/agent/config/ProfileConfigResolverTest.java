// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileConfigResolverTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentAppProfileId PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final String EDGE_IMAGE = "thingsboard/tb-edge-pe:3.8.0";

    @Mock
    private AgentAppProfileService profileService;

    private ProfileConfigResolver resolver;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        resolver = new ProfileConfigResolver(profileService);
    }

    @Test
    void resolve_nullApplicationProfileId_returnsNull() {
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);

        assertThat(resolver.resolve(TENANT_ID, app)).isNull();
        verifyNoInteractions(profileService);
    }

    @Test
    void resolve_profileNotFound_throws() {
        AgentApplication app = edgeApp(Map.of());
        when(profileService.findProfileById(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(TENANT_ID, app))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining(PROFILE_ID.getId().toString());
    }

    @Test
    void resolve_profileConfigNull_throws() {
        AgentApplication app = edgeApp(Map.of());
        AgentAppProfile profile = new AgentAppProfile();
        profile.setConfig(null);
        when(profileService.findProfileById(any(), any())).thenReturn(profile);

        assertThatThrownBy(() -> resolver.resolve(TENANT_ID, app))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void resolve_copiesProfileConfig_setsVersion_andReappliesIncomingCreds() {
        AgentApplication app = edgeApp(Map.of(
                "CLOUD_ROUTING_KEY", "app-key",
                "CLOUD_ROUTING_SECRET", "app-secret"));
        DockerComposeConfig profileConfig = edgeConfig(Map.of(
                "CLOUD_ROUTING_KEY", "PLACEHOLDER",
                "CLOUD_ROUTING_SECRET", "PLACEHOLDER"));
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(TENANT_ID);
        profile.setConfig(profileConfig);
        profile.setVersion(7L);
        when(profileService.findProfileById(any(), any())).thenReturn(profile);

        AgentAppProfile returned = resolver.resolve(TENANT_ID, app);

        assertThat(returned).isSameAs(profile);
        assertThat(app.getProfileConfigVersion()).isEqualTo(7L);
        // config is a copy of the profile config, not the same instance
        assertThat(app.getConfig()).isNotSameAs(profileConfig);
        // incoming credentials were re-applied onto the resolved (profile) compose
        assertThat(envValue(app, "CLOUD_ROUTING_KEY")).isEqualTo("app-key");
        assertThat(envValue(app, "CLOUD_ROUTING_SECRET")).isEqualTo("app-secret");
    }

    @Test
    void resolve_genericAppType_noCredPropagation() {
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.GENERIC);
        app.setApplicationProfileId(PROFILE_ID);
        app.setConfig(edgeConfig(Map.of("CLOUD_ROUTING_KEY", "ignored")));
        DockerComposeConfig profileConfig = edgeConfig(Map.of("CLOUD_ROUTING_KEY", "PLACEHOLDER"));
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(TENANT_ID);
        profile.setConfig(profileConfig);
        profile.setVersion(3L);
        when(profileService.findProfileById(any(), any())).thenReturn(profile);

        AgentAppProfile returned = resolver.resolve(TENANT_ID, app);

        assertThat(returned).isSameAs(profile);
        assertThat(app.getProfileConfigVersion()).isEqualTo(3L);
        // GENERIC has no credential env keys -> profile placeholder remains untouched
        assertThat(envValue(app, "CLOUD_ROUTING_KEY")).isEqualTo("PLACEHOLDER");
    }

    // ==================== fixtures ====================

    private AgentApplication edgeApp(Map<String, String> env) {
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);
        app.setApplicationProfileId(PROFILE_ID);
        app.setConfig(edgeConfig(env));
        return app;
    }

    private DockerComposeConfig edgeConfig(Map<String, String> env) {
        ObjectNode environment = JacksonUtil.newObjectNode();
        env.forEach(environment::put);

        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", EDGE_IMAGE);
        service.set("environment", environment);

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);

        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }

    private static String envValue(AgentApplication app, String key) {
        return DockerComposeUtils.getEnvVariable(
                ((DockerComposeConfig) app.getConfig()).getCompose(),
                AgentApplicationType.EDGE.getMainImagePattern(),
                key);
    }
}
