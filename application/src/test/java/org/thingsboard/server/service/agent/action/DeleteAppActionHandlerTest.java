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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteAppActionHandlerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private AgentAppEventService appEventService;

    private DeleteAppActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeleteAppActionHandler(appEventService);
    }

    @Test
    void getActionType_isDelete() {
        assertThat(handler.getActionType()).isEqualTo(AgentAppEventActionType.DELETE);
    }

    @Test
    void handle_deletesPendingEvents_andMarksPendingDeletion() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        AgentApplication application = new AgentApplication(appId);

        handler.handle(application, new AgentAppEventRequest(), new AgentAppActionContext(TENANT_ID));

        verify(appEventService).deleteAllPendingByApplicationId(appId);
        assertThat(application.isPendingDeletion()).isTrue();
    }
}
