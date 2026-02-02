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
package org.thingsboard.server.msa.connectivity.lwm2m.client;

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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.thingsboard.server.msa.PortFinder.isUDPPortAvailable;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_FAILURE;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_TIMEOUT;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_FAILURE;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_TIMEOUT;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_EXPECTED_ERROR;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_FAILURE;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_TIMEOUT;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_FAILURE;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_TIMEOUT;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.resources;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.serverId;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.shortServerId;

@Slf4j
@Data
public class LwM2MTestClient {

    private final ScheduledExecutorService executor;
    private final String endpoint;
    private LeshanClient leshanClient;
    private SimpleLwM2MDevice lwM2MDevice;

    private Set<LwM2MClientState> clientStates;

    private FwLwM2MDevice fwLwM2MDevice;

    private LwM2mBinaryAppDataContainer lwM2MBinaryAppDataContainer;
    private Map<LwM2MClientState, Integer> clientDtlsCid;

    private int countUpdateRegistrationSuccess;
    private int countReadObserveAfterUpdateRegistrationSuccess;

    private int clientPort;

    public void init(Security security, int clientPort) throws InvalidDDFFileException, IOException {
        assertThat(leshanClient).as("client already initialized").isNull();
        this.clientPort = clientPort;

        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(LwM2MTestClient.class.getClassLoader().getResourceAsStream("lwm2m-registry/" + resourceName), resourceName));
        }
        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);

        // SECURITY
        initializer.setInstancesForObject(SECURITY, security);
        // SERVER
        Server lwm2mServer = new Server(shortServerId, TimeUnit.MINUTES.toSeconds(60));
        lwm2mServer.setId(serverId);
        initializer.setInstancesForObject(SERVER, lwm2mServer);

        SimpleLwM2MDevice simpleLwM2MDevice = new SimpleLwM2MDevice(executor);
        initializer.setInstancesForObject(DEVICE, lwM2MDevice = simpleLwM2MDevice);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(FIRMWARE, fwLwM2MDevice = new FwLwM2MDevice());
        initializer.setInstancesForObject(BINARY_APP_DATA_CONTAINER, lwM2MBinaryAppDataContainer = new LwM2mBinaryAppDataContainer(executor, 0),
                new LwM2mBinaryAppDataContainer(executor, 1));

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

        // Set Californium Configuration

        endpointsBuilder.setConfiguration(clientCoapConfig);
        endpointsBuilder.setClientAddress(new InetSocketAddress(clientPort).getAddress());


        // creates EndpointsProvider
        List<LwM2mClientEndpointsProvider> endpointsProvider = new ArrayList<>();
        endpointsProvider.add(endpointsBuilder.build());

        // Configure Registration Engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        /**
         * Force reconnection/rehandshake on registration update.
         */
        int comPeriodInSec = 5;
        if (comPeriodInSec > 0)   engineFactory.setCommunicationPeriod(comPeriodInSec * 1000);

        /**
         * By default client will try to resume DTLS session by using abbreviated Handshake. This option force to always do a full handshake."
         */
        boolean reconnectOnUpdate = false;
        engineFactory.setReconnectOnUpdate(reconnectOnUpdate);
        engineFactory.setResumeOnConnect(true);

        /**
         * Client use queue mode.
         */
        engineFactory.setQueueMode(false);

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(enablers);
        builder.setEndpointsProviders(endpointsProvider.toArray(new LwM2mClientEndpointsProvider[endpointsProvider.size()]));
        builder.setDataSenders(new ManualDataSender());
        builder.setRegistrationEngineFactory(engineFactory);
        boolean supportOldFormat =  true;
        if (supportOldFormat) {
            builder.setDecoder(new DefaultLwM2mDecoder(supportOldFormat));
            builder.setEncoder(new DefaultLwM2mEncoder(new LwM2mValueConverterImpl(), supportOldFormat));
        }

        builder.setRegistrationEngineFactory(engineFactory);
//        builder.setSharedExecutor(executor);

        clientStates = new HashSet<>();
        clientDtlsCid = new HashMap<>();
        clientStates.add(ON_INIT);
        leshanClient = builder.build();
        simpleLwM2MDevice.setLwM2MTestClient(this);

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
                countUpdateRegistrationSuccess++;
                countReadObserveAfterUpdateRegistrationSuccess = 0;
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

            @Override
            public void resourceChanged(LwM2mPath... paths) {
                countReadObserveAfterUpdateRegistrationSuccess++;
                log.trace("resourceChanged paths {} cntReadObserve {}  cntUpdateSuccess {} .", paths, countReadObserveAfterUpdateRegistrationSuccess, countUpdateRegistrationSuccess);
            }
        });

    }

    public void start() {
        leshanClient.start();
        if (clientPort > 0) {
            log.error("Await UDP clientPort {} to be available before leshanClient.start()", clientPort);
            await("Await UDP clientPort " + clientPort + " to be available before leshanClient.start()")
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> isUDPPortAvailable(clientPort));
        }
    }

    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
            if (clientPort > 0) {
                log.error("Await UDP clientPort {} to disconnect after leshanClient.stop(deregister)", clientPort);
                await("Await client UDP port " + clientPort + " to disconnect after leshanClient.stop(deregister)")
                        .atMost(30, TimeUnit.SECONDS)
                        .until(() -> isUDPPortAvailable(clientPort));
            }
        }
        if (lwM2MDevice != null) {
            lwM2MDevice.destroy();
        }
        if (fwLwM2MDevice != null) {
            fwLwM2MDevice.destroy();
        }
        if (lwM2MBinaryAppDataContainer != null) {
            lwM2MBinaryAppDataContainer.destroy();
        }
    }
}