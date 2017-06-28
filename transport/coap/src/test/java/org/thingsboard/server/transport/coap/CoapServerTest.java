/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.aware.SessionAwareMsg;
import org.thingsboard.server.common.msg.core.BasicGetAttributesResponse;
import org.thingsboard.server.common.msg.core.BasicRequest;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.common.msg.kv.BasicAttributeKVMsg;
import org.thingsboard.server.common.msg.session.*;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthResult;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@Slf4j
public class CoapServerTest {

    private static final int TEST_PORT = 5555;
    private static final String TELEMETRY_POST_MESSAGE = "[{\"key1\":\"value1\"}]";
    private static final String TEST_ATTRIBUTES_RESPONSE = "{\"key1\":\"value1\",\"key2\":42}";
    private static final String DEVICE1_TOKEN = "Device1Token";

    @Configuration
    public static class EchoCoapServerITConfiguration extends CoapServerTestConfiguration {

        @Bean
        public static DeviceAuthService authService() {
            return new DeviceAuthService() {

                private final DeviceId devId = new DeviceId(UUID.randomUUID());

                @Override
                public DeviceAuthResult process(DeviceCredentialsFilter credentials) {
                    if (credentials != null && credentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
                        DeviceTokenCredentials tokenCredentials = (DeviceTokenCredentials) credentials;
                        if (tokenCredentials.getCredentialsId().equals(DEVICE1_TOKEN)) {
                            return DeviceAuthResult.of(devId);
                        }
                    }
                    return DeviceAuthResult.of("Credentials are invalid!");
                }

                @Override
                public Optional<Device> findDeviceById(DeviceId deviceId) {
                    if (deviceId.equals(devId)) {
                        Device dev = new Device();
                        dev.setId(devId);
                        dev.setTenantId(new TenantId(UUID.randomUUID()));
                        dev.setCustomerId(new CustomerId(UUID.randomUUID()));
                        return Optional.of(dev);
                    } else {
                        return Optional.empty();
                    }
                }
            };
        }

        @Bean
        public static SessionMsgProcessor sessionMsgProcessor() {
            return new SessionMsgProcessor() {

                @Override
                public void process(SessionAwareMsg toActorMsg) {
                    if (toActorMsg instanceof ToDeviceActorSessionMsg) {
                        AdaptorToSessionActorMsg sessionMsg = ((ToDeviceActorSessionMsg) toActorMsg).getSessionMsg();
                        try {
                            FromDeviceMsg deviceMsg = sessionMsg.getMsg();
                            ToDeviceMsg toDeviceMsg = null;
                            if (deviceMsg.getMsgType() == MsgType.POST_TELEMETRY_REQUEST) {
                                toDeviceMsg = BasicStatusCodeResponse.onSuccess(deviceMsg.getMsgType(), BasicRequest.DEFAULT_REQUEST_ID);
                            } else if (deviceMsg.getMsgType() == MsgType.GET_ATTRIBUTES_REQUEST) {
                                List<AttributeKvEntry> data = new ArrayList<>();
                                data.add(new BaseAttributeKvEntry(new StringDataEntry("key1", "value1"), System.currentTimeMillis()));
                                data.add(new BaseAttributeKvEntry(new LongDataEntry("key2", 42L), System.currentTimeMillis()));
                                BasicAttributeKVMsg kv = BasicAttributeKVMsg.fromClient(data);
                                toDeviceMsg = BasicGetAttributesResponse.onSuccess(deviceMsg.getMsgType(), BasicRequest.DEFAULT_REQUEST_ID, kv);
                            }
                            if (toDeviceMsg != null) {
                                sessionMsg.getSessionContext().onMsg(new BasicSessionActorToAdaptorMsg(sessionMsg.getSessionContext(), toDeviceMsg));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }
    }

    @Autowired
    private CoapTransportService service;

    @Before
    public void beforeTest() {
        log.info("Service info: {}", service.toString());
    }

    @Test
    public void testBadJsonTelemetryPostRequest() {
        CoapClient client = new CoapClient(getBaseTestUrl() + DEVICE1_TOKEN + "/" + FeatureType.TELEMETRY.name().toLowerCase());
        CoapResponse response = client.setTimeout(6000).post("test", MediaTypeRegistry.APPLICATION_JSON);
        Assert.assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        log.info("Response: {}, {}", response.getCode(), response.getResponseText());
    }

    @Test
    public void testNoCredentialsPostRequest() {
        CoapClient client = new CoapClient(getBaseTestUrl());
        CoapResponse response = client.setTimeout(6000).post(TELEMETRY_POST_MESSAGE, MediaTypeRegistry.APPLICATION_JSON);
        Assert.assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        log.info("Response: {}, {}", response.getCode(), response.getResponseText());
    }

    @Test
    public void testValidJsonTelemetryPostRequest() {
        CoapClient client = new CoapClient(getBaseTestUrl() + DEVICE1_TOKEN + "/" + FeatureType.TELEMETRY.name().toLowerCase());
        CoapResponse response = client.setTimeout(6000).post(TELEMETRY_POST_MESSAGE, MediaTypeRegistry.APPLICATION_JSON);
        Assert.assertEquals(ResponseCode.CREATED, response.getCode());
        log.info("Response: {}, {}", response.getCode(), response.getResponseText());
    }

    @Test
    public void testNoCredentialsAttributesGetRequest() {
        CoapClient client = new CoapClient("coap://localhost:5555/api/v1?keys=key1,key2");
        CoapResponse response = client.setTimeout(6000).get();
        Assert.assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
    }

    @Test
    public void testNoKeysAttributesGetRequest() {
        CoapClient client = new CoapClient(getBaseTestUrl() + DEVICE1_TOKEN + "/" + FeatureType.ATTRIBUTES.name().toLowerCase() + "?data=key1,key2");
        CoapResponse response = client.setTimeout(6000).get();
        Assert.assertEquals(ResponseCode.CONTENT, response.getCode());
    }

    @Test
    public void testValidAttributesGetRequest() {
        CoapClient client = new CoapClient(getBaseTestUrl() + DEVICE1_TOKEN + "/" + FeatureType.ATTRIBUTES.name().toLowerCase() + "?clientKeys=key1,key2");
        CoapResponse response = client.setTimeout(6000).get();
        Assert.assertEquals(ResponseCode.CONTENT, response.getCode());
        Assert.assertEquals(TEST_ATTRIBUTES_RESPONSE, response.getResponseText());
        log.info("Response: {}, {}", response.getCode(), response.getResponseText());
    }

    private String getBaseTestUrl() {
        return "coap://localhost:" + TEST_PORT + "/api/v1/";
    }

}
