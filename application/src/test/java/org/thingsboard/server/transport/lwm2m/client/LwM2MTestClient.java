/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.scandium.DTLSConnector;
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
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

@Slf4j
@Data
public class LwM2MTestClient {

    private final ScheduledExecutorService executor;
    private final String endpoint;
    private LeshanClient client;

    public void init(Security security, NetworkConfig coapConfig) throws InvalidDDFFileException, IOException {
        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml"};
        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(LwM2MTestClient.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName), resourceName));
        }
        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, new Server(123, 300));
        initializer.setInstancesForObject(DEVICE, new SimpleLwM2MDevice());
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);

        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(true);
        dtlsConfig.setClientOnly();

        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        EndpointFactory endpointFactory = new EndpointFactory() {

            @Override
            public CoapEndpoint createUnsecuredEndpoint(InetSocketAddress address, NetworkConfig coapConfig,
                                                        ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setInetSocketAddress(address);
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }

            @Override
            public CoapEndpoint createSecuredEndpoint(DtlsConnectorConfig dtlsConfig, NetworkConfig coapConfig,
                                                      ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                DtlsConnectorConfig.Builder dtlsConfigBuilder = new DtlsConnectorConfig.Builder(dtlsConfig);

                // tricks to be able to change psk information on the fly
//                AdvancedPskStore pskStore = dtlsConfig.getAdvancedPskStore();
//                if (pskStore != null) {
//                    PskPublicInformation identity = pskStore.getIdentity(null, null);
//                    SecretKey key = pskStore
//                            .requestPskSecretResult(ConnectionId.EMPTY, null, identity, null, null, null).getSecret();
//                    singlePSKStore = new SinglePSKStore(identity, key);
//                    dtlsConfigBuilder.setAdvancedPskStore(singlePSKStore);
//                }
                builder.setConnector(new DTLSConnector(dtlsConfigBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }
        };


        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setLocalAddress("0.0.0.0", 11000);
        builder.setObjects(initializer.createAll());
        builder.setCoapConfig(coapConfig);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setEndpointFactory(endpointFactory);
        builder.setSharedExecutor(executor);
        builder.setDecoder(new DefaultLwM2mNodeDecoder(true));
        builder.setEncoder(new DefaultLwM2mNodeEncoder(true));
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

        client.start();
    }

    public void destroy() {
        client.destroy(true);
    }

}
