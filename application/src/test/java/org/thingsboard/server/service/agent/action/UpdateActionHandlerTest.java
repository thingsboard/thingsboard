// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdateActionHandlerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private ProfileConfigResolver profileConfigResolver;
    @Mock
    private AgentAppProfileService profileService;

    private UpdateActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateActionHandler(profileConfigResolver, profileService);
    }

    private AgentAppProfile stubProfile(AgentAppProfileId profileId, String templateVersion) {
        AgentAppProfile profile = new AgentAppProfile(profileId);
        profile.setTemplateVersion(templateVersion);
        willReturn(profile).given(profileService).findProfileById(eq(TENANT_ID), eq(profileId));
        return profile;
    }

    @Test
    void getActionType_isUpdate() {
        assertThat(handler.getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void profileManaged_skipRefetch_keepsIncomingConfig_andRenames() {
        AgentApplication application = new AgentApplication();
        application.setApplicationProfileId(new AgentAppProfileId(UUID.randomUUID()));
        DockerComposeConfig incomingConfig = new DockerComposeConfig();

        AgentApplication incoming = new AgentApplication();
        incoming.setName("renamed");
        incoming.setConfig(incomingConfig);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);
        request.setSkipProfileRefetch(true);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        assertThat(application.getConfig()).isSameAs(incomingConfig);
        assertThat(application.getName()).isEqualTo("renamed");
        verifyNoInteractions(profileConfigResolver);
    }

    @Test
    void profileManaged_normal_reResolvesConfig_andRenames() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentApplication application = new AgentApplication();
        application.setApplicationProfileId(profileId);
        application.setTemplateVersion("1.0.0");
        stubProfile(profileId, "1.0.0");

        AgentApplication incoming = new AgentApplication();
        incoming.setName("renamed");

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);
        request.setSkipProfileRefetch(false);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        verify(profileConfigResolver).resolve(eq(TENANT_ID), eq(application));
        assertThat(application.getName()).isEqualTo("renamed");
    }

    @Test
    void profileManaged_profileTemplateDrifted_skipsRefetch_keepsIncomingConfig() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentApplication application = new AgentApplication();
        application.setApplicationProfileId(profileId);
        application.setTemplateVersion("1.0.0");
        stubProfile(profileId, "2.0.0");

        DockerComposeConfig incomingConfig = new DockerComposeConfig();
        AgentApplication incoming = new AgentApplication();
        incoming.setConfig(incomingConfig);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);
        request.setSkipProfileRefetch(false);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        assertThat(application.getConfig()).isSameAs(incomingConfig);
        assertThat(application.getTemplateVersion()).isEqualTo("1.0.0");
        verifyNoInteractions(profileConfigResolver);
    }

    @Test
    void nonProfileManaged_copiesNameAndConfig() {
        AgentApplication application = new AgentApplication();
        DockerComposeConfig incomingConfig = new DockerComposeConfig();

        AgentApplication incoming = new AgentApplication();
        incoming.setName("standalone");
        incoming.setConfig(incomingConfig);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        assertThat(application.getName()).isEqualTo("standalone");
        assertThat(application.getConfig()).isSameAs(incomingConfig);
        verifyNoInteractions(profileConfigResolver);
    }
}
