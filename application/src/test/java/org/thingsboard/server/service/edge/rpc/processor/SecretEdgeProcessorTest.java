// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.processor.secret.SecretEdgeProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecretEdgeProcessorTest {

    @Mock
    private EdgeContextComponent edgeCtx;
    @Mock
    private SecretService secretService;

    private final SecretEdgeProcessor processor = new SecretEdgeProcessor();
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(processor, "edgeCtx", edgeCtx);
        when(edgeCtx.getSecretService()).thenReturn(secretService);
    }

    @Test
    public void customerSecretIsNotSentToOlderEdge() {
        Secret secret = stubSecret(new CustomerId(UUID.randomUUID()));

        assertThat(processor.convertEdgeEventToDownlink(secretEvent(secret, EdgeEventActionType.ADDED), EdgeVersion.V_4_4_0)).isNull();
        assertThat(processor.convertEdgeEventToDownlink(secretEvent(secret, EdgeEventActionType.UPDATED), EdgeVersion.V_4_4_0)).isNull();
        assertThat(processor.convertEdgeEventToDownlink(secretEvent(secret, EdgeEventActionType.DELETED), EdgeVersion.V_4_4_0)
                .getSecretUpdateMsgCount()).isEqualTo(1);
        assertThat(processor.convertEdgeEventToDownlink(secretEvent(secret, EdgeEventActionType.ADDED), EdgeVersion.V_LATEST)
                .getSecretUpdateMsgCount()).isEqualTo(1);
    }

    @Test
    public void tenantSecretIsSentToOlderEdge() {
        Secret secret = stubSecret(new CustomerId(EntityId.NULL_UUID));

        assertThat(processor.convertEdgeEventToDownlink(secretEvent(secret, EdgeEventActionType.ADDED), EdgeVersion.V_4_4_0)
                .getSecretUpdateMsgCount()).isEqualTo(1);
    }

    private Secret stubSecret(CustomerId customerId) {
        Secret secret = new Secret();
        secret.setId(new SecretId(UUID.randomUUID()));
        secret.setTenantId(tenantId);
        secret.setCustomerId(customerId);
        secret.setName("secret");
        secret.setType(SecretType.TEXT);
        when(secretService.findSecretById(tenantId, secret.getId())).thenReturn(secret);
        return secret;
    }

    private EdgeEvent secretEvent(Secret secret, EdgeEventActionType action) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setType(EdgeEventType.SECRET);
        edgeEvent.setAction(action);
        edgeEvent.setEntityId(secret.getUuidId());
        return edgeEvent;
    }

}
