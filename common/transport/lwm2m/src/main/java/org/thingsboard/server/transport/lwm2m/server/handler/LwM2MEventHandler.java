package org.thingsboard.server.transport.lwm2m.server.handler;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.transport.lwm2m.LwM2MTransportContext;
import org.thingsboard.server.gen.transport.TransportProtos.LwM2MRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.LwM2MResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.*;

@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MEventHandler {

    private LeshanServer lhServer;

    @Autowired
    private LwM2MTransportContext context;

    public void onRegistered(Registration registration) {
        String endpointId = registration.getEndpoint();
        String smsNumber = registration.getSmsNumber() == null ? "" : registration.getSmsNumber();
        String lwm2mVersion = registration.getLwM2mVersion();

        log.trace("[{}][{}] Received endpoint registration event", lwm2mVersion, endpointId);
        LwM2MRegistrationRequestMsg registrationRequestMsg = LwM2MRegistrationRequestMsg.newBuilder()
                .setTenantId(context.getTenantId())
                .setEndpoint(endpointId)
                .setLwM2MVersion(lwm2mVersion)
                .setSmsNumber(smsNumber).build();
        LwM2MRequestMsg requestMsg = LwM2MRequestMsg.newBuilder().setRegistrationMsg(registrationRequestMsg).build();
        context.getTransportService().process(requestMsg, new TransportServiceCallback<LwM2MResponseMsg>() {
            @Override
            public void onSuccess(LwM2MResponseMsg msg) {
                log.trace("[{}][{}] Received endpoint registration response: {}", lwm2mVersion, endpointId, msg);
                //TODO: associate endpointId with device information.

                PostAttributeMsg
            }

            @Override
            public void onError(Throwable e) {
                log.warn("[{}][{}] Failed to process registration request", lwm2mVersion, endpointId, e);
            }
        });
    }

    public void setServer(LeshanServer lhServer) {
        this.lhServer = lhServer;
    }
}
