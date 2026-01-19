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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.send.SendListener;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.Collection;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

@Slf4j
public class LwM2mServerListener {

    private final LwM2mUplinkMsgHandler service;

    public LwM2mServerListener(LwM2mUplinkMsgHandler service) {
        this.service = service;
    }

    public final RegistrationListener registrationListener = new RegistrationListener() {
        /**
         * Register – query represented as POST /rd?…
         */
        @Override
        public void registered(Registration registration, Registration previousReg,
                               Collection<Observation> previousObservations) {
            log.debug("Client: registered: [{}]", registration.getEndpoint());
            service.onRegistered(registration, previousObservations);
        }

        /**
         * Update – query represented as CoAP POST request for the URL received in response to Register.
         */
        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                            Registration previousRegistration) {
            service.updatedReg(updatedRegistration);
        }

        /**
         * De-register (CoAP DELETE) – Sent by the client when a shutdown procedure is initiated.
         */
        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            service.unReg(registration, observations);
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {
        @Override
        public void onSleeping(Registration registration) {
            log.info("[{}] onSleeping", registration.getEndpoint());
            service.onSleepingDev(registration);
        }

        @Override
        public void onAwake(Registration registration) {
            log.info("[{}] onAwake", registration.getEndpoint());
            service.onAwakeDev(registration);
        }
    };

    public final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
            log.trace("Canceled Observation [RegistrationId:{}: {}].", observation.getRegistrationId(), observation instanceof SingleObservation ?
                    "SingleObservation: " + ((SingleObservation) observation).getPath() :
                    "CompositeObservation: " + ((CompositeObservation) observation).getPaths());
       }

        @Override
        public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
            if (registration != null) {
                LwM2mClient lwM2MClient = service.getClientContext().getClientByEndpoint(registration.getEndpoint());
                if (lwM2MClient != null) {
                    service.onUpdateValueAfterReadResponse(registration, convertObjectIdToVersionedId(observation.getPath().toString(), lwM2MClient), response);
                }
            }
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration, ObserveCompositeResponse response) {
            log.trace("Update Composite Observation [{}: {}].", observation.getRegistrationId(), observation.getPaths());
            service.onUpdateValueAfterReadCompositeResponse(registration, response);
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            if (error != null) {
                var path = observation instanceof SingleObservation ? "Single Observation Cancel: " + ((SingleObservation) observation).getPath() : "Composite Observation Cancel: " + ((CompositeObservation) observation).getPaths();
                var msgError = path + ": " + error.getMessage();
                log.trace("Unable to handle notification [RegistrationId:{}]: [{}].", observation.getRegistrationId(), msgError);
                service.onErrorObservation(registration, msgError);
            }
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
            log.trace("Successful start newObservation  [RegistrationId:{}: {}].", observation.getRegistrationId(), observation instanceof SingleObservation ?
                    "Single: " + ((SingleObservation) observation).getPath() :
                    "Composite: " + ((CompositeObservation) observation).getPaths());
        }
    };

    public final SendListener sendListener = new SendListener() {

        @Override
        public void dataReceived(Registration registration, TimestampedLwM2mNodes data, SendRequest request) {
            log.trace("Received Send request from [{}] containing value: [{}], coapRequest: [{}]", registration.getEndpoint(), data.toString(), request.getCoapRequest().toString());
            if (registration != null) {
                service.onUpdateValueWithSendRequest(registration, data);
            }
        }

        @Override
        public void onError(Registration registration, String errorMessage, Exception error) {

        }
    };
}
