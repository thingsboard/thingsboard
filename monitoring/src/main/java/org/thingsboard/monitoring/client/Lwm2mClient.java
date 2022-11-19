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
package org.thingsboard.monitoring.client;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.response.ReadResponse;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

@Slf4j
public class Lwm2mClient extends BaseInstanceEnabler implements Destroyable {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private LeshanClient leshanClient;

    private List<ObjectModel> models;
    private Security security;
    private NetworkConfig coapConfig;

    private static final List<Integer> supportedResources = Collections.singletonList(2);

    private String data = UUID.randomUUID().toString();

    private String serverUri;
    private String endpoint;

    public Lwm2mClient(String serverUri, String endpoint) {
        this.serverUri = serverUri;
        this.endpoint = endpoint;
    }

    public Lwm2mClient() {
    }

    public void initClient() throws InvalidDDFFileException, IOException {
        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml"};
        models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(getClass().getClassLoader().getResourceAsStream("lwm2m/" + resourceName), resourceName));
        }

        security = noSec(serverUri, 123);
        coapConfig = new NetworkConfig().setString("COAP_PORT", StringUtils.substringAfterLast(serverUri, ":"));


        setName(endpoint);

        LeshanClient leshanClient;

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, new Server(123, 300));
        initializer.setInstancesForObject(DEVICE, this);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
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
                builder.setConnector(new DTLSConnector(dtlsConfigBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }
        };

        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(initializer.createAll());
        builder.setCoapConfig(coapConfig);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setEndpointFactory(endpointFactory);
        builder.setDecoder(new DefaultLwM2mDecoder(false));
        builder.setEncoder(new DefaultLwM2mEncoder(false));
        leshanClient = builder.build();

        setLeshanClient(leshanClient);

        leshanClient.start();
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        if (resourceId == 2) {
            return ReadResponse.success(resourceId, data);
        }
        return ReadResponse.notFound();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @SneakyThrows
    public void send(String data) {
        this.data = data;
        fireResourcesChange(2);
    }

    @Override
    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
        }
    }
}
