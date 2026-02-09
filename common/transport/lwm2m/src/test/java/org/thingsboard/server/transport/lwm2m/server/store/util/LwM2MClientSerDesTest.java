/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.server.registration.DefaultRegistrationDataExtractor;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LwM2MClientSerDesTest {

    @Test
    public void serializeDeserialize() throws Exception {
        LwM2mClient client = new LwM2mClient("nodeId", "endpoint");

        TransportDeviceInfo tdi = new TransportDeviceInfo();
        tdi.setPowerMode(PowerMode.PSM);
        tdi.setPsmActivityTimer(10000L);
        tdi.setPagingTransmissionWindow(2000L);
        tdi.setEdrxCycle(3000L);
        tdi.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        tdi.setCustomerId(new CustomerId(UUID.randomUUID()));
        tdi.setDeviceId(new DeviceId(UUID.randomUUID()));
        tdi.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        tdi.setDeviceName("testDevice");
        tdi.setDeviceType("testType");
        ValidateDeviceCredentialsResponse credentialsResponse = ValidateDeviceCredentialsResponse.builder()
                .deviceInfo(tdi)
                .build();

        client.init(credentialsResponse, UUID.randomUUID());

        AttributeSet attrs = new AttributeSet( //
                new ResourceTypeAttribute("oma.lwm2m"));

        Link[] objs = new Link[]{new Link("/15", attrs), new Link("/17")};

        RegistrationDataExtractor.RegistrationData dataFromObjectLinks = new DefaultRegistrationDataExtractor().extractDataFromObjectLinks(objs,
                LwM2mVersion.V1_0);

        Registration registration = new Registration
                .Builder("test", "endpoint", new IpPeer(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 1000)),
                EndpointUriUtil.createUri("coap://localhost:5685"))
                .objectLinks(objs)
                .rootPath(dataFromObjectLinks.getAlternatePath())
                .supportedContentFormats(dataFromObjectLinks.getSupportedContentFormats())
                .supportedObjects(dataFromObjectLinks.getSupportedObjects())
                .availableInstances(dataFromObjectLinks.getAvailableInstances())
                .build();

        client.setRegistration(registration);
        client.setState(LwM2MClientState.REGISTERED);
        client.getSharedAttributes().put("key1", TransportProtos.TsKvProto.newBuilder().setTs(0).setKv(TransportProtos.KeyValueProto.newBuilder().setStringV("test").build()).build());
        client.getSharedAttributes().put("key2", TransportProtos.TsKvProto.newBuilder().setTs(1).setKv(TransportProtos.KeyValueProto.newBuilder().setDoubleV(1.02).build()).build());

        TransportResourceCache resourceCache = mock(TransportResourceCache.class);
        LwM2mTransportContext context = mock(LwM2mTransportContext.class);
        LwM2mClientContext clientContext = mock(LwM2mClientContext.class);

        var provider = new LwM2mVersionedModelProvider(clientContext, new LwM2mTransportServerHelper(context), context);

        TbResource resource15 = new TbResource();
        resource15.setData(Files.readAllBytes(Path.of(this.getClass().getClassLoader().getResource("15.xml").toURI())));
        TbResource resource17 = new TbResource();
        resource17.setData(Files.readAllBytes(Path.of(this.getClass().getClassLoader().getResource("17.xml").toURI())));

        when(resourceCache.get(any(), any(), eq("15_1.0"))).thenReturn(Optional.of(resource15));
        when(resourceCache.get(any(), any(), eq("17_1.0"))).thenReturn(Optional.of(resource17));
        when(context.getTransportResourceCache()).thenReturn(resourceCache);
        when(clientContext.getClientByEndpoint(any())).thenReturn(client);

        LwM2mResource singleResource = LwM2mSingleResource.newStringResource(15, "testValue");
        LwM2mResource multipleResource = LwM2mMultipleResource.newStringResource(17, Map.of(0, "testValue", 1, "testValue"));
        client.saveResourceValue("/15_1.0/0/0", singleResource, provider, WriteRequest.Mode.UPDATE);
        client.saveResourceValue("/17_1.0/0/0", multipleResource, provider, WriteRequest.Mode.UPDATE);

        byte[] bytes = LwM2MClientSerDes.serialize(client);
        assertNotNull(bytes);

        LwM2mClient desClient = LwM2MClientSerDes.deserialize(bytes);

        assertEquals(client.getNodeId(), desClient.getNodeId());
        assertEquals(client.getEndpoint(), desClient.getEndpoint());
        assertEquals(client.getSharedAttributes(), desClient.getSharedAttributes());
        assertEquals(client.getKeyTsLatestMap(), desClient.getKeyTsLatestMap());
        assertEquals(client.getTenantId(), desClient.getTenantId());
        assertEquals(client.getProfileId(), desClient.getProfileId());
        assertEquals(client.getDeviceId(), desClient.getDeviceId());
        assertEquals(client.getState(), desClient.getState());
        assertEquals(client.getSession(), desClient.getSession());
        assertEquals(client.getPowerMode(), desClient.getPowerMode());
        assertEquals(client.getPsmActivityTimer(), desClient.getPsmActivityTimer());
        assertEquals(client.getPagingTransmissionWindow(), desClient.getPagingTransmissionWindow());
        assertEquals(client.getEdrxCycle(), desClient.getEdrxCycle());
        assertEquals(client.getRegistration(), desClient.getRegistration());
        assertEquals(client.isAsleep(), desClient.isAsleep());
        assertEquals(client.getLastUplinkTime(), desClient.getLastUplinkTime());
        assertEquals(client.getSleepTask(), desClient.getSleepTask());
        assertEquals(client.getClientSupportContentFormats(), desClient.getClientSupportContentFormats());
        assertEquals(client.getDefaultContentFormat(), desClient.getDefaultContentFormat());
        assertEquals(client.getRetryAttempts().get(), desClient.getRetryAttempts().get());
        assertEquals(client.getLastSentRpcId(), desClient.getLastSentRpcId());

        Map<String, ResourceValue> expectedResources = client.getResources();
        Map<String, ResourceValue> actualResources = desClient.getResources();
        assertNotNull(actualResources);
        assertEquals(expectedResources.size(), actualResources.size());
        for (Entry entry : expectedResources.entrySet()) {
            LwM2mPath expectedPathId = client.getLwM2mPathFromString(entry.getKey().toString());
            String actualOld = actualResources.get(String.valueOf(expectedPathId.getObjectId())).toString();
            String actual = actualOld.replaceAll("\"", "");
            assertEquals(entry.getValue().toString(), actual);
        }
    }
}