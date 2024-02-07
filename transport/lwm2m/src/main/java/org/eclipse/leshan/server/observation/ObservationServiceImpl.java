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
 *
 * Copyright (c) 2016 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 */
package org.eclipse.leshan.server.observation;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of the {@link ObservationService} accessing the persisted observation via the provided
 * {@link RegistrationStore}.
 *
 * When a new observation is added or changed or canceled, the registered listeners are notified.
 */
@Slf4j
public class ObservationServiceImpl implements ObservationService, LwM2mNotificationReceiver {

    private final Logger LOG = LoggerFactory.getLogger(ObservationServiceImpl.class);

    private final RegistrationStore registrationStore;
    private final LwM2mServerEndpointsProvider endpointProvider;
    private final boolean updateRegistrationOnNotification;

    private final List<ObservationListener> listeners = new CopyOnWriteArrayList<>();;

    /**
     * Creates an instance of {@link ObservationServiceImpl}
     */
    public ObservationServiceImpl(RegistrationStore store, LwM2mServerEndpointsProvider endpointProvider) {
        this(store, endpointProvider, false);
    }

    /**
     * Creates an instance of {@link ObservationServiceImpl}
     *
     * @param updateRegistrationOnNotification will activate registration update on observe notification.
     *
     * @since 1.1
     */
    public ObservationServiceImpl(RegistrationStore store, LwM2mServerEndpointsProvider endpointProvider,
                                  boolean updateRegistrationOnNotification) {
        this.registrationStore = store;
        this.updateRegistrationOnNotification = updateRegistrationOnNotification;
        this.endpointProvider = endpointProvider;
    }

    @Override
    public int cancelObservations(Registration registration) {
        // check registration id
        String registrationId = registration.getId();
        if (registrationId == null)
            return 0;

        Collection<Observation> observations = registrationStore.removeObservations(registrationId);
        if (observations == null)
            return 0;

        for (Observation observation : observations) {
            cancel(observation);
        }

        return observations.size();
    }

