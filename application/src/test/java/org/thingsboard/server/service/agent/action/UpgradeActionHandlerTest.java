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
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpgradeActionHandlerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private ProfileConfigResolver profileConfigResolver;

    private UpgradeActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpgradeActionHandler(profileConfigResolver);
    }

    @Test
    void getActionType_isUpgrade() {
        assertThat(handler.getActionType()).isEqualTo(AgentAppEventActionType.UPGRADE);
    }

    @Test
    void nullIncomingApplication_throws() {
        AgentApplication application = new AgentApplication();
        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(null);

        assertThatThrownBy(() -> handler.handle(application, request, new AgentAppActionContext(TENANT_ID)))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("must include an application");
    }

    @Test
    void profileManaged_setsDesiredTemplateIdFromResolvedProfile() {
        AgentApplication application = new AgentApplication();
        application.setApplicationProfileId(new AgentAppProfileId(UUID.randomUUID()));

        String profileTemplateVersion = "1.0.0";
        AgentAppProfile resolvedProfile = new AgentAppProfile();
        resolvedProfile.setTemplateVersion(profileTemplateVersion);
        when(profileConfigResolver.resolve(eq(TENANT_ID), eq(application))).thenReturn(resolvedProfile);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(new AgentApplication());

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        verify(profileConfigResolver).resolve(eq(TENANT_ID), eq(application));
        assertThat(application.getDesiredTemplateVersion()).isEqualTo(profileTemplateVersion);
    }

    @Test
    void nonProfileManaged_copiesConfigAndDesiredTemplateId() {
        AgentApplication application = new AgentApplication();

        String incomingTemplateVersion = "1.0.0";
        DockerComposeConfig incomingConfig = new DockerComposeConfig();
        AgentApplication incoming = new AgentApplication();
        incoming.setConfig(incomingConfig);
        incoming.setTemplateVersion(incomingTemplateVersion);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        assertThat(application.getConfig()).isSameAs(incomingConfig);
        assertThat(application.getDesiredTemplateVersion()).isEqualTo(incomingTemplateVersion);
        verifyNoInteractions(profileConfigResolver);
    }
}
