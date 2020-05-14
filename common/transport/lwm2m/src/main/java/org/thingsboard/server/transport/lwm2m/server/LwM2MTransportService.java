/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.lwm2m.utils.MagicLwM2mValueConverter;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;

@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportService {

    @Autowired
    LwM2MTransportCtx lwm2mTransportContext;

    private List<String> hikes = new ArrayList<>(Arrays.asList(
            "Wonderland Trail", "South Maroon Peak", "Tour du Mont Blanc",
            "Teton Crest Trail", "Everest Base Camp via Cho La Pass", "Kesugi Ridge"
    ));

    private LeshanServer lwServer;
    private Registration registrationClient;
    private String localAddress = "localhost";
    private int localPort = 5685;
    private String secureLocalAddress = "localhost";
    private int secureLocalPort = 5686;
    // Parse arguments
    private CommandLine cl;
    private  String modelsFolderPath;
    private final static String[] modelPaths = new String[] { "10241.xml", "10242.xml", "10243.xml", "10244.xml",
            "10245.xml", "10246.xml", "10247.xml", "10248.xml", "10249.xml", "10250.xml", "10251.xml",
            "10252.xml", "10253.xml", "10254.xml", "10255.xml", "10256.xml", "10257.xml", "10258.xml",
            "10259.xml", "10260-2_0.xml", "10260.xml", "10262.xml", "10263.xml", "10264.xml",
            "10265.xml", "10266.xml", "10267.xml", "10268.xml", "10269.xml", "10270.xml", "10271.xml",
            "10272.xml", "10273.xml", "10274.xml", "10275.xml", "10276.xml", "10277.xml", "10278.xml",
            "10279.xml", "10280.xml", "10281.xml", "10282.xml", "10283.xml", "10284.xml", "10286.xml",
            "10290.xml", "10291.xml", "10292.xml", "10299.xml", "10300.xml", "10308-2_0.xml",
            "10308.xml", "10309.xml", "10311.xml", "10313.xml", "10314.xml", "10315.xml", "10316.xml",
            "10318.xml", "10319.xml", "10320.xml", "10322.xml", "10323.xml", "10324.xml", "10326.xml",
            "10327.xml", "10328.xml", "10329.xml", "10330.xml", "10331.xml", "10332.xml", "10333.xml",
            "10334.xml", "10335.xml", "10336.xml", "10337.xml", "10338.xml", "10339.xml", "10340.xml",
            "10341.xml", "10342.xml", "10343.xml", "10344.xml", "10345.xml", "10346.xml", "10347.xml",
            "10348.xml", "10349.xml", "10350.xml", "10351.xml", "10352.xml", "10353.xml", "10354.xml",
            "10355.xml", "10356.xml", "10357.xml", "10358.xml", "10359.xml", "10360.xml", "10361.xml",
            "10362.xml", "10363.xml", "10364.xml", "10365.xml", "10366.xml", "10368.xml", "10369.xml",

            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
            "2055.xml", "2056.xml", "2057.xml",

            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
            "3347.xml", "3348.xml", "3349.xml", "3350.xml", "3351.xml", "3352.xml", "3353.xml",
            "3354.xml", "3355.xml", "3356.xml", "3357.xml", "3358.xml", "3359.xml", "3360.xml",
            "3361.xml", "3362.xml", "3363.xml", "3364.xml", "3365.xml", "3366.xml", "3367.xml",
            "3368.xml", "3369.xml", "3370.xml", "3371.xml", "3372.xml", "3373.xml", "3374.xml",
            "3375.xml", "3376.xml", "3377.xml", "3378.xml", "3379.xml", "3380-2_0.xml", "3380.xml",
            "3381.xml", "3382.xml", "3383.xml", "3384.xml", "3385.xml", "3386.xml",

            "LWM2M_APN_Connection_Profile-v1_0_1.xml", "LWM2M_Bearer_Selection-v1_0_1.xml",
            "LWM2M_Cellular_Connectivity-v1_0_1.xml", "LWM2M_DevCapMgmt-v1_0.xml",
            "LWM2M_LOCKWIPE-v1_0_1.xml", "LWM2M_Portfolio-v1_0.xml",
            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",
            "LWM2M_WLAN_connectivity4-v1_0.xml", "LWM2M_BinaryAppDataContainer-v1_0_1.xml",
            "LWM2M_EventLog-V1_0.xml" };

    @PostConstruct
    public void init()  {
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
        this.lwServer = builder.build();
        /**
         * Registration Interface
         *
         */
        this.lwServer.getRegistrationService().addListener(new RegistrationListener() {

            /**
             * Register – запрос, представленный в виде POST /rd?…
             */
            public void registered(Registration registration, Registration previousReg,
                                   Collection<Observation> previousObsersations) {
                setRegistrationClient (registration);
                System.out.println("new device: " + registration.getEndpoint());
                getClientValue("/3/0/14");
            }

            /**
             * Update – представляет из себя CoAP POST запрос на URL, полученный в ответ на Register.
             */
            public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
                System.out.println("device is still here: " + updatedReg.getEndpoint());
            }

            /**
             * De-register (CoAP DELETE) – отправляется клиентом в случае инициирования процедуры выключения.
             */
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                     Registration newReg) {
                setRegistrationClient(null);
                System.out.println("device left: " + registration.getEndpoint());
            }
        });
        this.lwServer.start();
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


    public void getClientValue(String path) {
        Registration registration = getRegistrationClient();
        if (registration != null) {
            System.out.println("new device: " + registration.getEndpoint());
            try {
                String[] paths = path.substring(1).split("/");

//            ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0]),Integer.valueOf(paths[1]),Integer.valueOf(paths[2])));
//                ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0]), Integer.valueOf(paths[1])));
                ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0])));
                if (response.isSuccess()) {
//                    System.out.println("Device return: " + "\n" +
//                            "nanoTimestamp: " + ((Response) response.getCoapResponse()).getNanoTimestamp() +  "\n" +
//                            "code: " + ((Response) response.getCoapResponse()).getCode().text);
                    String typeValue = response.getContent().getClass().getName().substring(response.getContent().getClass().getName().lastIndexOf(".") + 1);
                    if (typeValue.equals("LwM2mSingleResource")) {
                        System.out.println(typeValue + ": id = " + response.getContent().getId() + "\n" +
                                "value: " + ((LwM2mSingleResource) response.getContent()).getValue());
                    } else if (typeValue.equals("LwM2mObject")) {
                        for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) response.getContent()).getInstances().entrySet()) {
                            System.out.println(entry.getKey());
                            for (Map.Entry<Integer, LwM2mResource> entryRes : entry.getValue().getResources().entrySet()) {
                                System.out.println(entryRes.getValue());
                            }

                        }
                    } else if (typeValue.equals("LwM2mObjectInstance")) {
                        for (Map.Entry<Integer, LwM2mResource> entry : ((LwM2mObjectInstance) response.getContent()).getResources().entrySet()) {
                            System.out.println(entry.getValue());
                        }
                    }
//                ((LwM2mObject)response.getContent()).getInstances().forEach((key1, value1) -> System.out.println("LwM2mSingleResourceId: " + key1 +  " "  + value1.getResources().forEach((key, value) -> System.out.println(key + " " + value))));
//                        "lwM2mObjectInstanceId: " + ((LwM2mObject)response.getContent()).getInstance(0).getId() + "\n" +
//                        "lwM2mObjectInstanceId: " + ((LwM2mObject)response.getContent()).getInstance(0).getResources());
                } else {
                    System.out.println("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setRegistrationClient (Registration registration) {
        this.registrationClient = registration;
    }

    public Registration getRegistrationClient () {
        return  this.registrationClient;
    }

}
