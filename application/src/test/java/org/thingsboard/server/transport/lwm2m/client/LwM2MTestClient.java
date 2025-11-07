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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientEndpointFactory;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.NodeDecoder;
import org.eclipse.leshan.core.node.codec.NodeEncoder;
import org.eclipse.leshan.core.node.codec.cbor.LwM2mNodeCborDecoder;
import org.eclipse.leshan.core.node.codec.cbor.LwM2mNodeCborEncoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLDecoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLEncoder;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.senml.cbor.upokecenter.SenMLCborUpokecenterEncoderDecoder;
import org.eclipse.leshan.senml.json.jackson.SenMLJsonJacksonEncoderDecoder;
import org.junit.Assert;
import org.mockito.Mockito;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.LOCATION;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder.getDefaultPathEncoder;
import static org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest.serverId;
import static org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest.serverIdBs;
import static org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest.shortServerId;
import static org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest.shortServerIdBs0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_FAILURE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_FAILURE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_EXPECTED_ERROR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_FAILURE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_FAILURE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.TEMPERATURE_SENSOR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.lwm2mClientResources;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.setDtlsConnectorConfigCidLength;


@Slf4j
@Data
public class LwM2MTestClient {

    private final ScheduledExecutorService executor;
    private final String endpoint;
    private final String[] modelResources;
    private LeshanClient leshanClient;
    private SimpleLwM2MDevice lwM2MDevice;
    private FwLwM2MDevice fwLwM2MDevice;
    private SwLwM2MDevice swLwM2MDevice;
    private LwM2mBinaryAppDataContainer lwM2MBinaryAppDataContainer;
    private LwM2MLocationParams locationParams;
    private LwM2mTemperatureSensor lwM2MTemperatureSensor;
    private Set<LwM2MClientState> clientStates;
    private Map<LwM2MClientState, Integer> clientDtlsCid;
    private LwM2mUplinkMsgHandler defaultLwM2mUplinkMsgHandlerTest;
    private LwM2mClientContext clientContext;
    private LwM2mTemperatureSensor lwM2mTemperatureSensor12;
    private String deviceIdStr;

