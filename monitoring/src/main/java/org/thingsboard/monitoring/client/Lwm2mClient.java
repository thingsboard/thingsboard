/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.monitoring.util.ResourceUtils;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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

    public void initClient() throws InvalidDDFFileException, IOException {
        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "test-model.xml"};
        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(ResourceUtils.getResourceAsStream("lwm2m/models/" + resourceName), resourceName));
        }
        URI uri = URI.create(serverUri);
        int port = uri.getPort();

        Configuration coapConfig = new Configuration();
        coapConfig.set(CoapConfig.COAP_PORT, port);
        coapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, true);
        coapConfig.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.CLIENT_ONLY);
        LeshanClient leshanClient;

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, noSec(serverUri, 123));
        initializer.setInstancesForObject(SERVER, new Server(123, TimeUnit.MINUTES.toSeconds(60)));
        initializer.setInstancesForObject(DEVICE, this);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setFactoryForObject(3, new BaseInstanceEnablerFactory() {
            @Override
            public LwM2mInstanceEnabler create() {
                return new DummyInstanceEnabler();
            }
        });

        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        List<ClientProtocolProvider> protocolProvider = new ArrayList<>();
        protocolProvider.add(new CoapOscoreProtocolProvider());
        CaliforniumClientEndpointsProvider.Builder endpointsBuilder = new CaliforniumClientEndpointsProvider.Builder(
                protocolProvider.toArray(new ClientProtocolProvider[protocolProvider.size()]));

        endpointsBuilder.setConfiguration(coapConfig);
        endpointsBuilder.setClientAddress(new InetSocketAddress(port).getAddress());

        List<LwM2mClientEndpointsProvider> endpointsProvider = new ArrayList<>();
        endpointsProvider.add(endpointsBuilder.build());

        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(initializer.createAll());
        builder.setEndpointsProviders(endpointsProvider);
        builder.setDataSenders(new ManualDataSender());
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setDecoder(new DefaultLwM2mDecoder(false));
        builder.setEncoder(new DefaultLwM2mEncoder(false));
        leshanClient = builder.build();

        setLeshanClient(leshanClient);

        leshanClient.start();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public ReadResponse read(LwM2mServer identity, int resourceId) {
        return switch (resourceId) {
            case 0 -> ReadResponse.success(resourceId, data);
            case 16 -> ReadResponse.success(resourceId, "U");
            default -> super.read(identity, resourceId);
        };
    }

    public void send(String data) {
        this.data = data;
        fireResourceChange(0);
    }

    @Override
    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
        }
    }
}
