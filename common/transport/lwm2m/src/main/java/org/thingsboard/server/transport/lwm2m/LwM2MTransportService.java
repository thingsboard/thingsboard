package org.thingsboard.server.transport.lwm2m;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos.LwM2MRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.LwM2MResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.*;
import org.thingsboard.server.transport.lwm2m.server.LwM2MService;
import org.thingsboard.server.transport.lwm2m.server.handler.LwM2MEventHandler;
import org.thingsboard.server.transport.lwm2m.utils.MagicLwM2mValueConverter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.Collection;

@Service("Lwm2mTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportService {

    @Autowired
    private LwM2MTransportContext context;

    @Autowired
    LwM2MEventHandler eventHandler;

    private LeshanServer lhServer;

    @PostConstruct
    public void init() {
        // Prepare LWM2M server
        log.info("Starting LwM2M transport... PostConstruct");
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(context.getHost(), context.getPort());
        builder.setLocalSecureAddress(context.getSecureHost(), context.getSecurePort());
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

//        // Define model provider
//        List<ObjectModel> models = ObjectLoader.loadDefault();
//        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));
////        modelsFolderPath = cl.getOptionValue("m"); // Get models folder
//        modelsFolderPath = null; // Get models folder
//        if (modelsFolderPath != null) {
//            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
//        }
//        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
//        builder.setObjectModelProvider(modelProvider);

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new MagicLwM2mValueConverter()));

        // Create and start LWM2M server
        this.lhServer = builder.build();
//        lwM2MService = new LwM2MService(lhServer, lwm2mTransportContext);
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
                eventHandler.onRegistered(registration);

//                Registration registration1 = lwM2MService.getClient("client1");
//                Registration registration2 = lwM2MService.getClient("client2");
//                log.info("new device: {}", registration.getEndpoint());
////                getClientValue("/3/0/14", registration.getEndpoint());
////                getClientValue("/3/0", registration.getEndpoint());
//                getClientValue("/3", registration.getEndpoint());
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

        eventHandler.setServer(lhServer);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping LwM2M transport!");
        try {
            lhServer.destroy();
        } finally {
        }
        log.info("LwM2M transport stopped!");
    }
}