    public void init(Security security, Security securityBs, int port, boolean isRpc,
                     LwM2mUplinkMsgHandler defaultLwM2mUplinkMsgHandler,
                     LwM2mClientContext clientContext, Integer cIdLength, boolean queueMode,
                     boolean supportFormatOnly_SenMLJSON_SenMLCBOR, Integer value3_0_9) throws InvalidDDFFileException, IOException {
        Assert.assertNull("client already initialized", leshanClient);
        this.defaultLwM2mUplinkMsgHandlerTest = defaultLwM2mUplinkMsgHandler;
        this.clientContext = clientContext;

        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        for (String resourceName : lwm2mClientResources) {
            models.addAll(ObjectLoader.loadDdfFile(LwM2MTestClient.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName), resourceName));
        }
        if (this.modelResources != null) {
            List<ObjectModel> modelsRes = new ArrayList<>();
            for (String resourceName : this.modelResources) {
                modelsRes.addAll(ObjectLoader.loadDdfFile(LwM2MTestClient.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName), resourceName));
            }
            Set<Integer> idsToRemove = new HashSet<>();
            for (ObjectModel model : modelsRes) {
                idsToRemove.add(model.id);
            }
            models.removeIf(model -> idsToRemove.contains(model.id));
            models.addAll(modelsRes);
        }

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        if (securityBs != null && security != null) {
            // SECURITY
            security.setId(serverId);
            securityBs.setId(serverIdBs);
            LwM2mInstanceEnabler[] instances = new LwM2mInstanceEnabler[]{securityBs, security};
            initializer.setClassForObject(SECURITY, Security.class);
            initializer.setInstancesForObject(SECURITY, instances);
            // SERVER
            Server lwm2mServer = new Server(shortServerId, TimeUnit.MINUTES.toSeconds(60));
            lwm2mServer.setId(serverId);
            Server serverBs = new Server(shortServerIdBs0, TimeUnit.MINUTES.toSeconds(60));
            serverBs.setId(serverIdBs);
            instances = new LwM2mInstanceEnabler[]{serverBs, lwm2mServer};
            initializer.setClassForObject(SERVER, Server.class);
            initializer.setInstancesForObject(SERVER, instances);
        } else if (securityBs != null) {
            // SECURITY
            initializer.setInstancesForObject(SECURITY, securityBs);
            // SERVER
            initializer.setClassForObject(SERVER, Server.class);
        } else {
            // SECURITY
            initializer.setInstancesForObject(SECURITY, security);
            // SERVER
            Server lwm2mServer = new Server(shortServerId, TimeUnit.MINUTES.toSeconds(60));
            lwm2mServer.setId(serverId);
            initializer.setInstancesForObject(SERVER, lwm2mServer);
        }

        initializer.setInstancesForObject(DEVICE, lwM2MDevice = new SimpleLwM2MDevice(executor, value3_0_9));
        initializer.setInstancesForObject(FIRMWARE, fwLwM2MDevice = new FwLwM2MDevice());
        initializer.setInstancesForObject(SOFTWARE_MANAGEMENT, swLwM2MDevice = new SwLwM2MDevice());
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(BINARY_APP_DATA_CONTAINER, lwM2MBinaryAppDataContainer = new LwM2mBinaryAppDataContainer(executor, OBJECT_INSTANCE_ID_0),
                new LwM2mBinaryAppDataContainer(executor, OBJECT_INSTANCE_ID_1));
        locationParams = new LwM2MLocationParams();
        locationParams.getPos();
        initializer.setInstancesForObject(LOCATION, new LwM2mLocation(locationParams.getLatitude(), locationParams.getLongitude(), locationParams.getScaleFactor(), executor, OBJECT_INSTANCE_ID_0));
        LwM2mTemperatureSensor lwM2mTemperatureSensor0 = new LwM2mTemperatureSensor(executor, OBJECT_INSTANCE_ID_0);
        lwM2mTemperatureSensor12 = new LwM2mTemperatureSensor(executor, OBJECT_INSTANCE_ID_12);
        initializer.setInstancesForObject(TEMPERATURE_SENSOR, lwM2mTemperatureSensor0, lwM2mTemperatureSensor12);

        List<LwM2mObjectEnabler> enablers = initializer.createAll();

        // Create Californium Endpoints Provider:
        // --------------------------------------
        // Define Custom CoAPS protocol provider
        CoapsClientProtocolProvider customCoapsProtocolProvider = new CoapsClientProtocolProvider() {
            @Override
            public CaliforniumClientEndpointFactory createDefaultEndpointFactory() {
                return new CoapsClientEndpointFactory() {

                    @Override
                    protected DtlsConnectorConfig.Builder createRootDtlsConnectorConfigBuilder(
                            Configuration configuration) {
                        DtlsConnectorConfig.Builder builder = super.createRootDtlsConnectorConfigBuilder(configuration);

                        // Add DTLS Session lifecycle logger
                        builder.setSessionListener(new DtlsSessionLogger(clientStates, clientDtlsCid));

                        return builder;
                    };
                };
            }
        };

        // Create client endpoints Provider
        List<ClientProtocolProvider> protocolProvider = new ArrayList<>();

        /**
         * "Use java-coap for CoAP protocol instead of Californium."
         */
        protocolProvider.add(new CoapOscoreProtocolProvider());
        protocolProvider.add(customCoapsProtocolProvider);
        CaliforniumClientEndpointsProvider.Builder endpointsBuilder = new CaliforniumClientEndpointsProvider.Builder(
                protocolProvider.toArray(new ClientProtocolProvider[protocolProvider.size()]));


        // Create Californium Configuration
        Configuration clientCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        // These configuration values are always overwritten by CLI therefore set them to transient.
        clientCoapConfig.setTransient(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        clientCoapConfig.setTransient(DTLS_CONNECTION_ID_LENGTH);
        boolean supportDeprecatedCiphers = false;
        clientCoapConfig.set(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !supportDeprecatedCiphers);

        if (cIdLength != null) {
            setDtlsConnectorConfigCidLength(clientCoapConfig, cIdLength);
        }

        if (cIdLength != null) {
            setDtlsConnectorConfigCidLength(clientCoapConfig, cIdLength);
        }

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(clientCoapConfig);
        endpointsBuilder.setClientAddress(new InetSocketAddress(port).getAddress());


        // creates EndpointsProvider
        List<LwM2mClientEndpointsProvider> endpointsProvider = new ArrayList<>();
        endpointsProvider.add(endpointsBuilder.build());
        /**
         * dependency -> org.eclipse.leshan.transport.javacoap.client.endpoint;
         */
//        if (useJavaCoap) {endpointsProvider.add(new JavaCoapClientEndpointsProvider());

        // Configure Registration Engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        // old
        /**
         * Force reconnection/rehandshake on registration update.
         */
        int comPeriodInSec = 5;
        if (comPeriodInSec > 0) engineFactory.setCommunicationPeriod(comPeriodInSec * 1000);
//        engineFactory.setCommunicationPeriod(5000); // old
        /**
         * By default client will try to resume DTLS session by using abbreviated Handshake. This option force to always do a full handshake."
         */
        boolean reconnectOnUpdate = false;
        engineFactory.setReconnectOnUpdate(reconnectOnUpdate);
        engineFactory.setResumeOnConnect(true);

        /**
         * Client use queue mode.
         */
        engineFactory.setQueueMode(queueMode);

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(enablers);
        builder.setEndpointsProviders(endpointsProvider.toArray(new LwM2mClientEndpointsProvider[endpointsProvider.size()]));
        builder.setDataSenders(new ManualDataSender());
        builder.setRegistrationEngineFactory(engineFactory);
        Map<ContentFormat, NodeDecoder> decoders = new HashMap<>();
        Map<ContentFormat, NodeEncoder> encoders = new HashMap<>();
        if (supportFormatOnly_SenMLJSON_SenMLCBOR) {
//                decoders.put(ContentFormat.OPAQUE, new LwM2mNodeOpaqueDecoder());
            decoders.put(ContentFormat.CBOR, new LwM2mNodeCborDecoder());
            decoders.put(ContentFormat.SENML_JSON, new LwM2mNodeSenMLDecoder(new SenMLJsonJacksonEncoderDecoder(), true));
            decoders.put(ContentFormat.SENML_CBOR, new LwM2mNodeSenMLDecoder(new SenMLCborUpokecenterEncoderDecoder(), false));
            builder.setDecoder(new DefaultLwM2mDecoder(decoders));

//            encoders.put(ContentFormat.OPAQUE, new LwM2mNodeOpaqueEncoder());
            encoders.put(ContentFormat.CBOR, new LwM2mNodeCborEncoder());
            encoders.put(ContentFormat.SENML_JSON, new LwM2mNodeSenMLEncoder(new SenMLJsonJacksonEncoderDecoder()));
            encoders.put(ContentFormat.SENML_CBOR, new LwM2mNodeSenMLEncoder(new SenMLCborUpokecenterEncoderDecoder()));
            builder.setEncoder(new DefaultLwM2mEncoder(new LwM2mValueConverterImpl(), false));
            builder.setEncoder(new DefaultLwM2mEncoder(encoders, getDefaultPathEncoder(), new LwM2mValueConverterImpl()));
        } else {
            boolean supportOldFormat = true;
            builder.setDecoder(new DefaultLwM2mDecoder(supportOldFormat));
            builder.setEncoder(new DefaultLwM2mEncoder(new LwM2mValueConverterImpl(), supportOldFormat));
        }


        builder.setRegistrationEngineFactory(engineFactory);
        builder.setSharedExecutor(executor);

        clientStates = new HashSet<>();
        clientDtlsCid = new HashMap<>();
        clientStates.add(ON_INIT);
        leshanClient = builder.build();

        LwM2mClientObserver observer = new LwM2mClientObserver() {
            @Override
            public void onBootstrapStarted(LwM2mServer bsserver, BootstrapRequest request) {
                clientStates.add(ON_BOOTSTRAP_STARTED);
            }

            @Override
            public void onBootstrapSuccess(LwM2mServer bsserver, BootstrapRequest request) {
                clientStates.add(ON_BOOTSTRAP_SUCCESS);
            }

            @Override
            public void onBootstrapFailure(LwM2mServer bsserver, BootstrapRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                clientStates.add(ON_BOOTSTRAP_FAILURE);
            }

            @Override
            public void onBootstrapTimeout(LwM2mServer bsserver, BootstrapRequest request) {
                clientStates.add(ON_BOOTSTRAP_TIMEOUT);
            }

            @Override
            public void onRegistrationStarted(LwM2mServer server, RegisterRequest request) {
                clientStates.add(ON_REGISTRATION_STARTED);
            }

            @Override
            public void onRegistrationSuccess(LwM2mServer server, RegisterRequest request, String registrationID) {
                clientStates.add(ON_REGISTRATION_SUCCESS);
            }

            @Override
            public void onRegistrationFailure(LwM2mServer server, RegisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                clientStates.add(ON_REGISTRATION_FAILURE);
            }

            @Override
            public void onRegistrationTimeout(LwM2mServer server, RegisterRequest request) {
                clientStates.add(ON_REGISTRATION_TIMEOUT);
            }

            @Override
            public void onUpdateStarted(LwM2mServer server, UpdateRequest request) {
                clientStates.add(ON_UPDATE_STARTED);
            }

            @Override
            public void onUpdateSuccess(LwM2mServer server, UpdateRequest request) {
                clientStates.add(ON_UPDATE_SUCCESS);
            }

            @Override
            public void onUpdateFailure(LwM2mServer server, UpdateRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                clientStates.add(ON_UPDATE_FAILURE);
            }

            @Override
            public void onUpdateTimeout(LwM2mServer server, UpdateRequest request) {
                clientStates.add(ON_UPDATE_TIMEOUT);
            }

            @Override
            public void onDeregistrationStarted(LwM2mServer server, DeregisterRequest request) {
                clientStates.add(ON_DEREGISTRATION_STARTED);
            }

            @Override
            public void onDeregistrationSuccess(LwM2mServer server, DeregisterRequest request) {
                clientStates.add(ON_DEREGISTRATION_SUCCESS);
            }

            @Override
            public void onDeregistrationFailure(LwM2mServer server, DeregisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                clientStates.add(ON_DEREGISTRATION_FAILURE);
            }

            @Override
            public void onDeregistrationTimeout(LwM2mServer server, DeregisterRequest request) {
                clientStates.add(ON_DEREGISTRATION_TIMEOUT);
            }

            @Override
            public void onUnexpectedError(Throwable unexpectedError) {
                clientStates.add(ON_EXPECTED_ERROR);
            }
        };
        this.leshanClient.addObserver(observer);

        // Add some log about object tree life cycle.
        this.leshanClient.getObjectTree().addListener(new ObjectsListenerAdapter() {

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                log.info("Object {} v{} disabled.", object.getId(), object.getObjectModel().version);
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                log.info("Object {} v{} enabled.", object.getId(), object.getObjectModel().version);
            }
        });


        if (!isRpc) {
            this.start(true);
        }
    }

    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
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

    public void start(boolean isStartLw) {
        if (leshanClient != null) {
            leshanClient.start();
            if (isStartLw) {
                this.awaitClientAfterStartConnectLw();
            }
            lwM2mTemperatureSensor12.setLeshanClient(leshanClient);
            fwLwM2MDevice.setLeshanClient(leshanClient);
        }
    }

    public void stop(boolean deregister) {
        if (leshanClient != null) {
            leshanClient.stop(deregister);
        }
    }

    private void awaitClientAfterStartConnectLw() {
        LwM2mClient lwM2MClient = this.clientContext.getClientByEndpoint(endpoint);
        Mockito.doAnswer(invocationOnMock -> null).when(defaultLwM2mUplinkMsgHandlerTest).initAttributes(lwM2MClient, true);
    }
}
