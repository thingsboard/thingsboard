/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.net.InetSocketAddress;
import java.util.UUID;

public class LwM2MClientSerDesTest {

    @Test
    public void serializeDeserialize() {
        LwM2mClient client = new LwM2mClient("nodeId", "testEndpoint");

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

        Registration registration =
                new Registration.Builder("test", "testEndpoint", Identity
                        .unsecure(new InetSocketAddress(1000)))
                        .supportedContentFormats()
                        .objectLinks(new Link[]{new Link("/")})
                        .build();

        client.setRegistration(registration);
        client.setState(LwM2MClientState.REGISTERED);

        client.getSharedAttributes().put("key1", TransportProtos.TsKvProto.newBuilder().setTs(0).setKv(TransportProtos.KeyValueProto.newBuilder().setStringV("test").build()).build());
        client.getSharedAttributes().put("key2", TransportProtos.TsKvProto.newBuilder().setTs(1).setKv(TransportProtos.KeyValueProto.newBuilder().setDoubleV(1.02).build()).build());

        byte[] bytes = LwM2MClientSerDes.serialize(client);
        Assert.assertNotNull(bytes);

        LwM2mClient desClient = LwM2MClientSerDes.deserialize(bytes);

        Assert.assertEquals(client.getNodeId(), desClient.getNodeId());
        Assert.assertEquals(client.getEndpoint(), desClient.getEndpoint());
        Assert.assertEquals(client.getResources(), desClient.getResources());
        Assert.assertEquals(client.getSharedAttributes(), desClient.getSharedAttributes());
        Assert.assertEquals(client.getKeyTsLatestMap(), desClient.getKeyTsLatestMap());
        Assert.assertEquals(client.getTenantId(), desClient.getTenantId());
        Assert.assertEquals(client.getProfileId(), desClient.getProfileId());
        Assert.assertEquals(client.getDeviceId(), desClient.getDeviceId());
        Assert.assertEquals(client.getState(), desClient.getState());
        Assert.assertEquals(client.getSession(), desClient.getSession());
        Assert.assertEquals(client.getPowerMode(), desClient.getPowerMode());
        Assert.assertEquals(client.getPsmActivityTimer(), desClient.getPsmActivityTimer());
        Assert.assertEquals(client.getPagingTransmissionWindow(), desClient.getPagingTransmissionWindow());
        Assert.assertEquals(client.getEdrxCycle(), desClient.getEdrxCycle());
        Assert.assertEquals(client.getRegistration(), desClient.getRegistration());
        Assert.assertEquals(client.isAsleep(), desClient.isAsleep());
        Assert.assertEquals(client.getLastUplinkTime(), desClient.getLastUplinkTime());
        Assert.assertEquals(client.getSleepTask(), desClient.getSleepTask());
        Assert.assertEquals(client.getClientSupportContentFormats(), desClient.getClientSupportContentFormats());
        Assert.assertEquals(client.getDefaultContentFormat(), desClient.getDefaultContentFormat());
        Assert.assertEquals(client.getRetryAttempts().get(), desClient.getRetryAttempts().get());
        Assert.assertEquals(client.getLastSentRpcId(), desClient.getLastSentRpcId());
    }
}