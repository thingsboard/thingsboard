/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.server;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.utils.MagicLwM2mValueConverter;
import org.thingsboard.server.transport.lwm2m.utils.Util;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;

@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportController {
    private LeshanServer lhServer;
    private transient LwM2MTransportService lwM2MTransportService;

    @Autowired
    LwM2MTransportCtx lwm2mTransportContext;

//    @Autowired
//    public LwM2MTransportController (LwM2MTransportService lwM2MTransportService) {
//        this.lwM2MTransportService = lwM2MTransportService;
//    }

    private List<String> hikes = new ArrayList<>(Arrays.asList(
            "Wonderland Trail", "South Maroon Peak", "Tour du Mont Blanc",
            "Teton Crest Trail", "Everest Base Camp via Cho La Pass", "Kesugi Ridge"
    ));


    //    private Registration registrationClient;
//    private String localAddress = "localhost";
//    private int localPort = 5685;
//    private String secureLocalAddress = "localhost";
//    private int secureLocalPort = 5686;
    // Parse arguments
//    private CommandLine cl;
    private String modelsFolderPath;
    private final static String[] modelPaths = Util.modelPaths;

    @PostConstruct
    public void init() {
        // Prepare LWM2M server
        log.info("Starting LwM2M transport... PostConstruct");
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(lwm2mTransportContext.getHost(), lwm2mTransportContext.getPort());
        builder.setLocalSecureAddress(lwm2mTransportContext.getSecureHost(), lwm2mTransportContext.getSecurePort());
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        builder.setCoapConfig(coapConfig);

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));
//        modelsFolderPath = cl.getOptionValue("m"); // Get models folder
        modelsFolderPath = null; // Get models folder
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new MagicLwM2mValueConverter()));

        // Create and start LWM2M server
        this.lhServer = builder.build();
        lwM2MTransportService = new LwM2MTransportService(lhServer, lwm2mTransportContext);
        /**
         * Registration Interface
         *
         */
        this.lhServer.getRegistrationService().addListener(new RegistrationListener() {

            /**
             * Register – запрос, представленный в виде POST /rd?…
             */
            public void registered(Registration registration, Registration previousReg,
                                   Collection<Observation> previousObsersations) {
                Registration registration1 = lwM2MTransportService.getClient("client1");
                Registration registration2 = lwM2MTransportService.getClient("client2");
                log.info("new device: {}", registration.getEndpoint());
//                getClientValue("/3/0/14", registration.getEndpoint());
//                getClientValue("/3/0", registration.getEndpoint());
                getClientValue("/3", registration.getEndpoint());
            }

            /**
             * Update – представляет из себя CoAP POST запрос на URL, полученный в ответ на Register.
             */
            public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
                log.info("device is still here: {}", updatedReg.getEndpoint());
            }

            /**
             * De-register (CoAP DELETE) – отправляется клиентом в случае инициирования процедуры выключения.
             */
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                     Registration newReg) {
                log.info("device left: {}", registration.getEndpoint());
            }
        });
        this.lhServer.start();
    }

//    @GetMapping("/")
//    @ResponseBody
//    public String indexGet(HttpServletRequest request, HttpServletResponse response) {
//        return String.join("\n", this.hikes);
//    }
//
//    @PostMapping("/")
//    @ResponseBody
//    public String postRegistration(HttpServletRequest request, HttpServletResponse response) {
//
//        this.hikes.add("Hello");
//        return String.join("\n", this.hikes);
//    }

    /**
     * let's try to send request on registration :
     */


    public void getClientValue(String path, String clientEndpoint) {
        Registration registration = this.lhServer.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration != null) {
            System.out.println("new device: " + registration.getEndpoint());
            try {
                String[] paths = path.substring(1).split("/");
                ReadRequest readRequest;
                switch (paths.length) {
                    case 1:
                        readRequest = new ReadRequest(Integer.valueOf(paths[0]));
                        break;
                    case 2:
                        readRequest = new ReadRequest(Integer.valueOf(paths[0]), Integer.valueOf(paths[1]));
                        break;
                    case 3:
                        readRequest = new ReadRequest(Integer.valueOf(paths[0]), Integer.valueOf(paths[1]), Integer.valueOf(paths[2]));
                        break;
                    default:
                        readRequest = null;
                }
//            ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0]),Integer.valueOf(paths[1]),Integer.valueOf(paths[2])));
//                ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0]), Integer.valueOf(paths[1])));
//                ReadResponse response = this.lhServer.send(registration, new ReadRequest(Integer.valueOf(paths[0])));
                if (readRequest != null) {
                    ReadResponse response = this.lhServer.send(registration, readRequest);
                    if (response.isSuccess()) {
//                    System.out.println("Device return: " + "\n" +
//                            "nanoTimestamp: " + ((Response) response.getCoapResponse()).getNanoTimestamp() +  "\n" +
//                            "code: " + ((Response) response.getCoapResponse()).getCode().text);
                        String typeValue = response.getContent().getClass().getName().substring(response.getContent().getClass().getName().lastIndexOf(".") + 1);
                        if (typeValue.equals("LwM2mSingleResource")) {
                            log.info("{}: id = {} \n value: {}", typeValue, response.getContent().getId(), ((LwM2mSingleResource) response.getContent()).getValue());
//                        log.info(typeValue + ": id = " + response.getContent().getId() + "\n" +
//                                "value: " + ((LwM2mSingleResource) response.getContent()).getValue());
                        } else if (typeValue.equals("LwM2mObject")) {
                            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) response.getContent()).getInstances().entrySet()) {
                                log.info("{}", entry.getKey());
                                for (Map.Entry<Integer, LwM2mResource> entryRes : entry.getValue().getResources().entrySet()) {
                                    log.info("{}", entryRes.getValue());
                                }

                            }
                        } else if (typeValue.equals("LwM2mObjectInstance")) {
                            for (Map.Entry<Integer, LwM2mResource> entry : ((LwM2mObjectInstance) response.getContent()).getResources().entrySet()) {
                                log.info("{}", entry.getValue());
                            }
                        }
//                ((LwM2mObject)response.getContent()).getInstances().forEach((key1, value1) -> System.out.println("LwM2mSingleResourceId: " + key1 +  " "  + value1.getResources().forEach((key, value) -> System.out.println(key + " " + value))));
//                        "lwM2mObjectInstanceId: " + ((LwM2mObject)response.getContent()).getInstance(0).getId() + "\n" +
//                        "lwM2mObjectInstanceId: " + ((LwM2mObject)response.getContent()).getInstance(0).getResources());
                    } else {
                        System.out.println("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
