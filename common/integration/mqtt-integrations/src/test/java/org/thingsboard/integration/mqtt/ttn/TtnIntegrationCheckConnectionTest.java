// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.ttn;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TtnIntegrationCheckConnectionTest {

    private NioEventLoopGroup eventLoopGroup;
    private ExecutorService executor;
    private IntegrationContext context;

    @Before
    public void setUp() {
        eventLoopGroup = new NioEventLoopGroup(1);
        executor = Executors.newSingleThreadExecutor();
        context = Mockito.mock(IntegrationContext.class);
        Mockito.when(context.getEventLoopGroup()).thenReturn(eventLoopGroup);
        Mockito.when(context.getExecutorService()).thenReturn(executor);
        Mockito.when(context.getIntegrationConnectTimeoutSec()).thenReturn(1);
        Mockito.when(context.getMqttClientRetransmissionMaxAttempts()).thenReturn(1);
        Mockito.when(context.getMqttClientRetransmissionInitialDelayMillis()).thenReturn(1000L);
        Mockito.when(context.getMqttClientRetransmissionJitterFactor()).thenReturn(0.1);
    }

    @After
    public void tearDown() {
        eventLoopGroup.shutdownGracefully();
        executor.shutdownNow();
    }

    // The check runs the same TTN setup as a running integration, which refuses empty credentials
    @Test
    public void checkRefusesEmptyCredentialsBeforeConnecting() {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        integration.setType(IntegrationType.TTN);
        integration.setConfiguration(JacksonUtil.toJsonNode("{\"clientConfiguration\":{\"host\":\"eu1\",\"port\":8883,"
                + "\"connectTimeoutSec\":1,\"ssl\":true,\"credentials\":{\"type\":\"basic\",\"username\":\"\",\"password\":\"\"}}}"));

        ThingsboardException e = Assert.assertThrows(ThingsboardException.class,
                () -> new TtnIntegration().doCheckConnection(integration, context));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Application Credentials"));
    }
}
