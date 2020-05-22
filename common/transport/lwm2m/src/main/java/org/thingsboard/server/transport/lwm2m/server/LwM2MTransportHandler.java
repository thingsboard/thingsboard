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
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;

@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportHandler {

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String EVENT_AWAKE = "AWAKE";

    private static final String EVENT_SLEEPING = "SLEEPING";

    private static final String EVENT_NOTIFICATION = "NOTIFICATION";

    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final long serialVersionUID = 1L;

    @Autowired
    private LeshanServer lhServer;

    @Autowired
    private LwM2MTransportService service;

    @PostConstruct
    public void init() {
        this.lhServer.getRegistrationService().addListener(this.registrationListener);
        this.lhServer.getPresenceService().addListener(this.presenceListener);
        this.lhServer.getObservationService().addListener(this.observationListener);
    }

    private final RegistrationListener registrationListener = new RegistrationListener() {

        /**
         * Register – запрос, представленный в виде POST /rd?…
         */
        @Override
        public void registered(Registration registration, Registration previousReg,
                               Collection<Observation> previousObsersations) {
           service.onRegistered(registration);
        }

        /**
         * Update – представляет из себя CoAP POST запрос на URL, полученный в ответ на Register.
         */
        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                            Registration previousRegistration) {
            service.updatedReg(updatedRegistration);
        }

        /**
         * De-register (CoAP DELETE) – отправляется клиентом в случае инициирования процедуры выключения.
         */
        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            service.unReg(registration);
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {

        @Override
        public void onSleeping(Registration registration) {
//            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();
            //            sendEvent(EVENT_SLEEPING, data, registration.getEndpoint());
            service.onSleepingDev (registration);
        }

        @Override
        public void onAwake(Registration registration) {
//            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();
            service.onAwakeDev (registration);
        }
    };

    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
            log.debug("Received notification cancelled from [{}] ", observation.getPath());
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            log.debug("Received notification onResponse from [{}] containing value [{}]", observation.getPath(), response.getContent().toString());
            if (registration != null) {
                service.observOnResponse(observation, registration,response);
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            log.info(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(), observation.getPath()), error);
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
            log.debug("Received notification cancelled from [{}] endpoint  [{}] ", observation.getPath(), registration.getEndpoint());
        }
    };
}
