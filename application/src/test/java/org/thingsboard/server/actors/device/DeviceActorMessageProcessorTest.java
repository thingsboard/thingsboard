/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.device;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.transport.TbCoreToTransportService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeviceActorMessageProcessorTest {

    public static final int MAX_CONCURRENT_SESSIONS_PER_DEVICE = 10;
    ActorSystemContext systemContext;
    AttributesService attributesService;
    TbCoreToTransportService transportService;
    DeviceService deviceService;
    TenantId tenantId = TenantId.SYS_TENANT_ID;
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");

    DeviceActorMessageProcessor processor;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        deviceService = mock(DeviceService.class);
        attributesService = mock(AttributesService.class);
        transportService = mock(TbCoreToTransportService.class);

        given(systemContext.getMaxConcurrentSessionsPerDevice()).willReturn(10);
        given(systemContext.getDeviceService()).willReturn(deviceService);
        given(systemContext.getAttributesService()).willReturn(attributesService);
        given(systemContext.getTbCoreToTransportService()).willReturn(transportService);

        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    @Test
    public void givenSystemContext_whenNewInstance_thenVerifySessionMapMaxSize() {
        assertThat(processor.sessions, instanceOf(LinkedHashMapRemoveEldest.class));
        assertThat(processor.sessions.getMaxEntries(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.sessions.getRemovalConsumer(), notNullValue());
    }

    @Test
    public void givenFullSessionMap_whenSessionOverflow_thenShouldDeleteAttributeAndRPCSubscriptions() {
        //givenFullSessionMap
        for (int i = 0; i < MAX_CONCURRENT_SESSIONS_PER_DEVICE; i++) {
            UUID sessionID = UUID.randomUUID();
            processor.sessions.put(sessionID, Mockito.mock(SessionInfoMetaData.class, RETURNS_DEEP_STUBS));
            processor.attributeSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
            processor.rpcSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
        }
        assertThat(processor.sessions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.attributeSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.rpcSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));

        //add one more
        processor.sessions.put(UUID.randomUUID(), Mockito.mock(SessionInfoMetaData.class));

        assertThat(processor.sessions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.attributeSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE-1));
        assertThat(processor.rpcSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE-1));

    }

    @Test
    public void testHandleGetAttributesRequest_Successful() {
        // Arrange
        List<AttributeKvEntry> clientAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("clientKey", "clientVal"), System.currentTimeMillis())
        );
        List<AttributeKvEntry> sharedAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sharedKey", "sharedVal"), System.currentTimeMillis())
        );

        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(clientAttr);
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(sharedAttr);
        given(attributesService.find(any(), any(), eq(org.thingsboard.server.common.data.AttributeScope.CLIENT_SCOPE),
                ArgumentMatchers.<Collection<String>>any())).willReturn(clientFuture);

        given(attributesService.find(any(), any(), eq(org.thingsboard.server.common.data.AttributeScope.SHARED_SCOPE),
                ArgumentMatchers.<Collection<String>>any())).willReturn(sharedFuture);

        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(42)
                .addClientAttributeNames("clientKey")
                .addSharedAttributeNames("sharedKey")
                .setAddClient(true)
                .setAddShared(true)
                .build();
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("node-1")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("node-1"), captor.capture());

        TransportProtos.ToTransportMsg msg = captor.getValue();
        assertThat(msg.hasGetAttributesResponse(), is(true));

        TransportProtos.GetAttributeResponseMsg response = msg.getGetAttributesResponse();
        assertThat(response.getRequestId(), is(42));
        assertThat(response.getClientAttributeListCount(), is(1));
        assertThat(response.getSharedAttributeListCount(), is(1));
        assertThat(response.getIsMultipleAttributesRequest(), is(true));
    }

    @Test
    public void testHandleGetAttributesRequest_Error() {
        // Arrange
        RuntimeException ex = new RuntimeException("Attr error");
        ListenableFuture<List<AttributeKvEntry>> errorFuture = Futures.immediateFailedFuture(ex);
        given(attributesService.find(any(), any(), eq(org.thingsboard.server.common.data.AttributeScope.CLIENT_SCOPE),
                ArgumentMatchers.<Collection<String>>any())).willReturn(errorFuture);

        given(attributesService.find(any(), any(), eq(org.thingsboard.server.common.data.AttributeScope.SHARED_SCOPE),
                ArgumentMatchers.<Collection<String>>any())).willReturn(errorFuture);


        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(100)
                .addClientAttributeNames("errKey")
                .setAddClient(true)
                .setAddShared(false)
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("node-err")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("node-err"), captor.capture());

        TransportProtos.ToTransportMsg msg = captor.getValue();
        assertThat(msg.hasGetAttributesResponse(), is(true));

        TransportProtos.GetAttributeResponseMsg response = msg.getGetAttributesResponse();
        assertThat(response.getRequestId(), is(100));
        assertThat(response.getError(), containsString("Attr error"));
    }

    @Test
    public void testHandleGetAttributesRequest_BackwardCompatible_OldFormat() {
        // Arrange
        List<AttributeKvEntry> clientAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("clientKey", "clientVal"), System.currentTimeMillis())
        );
        List<AttributeKvEntry> sharedAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sharedKey", "sharedVal"), System.currentTimeMillis())
        );

        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(clientAttr);
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(sharedAttr);

        given(attributesService.find(any(), any(), eq(org.thingsboard.server.common.data.AttributeScope.CLIENT_SCOPE),
                ArgumentMatchers.<Collection<String>>any())).willReturn(clientFuture);

        given(attributesService.find(any(), any(), eq(org.thingsboard.server.common.data.AttributeScope.SHARED_SCOPE),
                ArgumentMatchers.<Collection<String>>any())).willReturn(sharedFuture);

        // Build request WITHOUT addClient and addShared fields
        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(7)
                .addClientAttributeNames("clientKey")
                .addSharedAttributeNames("sharedKey")
                .build(); // Note: no setAddClient() / setAddShared()

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("legacy-node")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("legacy-node"), captor.capture());

        TransportProtos.ToTransportMsg msg = captor.getValue();
        assertThat(msg.hasGetAttributesResponse(), is(true));

        TransportProtos.GetAttributeResponseMsg response = msg.getGetAttributesResponse();
        assertThat(response.getRequestId(), is(7));
        assertThat(response.getClientAttributeListCount(), is(1));
        assertThat(response.getSharedAttributeListCount(), is(1));
    }
    @Test
    public void testHandleGetAttributesRequest_AddSharedOnly() {
        // Arrange
        List<AttributeKvEntry> sharedAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sharedKey", "sharedVal"), System.currentTimeMillis())
        );
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(sharedAttr);
        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(List.of());
        given(attributesService.find(any(), any(), eq(AttributeScope.SHARED_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(sharedFuture);
        given(attributesService.find(any(), any(), eq(AttributeScope.CLIENT_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(clientFuture);


        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(500)
                .setOnlyShared(false)
                .setAddClient(false)
                .setAddShared(true)
                .addClientAttributeNames("clientKeyShouldBeIgnored")
                .addSharedAttributeNames("sharedKey")
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("shared-only-flag")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("shared-only-flag"), captor.capture());

        TransportProtos.GetAttributeResponseMsg response = captor.getValue().getGetAttributesResponse();
        assertThat(response.getRequestId(), is(500));
        assertThat(response.getSharedAttributeListCount(), is(1));
        assertThat(response.getClientAttributeListCount(), is(0));
    }

    @Test
    public void testHandleGetAttributesRequest_AddClientOnly() {
        // Arrange
        List<AttributeKvEntry> clientAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("clientKey", "clientVal"), System.currentTimeMillis())
        );
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(List.of());
        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(clientAttr);
        given(attributesService.find(any(), any(), eq(AttributeScope.CLIENT_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(clientFuture);
        given(attributesService.find(any(), any(), eq(AttributeScope.SHARED_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(sharedFuture);

        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(501)
                .setOnlyShared(false)
                .setAddClient(true)
                .setAddShared(false)
                .addClientAttributeNames("clientKey")
                .addSharedAttributeNames("sharedKeyShouldBeIgnored")
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("client-only-flag")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("client-only-flag"), captor.capture());

        TransportProtos.GetAttributeResponseMsg response = captor.getValue().getGetAttributesResponse();
        assertThat(response.getRequestId(), is(501));
        assertThat(response.getSharedAttributeListCount(), is(0));
        assertThat(response.getClientAttributeListCount(), is(1));
    }

    @Test
    public void testHandleGetAttributesRequest_OnlyShared_ImplicitAddFlags() {
        // Arrange
        List<AttributeKvEntry> sharedAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sharedKey", "sharedVal"), System.currentTimeMillis())
        );
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(sharedAttr);
        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(List.of());
        given(attributesService.find(any(), any(), eq(AttributeScope.SHARED_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(sharedFuture);
        given(attributesService.find(any(), any(), eq(AttributeScope.CLIENT_SCOPE), ArgumentMatchers.<Collection<String>>any()))
                .willReturn(clientFuture);

        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(502)
                .setOnlyShared(true)
                .addClientAttributeNames("clientKeyShouldBeIgnored")
                .addSharedAttributeNames("sharedKey")
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("only-shared-implicit")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("only-shared-implicit"), captor.capture());

        TransportProtos.GetAttributeResponseMsg response = captor.getValue().getGetAttributesResponse();
        assertThat(response.getRequestId(), is(502));
        assertThat(response.getSharedAttributeListCount(), is(1));
        assertThat(response.getClientAttributeListCount(), is(0));
    }

    @Test
    public void testHandleGetAttributesRequest_SkipBothScopes() {
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(List.of());
        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(List.of());
        given(attributesService.find(any(), any(), eq(AttributeScope.SHARED_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(sharedFuture);
        given(attributesService.find(any(), any(), eq(AttributeScope.CLIENT_SCOPE), ArgumentMatchers.<Collection<String>>any())).willReturn(clientFuture);
        // Arrange
        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(600)
                .setOnlyShared(false)
                .setAddClient(false)
                .setAddShared(false)
                .addClientAttributeNames("clientKey")
                .addSharedAttributeNames("sharedKey")
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("skip-both")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("skip-both"), captor.capture());

        TransportProtos.GetAttributeResponseMsg response = captor.getValue().getGetAttributesResponse();
        assertThat(response.getRequestId(), is(600));
        assertThat(response.getSharedAttributeListCount(), is(0));
        assertThat(response.getClientAttributeListCount(), is(0));
    }

    @Test
    public void testHandleGetAttributesRequest_AddSharedWithNoKeys_ReturnsAllShared() {
        // Arrange
        List<AttributeKvEntry> sharedAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("sharedKey1", "val1"), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new StringDataEntry("sharedKey2", "val2"), System.currentTimeMillis())
        );
        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(sharedAttr);
        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(List.of());
        given(attributesService.findAll(any(), any(), eq(AttributeScope.SHARED_SCOPE))).willReturn(sharedFuture);
        given(attributesService.findAll(any(), any(), eq(AttributeScope.CLIENT_SCOPE))).willReturn(clientFuture);
        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(700)
                .setAddShared(true)
                .setAddClient(false)
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("fetch-all-shared")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("fetch-all-shared"), captor.capture());

        TransportProtos.GetAttributeResponseMsg response = captor.getValue().getGetAttributesResponse();
        assertThat(response.getRequestId(), is(700));
        assertThat(response.getSharedAttributeListCount(), is(2));
        assertThat(response.getClientAttributeListCount(), is(0));
    }

    @Test
    public void testHandleGetAttributesRequest_AddClientWithNoKeys_ReturnsAllClient() {
        // Arrange
        List<AttributeKvEntry> clientAttr = List.of(
                new BaseAttributeKvEntry(new StringDataEntry("clientKey1", "val1"), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new StringDataEntry("clientKey2", "val2"), System.currentTimeMillis())
        );

        ListenableFuture<List<AttributeKvEntry>> sharedFuture = Futures.immediateFuture(List.of());
        ListenableFuture<List<AttributeKvEntry>> clientFuture = Futures.immediateFuture(clientAttr);
        given(attributesService.findAll(any(), any(), eq(AttributeScope.SHARED_SCOPE))).willReturn(sharedFuture);
        given(attributesService.findAll(any(), any(), eq(AttributeScope.CLIENT_SCOPE))).willReturn(clientFuture);

        TransportProtos.GetAttributeRequestMsg request = TransportProtos.GetAttributeRequestMsg.newBuilder()
                .setRequestId(701)
                .setAddClient(true)
                .setAddShared(false)
                .build();

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(UUID.randomUUID().getMostSignificantBits())
                .setSessionIdLSB(UUID.randomUUID().getLeastSignificantBits())
                .setNodeId("fetch-all-client")
                .build();

        // Act
        processor.handleGetAttributesRequest(sessionInfo, request);

        // Assert
        ArgumentCaptor<TransportProtos.ToTransportMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToTransportMsg.class);
        verify(transportService).process(eq("fetch-all-client"), captor.capture());

        TransportProtos.GetAttributeResponseMsg response = captor.getValue().getGetAttributesResponse();
        assertThat(response.getRequestId(), is(701));
        assertThat(response.getClientAttributeListCount(), is(2));
        assertThat(response.getSharedAttributeListCount(), is(0));
    }
}