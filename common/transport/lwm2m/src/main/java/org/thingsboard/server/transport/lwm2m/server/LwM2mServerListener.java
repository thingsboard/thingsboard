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

import java.util.Collection;

@Slf4j
public class LwM2mServerListener {

    private final LeshanServer lhServer;
    private final LwM2MTransportServiceImpl service;

    public LwM2mServerListener(LeshanServer lhServer, LwM2MTransportServiceImpl service) {
        this.lhServer = lhServer;
        this.service = service;
    }

    public final RegistrationListener registrationListener = new RegistrationListener() {
        /**
         * Register – запрос, представленный в виде POST /rd?…
         */
        @Override
        public void registered(Registration registration, Registration previousReg,
                               Collection<Observation> previousObservations) {
            service.onRegistered(lhServer, registration, previousObservations);
        }

        /**
         * Update – представляет из себя CoAP POST запрос на URL, полученный в ответ на Register.
         */
        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                            Registration previousRegistration) {
            service.updatedReg(lhServer, updatedRegistration);
        }

        /**
         * De-register (CoAP DELETE) – отправляется клиентом в случае инициирования процедуры выключения.
         */
        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            service.unReg(lhServer, registration, observations);
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {
        @Override
        public void onSleeping(Registration registration) {
            log.info("onSleeping");
            service.onSleepingDev(registration);
        }

        @Override
        public void onAwake(Registration registration) {
            log.info("onAwake");
            service.onAwakeDev(registration);
        }
    };

    public final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
            log.info("Received notification cancelled from [{}] ", observation.getPath());
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            if (registration != null) {
                try {
                    service.onObservationResponse(registration, observation.getPath().toString(), response);
                } catch (Exception e) {
                    log.error("[{}] onResponse", e.toString());

                }
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            log.error(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(), observation.getPath()), error);
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
            log.info("Received newObservation from [{}] endpoint  [{}] ", observation.getPath(), registration.getEndpoint());
        }
    };

}