    @Override
    public int cancelObservations(Registration registration, String nodePath) {
        if (registration == null || registration.getId() == null || nodePath == null || nodePath.isEmpty())
            return 0;

        Set<Observation> observations = getObservationsForCancel(registration.getId(), nodePath);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public int cancelCompositeObservations(Registration registration, String[] nodePaths) {
        if (registration == null || registration.getId() == null || nodePaths == null || nodePaths.length == 0)
            return 0;

        Set<Observation> observations = getCompositeObservationsForCancel(registration.getId(), nodePaths);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public void cancelObservation(Observation observation) {
        if (observation == null)
            return;

        registrationStore.removeObservation(observation.getRegistrationId(), observation.getId());
        cancel(observation);
    }

    private void cancel(Observation observation) {
        List<LwM2mServerEndpoint> endpoints = endpointProvider.getEndpoints();
        for (LwM2mServerEndpoint lwM2mEndpoint : endpoints) {
            lwM2mEndpoint.cancelObservation(observation);
        }

        for (ObservationListener listener : listeners) {
            listener.cancelled(observation);
        }
    }

    @Override
    public Set<Observation> getObservations(Registration registration) {
        return getObservations(registration.getId());
    }

    private Set<Observation> getObservations(String registrationId) {
        if (registrationId == null)
            return Collections.emptySet();

        return new HashSet<>(registrationStore.getObservations(registrationId));
    }

    private Set<Observation> getCompositeObservationsForCancel(String registrationId, String[] nodePaths) {
        if (registrationId == null || nodePaths == null)
            return Collections.emptySet();

        // array of String to array of LWM2M path
        List<LwM2mPath> lwPaths = new ArrayList<>(nodePaths.length);
        for (int i = 0; i < nodePaths.length; i++) {
            lwPaths.add(new LwM2mPath(nodePaths[i]));
        }

        // search composite-observation
        Set<Observation> result = new HashSet<>();
        for (Observation obs : getObservations(registrationId)) {
            if (obs instanceof CompositeObservation) {
                if (lwPaths.equals(((CompositeObservation) obs).getPaths())) {
                    result.add(obs);
                }
            }
        }
        return result;
    }

    private Set<Observation> getObservationsForCancel(String registrationId, String nodePath) {
        if (registrationId == null || nodePath == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<>();
        LwM2mPath lwPath = new LwM2mPath(nodePath);
        for (Observation obs : getObservations(registrationId)) {
            if (obs instanceof SingleObservation) {
                LwM2mPath lwPathObs = ((SingleObservation) obs).getPath();
                if (lwPath.equals(lwPathObs) || lwPathObs.startWith(lwPath)) {                // nodePath = "3",     lwPathObs = "3/0/9": cancel for tne all lwPathObs
                    result.add(obs);
                } else if (!lwPath.equals(lwPathObs) && lwPath.startWith(lwPathObs)) {        // nodePath = "3/0/9", lwPathObs = "3":     error...
                        String errorMsg = String.format(
                                "Unexpected error <cancelObservation>: There is registration with id [%s] existing observation [%s] includes input observation [%s]!",
                                registrationId, lwPathObs, lwPath);
                        throw new IllegalStateException(errorMsg);
                }
            }
        }

        return result;
    }

    @Override
    public void addListener(ObservationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ObservationListener listener) {
        listeners.remove(listener);
    }

    private Registration updateRegistrationOnRegistration(Observation observation, LwM2mPeer sender,
            ClientProfile profile) {
        if (updateRegistrationOnNotification) {
            RegistrationUpdate regUpdate = new RegistrationUpdate(observation.getRegistrationId(), sender, null, null,
                    null, null, null, null, null, null, null, null);
            UpdatedRegistration updatedRegistration = registrationStore.updateRegistration(regUpdate);
            if (updatedRegistration == null || updatedRegistration.getUpdatedRegistration() == null) {
                String errorMsg = String.format(
                        "Unexpected error: There is no registration with id %s for this observation %s",
                        observation.getRegistrationId(), observation);
                LOG.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            return updatedRegistration.getUpdatedRegistration();
        }
        return profile.getRegistration();
    }

    // ********** NotificationListener interface **********//
    @Override
    public void onNotification(SingleObservation observation, LwM2mPeer sender, ClientProfile profile,
            ObserveResponse response) {
        try {
            Registration updatedRegistration = updateRegistrationOnRegistration(observation, sender, profile);
            for (ObservationListener listener : listeners) {
                listener.onResponse(observation, updatedRegistration, response);
            }
        } catch (Exception e) {
            for (ObservationListener listener : listeners) {
                listener.onError(observation, profile.getRegistration(), e);
            }
        }
    }

    @Override
    public void onNotification(CompositeObservation observation, LwM2mPeer sender, ClientProfile profile,
            ObserveCompositeResponse response) {
        try {
            Registration updatedRegistration = updateRegistrationOnRegistration(observation, sender, profile);
            for (ObservationListener listener : listeners) {
                listener.onResponse(observation, updatedRegistration, response);
            }
        } catch (Exception e) {
            for (ObservationListener listener : listeners) {
                listener.onError(observation, profile.getRegistration(), e);
            }
        }
    }

    @Override
    public void onError(Observation observation, LwM2mPeer sender, ClientProfile profile, Exception error) {
        for (ObservationListener listener : listeners) {
            listener.onError(observation, profile.getRegistration(), error);
        }
    }

    @Override
    public void newObservation(Observation observation, Registration registration) {
        for (ObservationListener listener : listeners) {
            listener.newObservation(observation, registration);
        }
    }

    @Override
    public void cancelled(Observation observation) {
        for (ObservationListener listener : listeners) {
            listener.cancelled(observation);
        }

    }
}
