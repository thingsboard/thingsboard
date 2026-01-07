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
package org.thingsboard.monitoring.client;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.LwM2mServer;
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
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.monitoring.util.ResourceUtils;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

@Slf4j
public class Lwm2mClient extends BaseInstanceEnabler implements Destroyable {

    @Getter
    @Setter
    private LeshanClient leshanClient;

    private static final List<Integer> supportedResources = List.of(0, 16);

    private String data = "";

    private String serverUri;
    private String endpoint;

    public Lwm2mClient(String serverUri, String endpoint) {
        this.serverUri = serverUri;
        this.endpoint = endpoint;
    }

    public Lwm2mClient() {
    }

    public void initClient() throws InvalidDDFFileException, IOException {
        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "test-model.xml"};
        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(ResourceUtils.getResourceAsStream("lwm2m/models/" + resourceName), resourceName));
        }

        Security security = noSec(serverUri, 123);
        Configuration coapConfig = new Configuration();
        String portStr = StringUtils.substringAfterLast(serverUri, ":");
        if (StringUtils.isNotEmpty(portStr)) {
            coapConfig.set(CoapConfig.COAP_PORT, Integer.parseInt(portStr));
        }

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, new Server(123, TimeUnit.MINUTES.toSeconds(5)));
        initializer.setInstancesForObject(DEVICE, this);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);

        // Create client endpoints Provider
        List<ClientProtocolProvider> protocolProvider = new ArrayList<>();
        protocolProvider.add(new CoapOscoreProtocolProvider());
        protocolProvider.add(new CoapsClientProtocolProvider());
        CaliforniumClientEndpointsProvider.Builder endpointsBuilder = new CaliforniumClientEndpointsProvider.Builder(
                protocolProvider.toArray(new ClientProtocolProvider[protocolProvider.size()]));

        // Create Californium Configuration
        Configuration clientCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        clientCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        clientCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, true);

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(clientCoapConfig);

        // creates EndpointsProvider
        List<LwM2mClientEndpointsProvider> endpointsProvider = new ArrayList<>();
        endpointsProvider.add(endpointsBuilder.build());

        // Configure registration engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        // Build the client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(initializer.createAll());
        builder.setEndpointsProviders(endpointsProvider.toArray(new LwM2mClientEndpointsProvider[endpointsProvider.size()]));
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setDecoder(new DefaultLwM2mDecoder(false));
        builder.setEncoder(new DefaultLwM2mEncoder(false));
        leshanClient = builder.build();

        // Add observer
        LwM2mClientObserver observer = new LwM2mClientObserver() {
            @Override
            public void onBootstrapStarted(LwM2mServer bsserver, BootstrapRequest request) {
                // No implementation needed
            }

            @Override
            public void onBootstrapSuccess(LwM2mServer bsserver, BootstrapRequest request) {
                // No implementation needed
            }

            @Override
            public void onBootstrapFailure(LwM2mServer bsserver, BootstrapRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onBootstrapFailure [{}] [{}] [{}]", request.getEndpointName(), responseCode, errorMessage);
                // No implementation needed
            }

            @Override
            public void onBootstrapTimeout(LwM2mServer bsserver, BootstrapRequest request) {
                // No implementation needed
            }

            @Override
            public void onRegistrationStarted(LwM2mServer server, RegisterRequest request) {
                log.debug("onRegistrationStarted [{}]", request.getEndpointName());
            }

            @Override
            public void onRegistrationSuccess(LwM2mServer server, RegisterRequest request, String registrationID) {
                log.debug("onRegistrationSuccess [{}] [{}]", request.getEndpointName(), registrationID);
            }

            @Override
            public void onRegistrationFailure(LwM2mServer server, RegisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onRegistrationFailure [{}] [{}] [{}]", request.getEndpointName(), responseCode, errorMessage);
            }

            @Override
            public void onRegistrationTimeout(LwM2mServer server, RegisterRequest request) {
                log.debug("onRegistrationTimeout [{}]", request.getEndpointName());
            }

            @Override
            public void onUpdateStarted(LwM2mServer server, UpdateRequest request) {
                log.debug("onUpdateStarted [{}]", request.getRegistrationId());
            }

            @Override
            public void onUpdateSuccess(LwM2mServer server, UpdateRequest request) {
                log.debug("onUpdateSuccess [{}]", request.getRegistrationId());
            }

            @Override
            public void onUpdateFailure(LwM2mServer server, UpdateRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onUpdateFailure [{}]", request.getRegistrationId());
            }

            @Override
            public void onUpdateTimeout(LwM2mServer server, UpdateRequest request) {
                log.debug("onUpdateTimeout [{}]", request.getRegistrationId());
            }

            @Override
            public void onDeregistrationStarted(LwM2mServer server, DeregisterRequest request) {
                log.debug("onDeregistrationStarted [{}]", request.getRegistrationId());
            }

            @Override
            public void onDeregistrationSuccess(LwM2mServer server, DeregisterRequest request) {
                log.debug("onDeregistrationSuccess [{}]", request.getRegistrationId());
            }

            @Override
            public void onDeregistrationFailure(LwM2mServer server, DeregisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onDeregistrationFailure [{}] [{}] [{}]", request.getRegistrationId(), responseCode, errorMessage);
            }

            @Override
            public void onDeregistrationTimeout(LwM2mServer server, DeregisterRequest request) {
                log.debug("onDeregistrationTimeout [{}]", request.getRegistrationId());
            }

            @Override
            public void onUnexpectedError(Throwable unexpectedError) {
                log.debug("onUnexpectedError [{}]", unexpectedError.toString());
            }
        };
        leshanClient.addObserver(observer);

        setLeshanClient(leshanClient);

        leshanClient.start();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public ReadResponse read(LwM2mServer server, int resourceId) {
        return switch (resourceId) {
            case 0 -> ReadResponse.success(0, data);
            case 16 -> ReadResponse.success(16, "U");
            default -> super.read(server, resourceId);
        };
    }

    @SneakyThrows
    public void send(String data, int resource) {
        this.data = data;
        fireResourceChange(resource);
    }

    @Override
    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
        }
    }
}
