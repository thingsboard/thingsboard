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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.junit.Assert;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.LOCATION;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.TEMPERATURE_SENSOR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resources;


@Slf4j
@Data
public class LwM2MTestClient {

    private final ScheduledExecutorService executor;
    private final String endpoint;
    private LeshanClient client;

    private Server lwm2mServer;
    private SimpleLwM2MDevice lwM2MDevice;
    private FwLwM2MDevice fwLwM2MDevice;
    private SwLwM2MDevice swLwM2MDevice;
    private LwM2mBinaryAppDataContainer lwM2MBinaryAppDataContainer;
    private LwM2MLocationParams locationParams;
    private LwM2mTemperatureSensor lwM2MTemperatureSensor;

    public void init(Security security, Configuration coapConfig, int port, boolean isRpc) throws InvalidDDFFileException, IOException {
        Assert.assertNull("client already initialized", client);
        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(LwM2MTestClient.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName), resourceName));
        }

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, lwm2mServer = new Server(123, 300));
        initializer.setInstancesForObject(DEVICE, lwM2MDevice = new SimpleLwM2MDevice());
        initializer.setInstancesForObject(FIRMWARE, fwLwM2MDevice = new FwLwM2MDevice());
        initializer.setInstancesForObject(SOFTWARE_MANAGEMENT, swLwM2MDevice = new SwLwM2MDevice());
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(BINARY_APP_DATA_CONTAINER, lwM2MBinaryAppDataContainer = new LwM2mBinaryAppDataContainer(executor, OBJECT_INSTANCE_ID_0),
                new LwM2mBinaryAppDataContainer(executor, OBJECT_INSTANCE_ID_1));
        locationParams = new LwM2MLocationParams();
        locationParams.getPos();
        initializer.setInstancesForObject(LOCATION, new LwM2mLocation(locationParams.getLatitude(), locationParams.getLongitude(), locationParams.getScaleFactor(), executor, OBJECT_INSTANCE_ID_0));
        initializer.setInstancesForObject(TEMPERATURE_SENSOR, lwM2MTemperatureSensor = new LwM2mTemperatureSensor(executor, OBJECT_INSTANCE_ID_0), new LwM2mTemperatureSensor(executor, OBJECT_INSTANCE_ID_12));

        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(coapConfig);
        dtlsConfig.set(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, true);

        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setLocalAddress("0.0.0.0", port);
        builder.setObjects(initializer.createAll());
        builder.setCoapConfig(coapConfig);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setSharedExecutor(executor);
        builder.setDecoder(new DefaultLwM2mDecoder(false));

        builder.setEncoder(new DefaultLwM2mEncoder(new LwM2mValueConverterImpl(), false));
        client = builder.build();

        LwM2mClientObserver observer = new LwM2mClientObserver() {
            @Override
            public void onBootstrapStarted(ServerIdentity bsserver, BootstrapRequest request) {
                log.info("ClientObserver -> onBootstrapStarted...");
            }

            @Override
            public void onBootstrapSuccess(ServerIdentity bsserver, BootstrapRequest request) {
                log.info("ClientObserver -> onBootstrapSuccess...");
            }

            @Override
            public void onBootstrapFailure(ServerIdentity bsserver, BootstrapRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.info("ClientObserver -> onBootstrapFailure...");
            }

            @Override
            public void onBootstrapTimeout(ServerIdentity bsserver, BootstrapRequest request) {
                log.info("ClientObserver -> onBootstrapTimeout...");
            }

            @Override
            public void onRegistrationStarted(ServerIdentity server, RegisterRequest request) {
//                log.info("ClientObserver -> onRegistrationStarted...  EndpointName [{}]", request.getEndpointName());
            }

            @Override
            public void onRegistrationSuccess(ServerIdentity server, RegisterRequest request, String registrationID) {
                log.info("ClientObserver -> onRegistrationSuccess...  EndpointName [{}] [{}]", request.getEndpointName(), registrationID);
            }

            @Override
            public void onRegistrationFailure(ServerIdentity server, RegisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.info("ClientObserver -> onRegistrationFailure... ServerIdentity [{}]", server);
            }

            @Override
            public void onRegistrationTimeout(ServerIdentity server, RegisterRequest request) {
                log.info("ClientObserver -> onRegistrationTimeout... RegisterRequest [{}]", request);
            }

            @Override
            public void onUpdateStarted(ServerIdentity server, UpdateRequest request) {
//                log.info("ClientObserver -> onUpdateStarted...  UpdateRequest [{}]", request);
            }

            @Override
            public void onUpdateSuccess(ServerIdentity server, UpdateRequest request) {
//                log.info("ClientObserver -> onUpdateSuccess...  UpdateRequest [{}]", request);
            }

            @Override
            public void onUpdateFailure(ServerIdentity server, UpdateRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {

            }

            @Override
            public void onUpdateTimeout(ServerIdentity server, UpdateRequest request) {

            }

            @Override
            public void onDeregistrationStarted(ServerIdentity server, DeregisterRequest request) {
                log.info("ClientObserver ->onDeregistrationStarted...  DeregisterRequest [{}]", request.getRegistrationId());

            }

            @Override
            public void onDeregistrationSuccess(ServerIdentity server, DeregisterRequest request) {
                log.info("ClientObserver ->onDeregistrationSuccess...  DeregisterRequest [{}]", request.getRegistrationId());

            }

            @Override
            public void onDeregistrationFailure(ServerIdentity server, DeregisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.info("ClientObserver ->onDeregistrationFailure...  DeregisterRequest [{}] [{}]", request.getRegistrationId(), request.getRegistrationId());
            }

            @Override
            public void onDeregistrationTimeout(ServerIdentity server, DeregisterRequest request) {
                log.info("ClientObserver ->onDeregistrationTimeout...  DeregisterRequest [{}] [{}]", request.getRegistrationId(), request.getRegistrationId());
            }

            @Override
            public void onUnexpectedError(Throwable unexpectedError) {

            }
        };
        this.client.addObserver(observer);
        if (!isRpc) {
            client.start();
        }
    }

    public void destroy() {
        if (client != null) {
            client.destroy(true);
        }
        if (lwm2mServer != null) {
            lwm2mServer = null;
        }
        if (lwM2MDevice != null) {
            lwM2MDevice.destroy();
        }
        if (fwLwM2MDevice != null) {
            fwLwM2MDevice.destroy();
        }
        if (swLwM2MDevice != null) {
            swLwM2MDevice.destroy();
        }
        if (lwM2MBinaryAppDataContainer != null) {
            lwM2MBinaryAppDataContainer.destroy();
        }
        if (lwM2MTemperatureSensor != null) {
            lwM2MTemperatureSensor.destroy();
        }
    }

    public void start() {
        if (client != null) {
            client.start();
        }
    }
}
