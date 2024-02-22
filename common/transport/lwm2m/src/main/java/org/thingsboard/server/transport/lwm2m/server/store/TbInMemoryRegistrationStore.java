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
package org.thingsboard.server.transport.lwm2m.server.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.RandomTokenGenerator;
import org.eclipse.californium.core.network.TokenGenerator;
import org.eclipse.californium.core.network.TokenGenerator.Scope;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.eclipse.leshan.core.californium.ObserveUtil.CTX_CF_OBERSATION;
import static org.eclipse.leshan.core.californium.ObserveUtil.extractSerializedObservation;

@Slf4j
public class TbInMemoryRegistrationStore implements RegistrationStore, Startable, Stoppable, Destroyable {

    // Data structure
    private final Map<String /* end-point */, Registration> regsByEp = new HashMap<>();
    private final Map<InetSocketAddress, Registration> regsByAddr = new HashMap<>();
    private final Map<String /* reg-id */, Registration> regsByRegId = new HashMap<>();
    private final Map<LwM2mIdentity, Registration> regsByIdentity = new HashMap<>();
    private final Map<ObservationIdentifier, Observation> obsByToken = new HashMap<>();
    private final Map<String, Set<ObservationIdentifier>> tokensByRegId = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private ScheduledFuture<?> cleanerTask;
    private boolean started = false;
    private final long cleanPeriod; // in seconds

    private final LwM2MTransportServerConfig config;

    private final LwM2mVersionedModelProvider modelProvider;

    private TokenGenerator tokenGenerator;

    public TbInMemoryRegistrationStore() {
        this(null, 2, null); // default clean period : 2s
    }

    public TbInMemoryRegistrationStore(LwM2MTransportServerConfig config, long cleanPeriodInSec, LwM2mVersionedModelProvider modelProvider) {
        this(config, Executors.newScheduledThreadPool(1,
                        new NamedThreadFactory(String.format("TbInMemoryRegistrationStore Cleaner (%ds)", cleanPeriodInSec))),
                cleanPeriodInSec, modelProvider);
    }

    public TbInMemoryRegistrationStore(LwM2MTransportServerConfig config, ScheduledExecutorService schedExecutor, long cleanPeriodInSec, LwM2mVersionedModelProvider modelProvider) {
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.modelProvider = modelProvider;
        this.config =  config;
    }

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) {
        try {
            lock.writeLock().lock();

            Registration registrationRemoved = regsByEp.put(registration.getEndpoint(), registration);
            regsByRegId.put(registration.getId(), registration);
            regsByIdentity.put(registration.getClientTransportData().getIdentity(), registration);
            // If a registration is already associated to this address we don't care as we only want to keep the most
            // recent binding.
            regsByAddr.put(registration.getSocketAddress(), registration);
            if (registrationRemoved != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registrationRemoved.getId());
                if (!registrationRemoved.getSocketAddress().equals(registration.getSocketAddress())) {
                    removeFromMap(regsByAddr, registrationRemoved.getSocketAddress(), registrationRemoved);
                }
                if (!registrationRemoved.getId().equals(registration.getId())) {
                    removeFromMap(regsByRegId, registrationRemoved.getId(), registrationRemoved);
                }
                if (!registrationRemoved.getClientTransportData().getIdentity()
                        .equals(registration.getClientTransportData().getIdentity())) {
                    removeFromMap(regsByIdentity, registrationRemoved.getClientTransportData().getIdentity(),
                            registrationRemoved);
                }
                return new Deregistration(registrationRemoved, observationsRemoved);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(update.getRegistrationId());
            if (registration == null) {
                return null;
            } else {
                Registration updatedRegistration = update.update(registration);
                regsByEp.put(updatedRegistration.getEndpoint(), updatedRegistration);
                // If registration is already associated to this address we don't care as we only want to keep the most
                // recent binding.
                regsByAddr.put(updatedRegistration.getSocketAddress(), updatedRegistration);
                if (!registration.getSocketAddress().equals(updatedRegistration.getSocketAddress())) {
                    removeFromMap(regsByAddr, registration.getSocketAddress(), registration);
                }
                regsByIdentity.put(updatedRegistration.getClientTransportData().getIdentity(), updatedRegistration);
                if (!registration.getClientTransportData().getIdentity()
                        .equals(updatedRegistration.getClientTransportData().getIdentity())) {
                    removeFromMap(regsByIdentity, registration.getClientTransportData().getIdentity(), registration);
                }

                regsByRegId.put(updatedRegistration.getId(), updatedRegistration);

                return new UpdatedRegistration(registration, updatedRegistration);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try {
            lock.readLock().lock();
            return regsByRegId.get(registrationId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        try {
            lock.readLock().lock();
            return regsByEp.get(endpoint);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        try {
            lock.readLock().lock();
            return regsByAddr.get(address);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByIdentity(LwM2mIdentity identity) {
        try {
            lock.readLock().lock();
            return regsByIdentity.get(identity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(regsByEp.values()).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(registrationId);
            if (registration != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registration.getId());
                regsByEp.remove(registration.getEndpoint());
                removeFromMap(regsByAddr, registration.getSocketAddress(), registration);
                removeFromMap(regsByRegId, registration.getId(), registration);
                removeFromMap(regsByIdentity, registration.getClientTransportData().getIdentity(), registration);
                return new Deregistration(registration, observationsRemoved);
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Leshan Observation API **************** */

    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation, boolean addIfAbsent) {
        List<Observation> removed = new ArrayList<>();
        try {
            lock.writeLock().lock();

            if (!regsByRegId.containsKey(registrationId)) {
                throw new IllegalStateException(String.format(
                        "can not add observation %s there is no registration with id %s", observation, registrationId));
            }

            if (observation instanceof SingleObservation) {
                if (validateObserveResource(((SingleObservation)observation).getPath(), registrationId)) {
                    updateSingleObservation(registrationId, (SingleObservation) observation, addIfAbsent, removed);
                        // cancel existing observations for the same path and registration id.
                    cancelObservation (observation, registrationId, removed);
                }
            } else {
                ContentFormat ct = ((CompositeObservation) observation).getResponseContentFormat();
                Map<String, String> ctx = observation.getContext();
                String serializedObservation = extractSerializedObservation(observation);
                JsonNode nodeSerObs = JacksonUtil.toJsonNode(serializedObservation);
                ((CompositeObservation)observation).getPaths().forEach(path -> {
                    if (validateObserveResource(path, registrationId)) {
                        String serializedObs = createSerializedSingleObservation(nodeSerObs, path.toString());
                        Observation singleObservation = createSingleObservation(registrationId, path, ct, ctx, serializedObs, getTokenGenerator());
                        updateSingleObservation(registrationId, (SingleObservation) singleObservation, addIfAbsent, removed);
                        // cancel existing observations for the same path and registration id.
                        cancelObservation (singleObservation, registrationId, removed);
                    }
                });
            }

        } finally {
            lock.writeLock().unlock();
        }

        return removed;
    }

    private boolean validateObserveResource(LwM2mPath path, String registrationId){
        // check if the resource is readable.
        if (path.isResource() || path.isResourceInstance()) {
            ObjectModel objectModel = modelProvider.getObjectModel(getRegistration(registrationId)).getObjectModel(path.getObjectId());
            ResourceModel resourceModel = objectModel == null ? null : objectModel.resources.get(path.getResourceId());
            if (resourceModel == null) {
                return false;
            } else if (!resourceModel.operations.isReadable()) {
                return false;
            } else if (path.isResourceInstance() && !resourceModel.multiple) {
                return false;
            }
        }
        return true;
    }

    private void updateSingleObservation (String registrationId, SingleObservation observation, boolean addIfAbsent, List<Observation> removed) {
            // Absorption by existing Observations


        Observation previousObservation = null;
        SingleObservation existingObservation = null;

        ObservationIdentifier id = observation.getId();
        if (addIfAbsent) {
            if (!obsByToken.containsKey(id)) {
                existingObservation = validateByAbsorptionExistingObservations(observation);
                if (existingObservation == null) {
                    obsByToken.put(id, observation);
                } else if (!existingObservation.getPath().equals(observation.getPath())){
                    obsByToken.put(id, observation);
                    previousObservation = obsByToken.get(existingObservation.getId());
                }
            }
        } else {
            previousObservation = obsByToken.put(id, observation);
        }
        if (!tokensByRegId.containsKey(registrationId)) {
            tokensByRegId.put(registrationId, new HashSet<ObservationIdentifier>());
        }

        if (existingObservation == null || !existingObservation.getPath().equals(observation.getPath())) {
            tokensByRegId.get(registrationId).add(id);
        }

        // log any collisions
        if (addIfAbsent && previousObservation != null) {
            if (!existingObservation.getPath().equals(observation.getPath())) {
                removed.add(previousObservation);
                log.warn("Token collision ? observation [{}] will be replaced by observation [{}], that this observation  includes input observation [{}]!",
                        previousObservation, observation, observation);
            } else {
                log.warn("Token collision ? existing observation [{}] includes input observation [{}]",
                        existingObservation, observation);
            }
        }
    }

    private SingleObservation validateByAbsorptionExistingObservations (SingleObservation observation) {
        LwM2mPath pathObservation = observation.getPath();
        AtomicReference<SingleObservation> result = new AtomicReference<>();
        obsByToken.values().stream().forEach(obs -> {
            LwM2mPath pathObs = ((SingleObservation)obs).getPath();
            if ((!pathObservation.equals(pathObs) && pathObs.startWith(pathObservation)) ||        // pathObs = "3/0/9"-> pathObservation = "3"
                    (pathObservation.equals(pathObs) && !observation.getId().equals(obs.getId()))) {
               result.set((SingleObservation)obs);
            } else if (!pathObservation.equals(pathObs) && pathObservation.startWith(pathObs)) {    // pathObs = "3" -> pathObservation = "3/0/9"
                result.set(observation);
            }
        });
        return result.get();
    }

    private TokenGenerator getTokenGenerator(){
        if (this.tokenGenerator == null) {
            this.tokenGenerator = new RandomTokenGenerator(config.getCoapConfig());
        }
        return this.tokenGenerator;
    }

    public static SingleObservation createSingleObservation(String registrationId, LwM2mPath target, ContentFormat ct,
                                                Map<String, String> ctx, String serializedObservation, TokenGenerator tokenGenerator) {
        Token token = tokenGenerator.createToken(Scope.SHORT_TERM);
        Map<String, String>  protocolData = Collections.emptyMap();
        if (serializedObservation != null) {
            protocolData = new HashMap<>();
            protocolData.put(CTX_CF_OBERSATION, serializedObservation);
        }
        return new SingleObservation(new ObservationIdentifier(token.getBytes()), registrationId, target, ct, ctx, protocolData);
    }

    public static String createSerializedSingleObservation(JsonNode nodeSerObs, String path){
        if (nodeSerObs.has("context")){
            ((ObjectNode) nodeSerObs.get("context")).put("leshan-path", path + "\n");
            return JacksonUtil.toString(nodeSerObs);
        }
        return null;
    }

    @Override
    public Observation removeObservation(String registrationId, ObservationIdentifier observationId) {
        try {
            lock.writeLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                unsafeRemoveObservation(observationId);
                return observation;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Observation getObservation(String registrationId, ObservationIdentifier observationId) {
        try {
            lock.readLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                return observation;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Observation getObservation(ObservationIdentifier observationId) {
        try {
            lock.readLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation != null) {
                return observation;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Prepare for Cancel one Observation
     * @param registrationId
     * @return
     */
    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try {
            lock.readLock().lock();
            return unsafeGetObservations(registrationId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * CancelAllObservation
     * @param registrationId
     * @return
     */

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try {
            lock.writeLock().lock();
            return unsafeRemoveAllObservations(registrationId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Observation utility functions **************** */

    private Observation unsafeGetObservation(ObservationIdentifier token) {
        Observation obs = obsByToken.get(token);
        return obs;
    }

    private void cancelObservation (Observation observation, String registrationId, List<Observation> removed) {
        for (Observation obs : unsafeGetObservations(registrationId)) {
            cancelExistingObservation(observation, obs, removed);
        }
    }

    private void cancelExistingObservation(Observation observation, Observation obs, List<Observation> removed) {
        LwM2mPath pathObservation = ((SingleObservation)observation).getPath();
        LwM2mPath pathObs = ((SingleObservation)obs).getPath();
        if ((!pathObservation.equals(pathObs) && pathObs.startWith(pathObservation)) ||        // pathObservation = "3", pathObs = "3/0/9"
             (pathObservation.equals(pathObs) && !observation.getId().equals(obs.getId()))) {
            unsafeRemoveObservation(obs.getId());
            removed.add(obs);
        } else if (!pathObservation.equals(pathObs) && pathObservation.startWith(pathObs)) {    // pathObservation = "3/0/9", pathObs = "3"
            unsafeRemoveObservation(observation.getId());
        }
    }

    private void unsafeRemoveObservation(ObservationIdentifier observationId) {
        Observation removed = obsByToken.remove(observationId);
        if (removed != null) {
            String registrationId = removed.getRegistrationId();
            Set<ObservationIdentifier> tokens = tokensByRegId.get(registrationId);
            tokens.remove(observationId);
            if (tokens.isEmpty()) {
                tokensByRegId.remove(registrationId);
            }
        }
    }


    /**
     * CancelAllObservation
     * @param registrationId
     * @return
     */
    private Collection<Observation> unsafeRemoveAllObservations(String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        Set<ObservationIdentifier> ids = tokensByRegId.get(registrationId);
        if (ids != null) {
            for (ObservationIdentifier id : ids) {
                Observation observationRemoved = obsByToken.remove(id);
                if (observationRemoved != null) {
                    removed.add(observationRemoved);
                }
            }
        }
        tokensByRegId.remove(registrationId);
        return removed;
    }

    private Collection<Observation> unsafeGetObservations(String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        Set<ObservationIdentifier> ids = tokensByRegId.get(registrationId);
        if (ids != null) {
            for (ObservationIdentifier id : ids) {
                Observation obs = unsafeGetObservation(id);
                if (obs != null) {
                    result.add(obs);
                }
            }
        }
        return result;
    }
    /* *************** Expiration handling **************** */

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        this.expirationListener = listener;
    }

    /**
     * start the registration store, will start regular cleanup of dead registrations.
     */
    @Override
    public synchronized void start() {
        if (!started) {
            started = true;
            cleanerTask = schedExecutor.scheduleAtFixedRate(new TbInMemoryRegistrationStore.Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public synchronized void stop() {
        if (started) {
            started = false;
            if (cleanerTask != null) {
                cleanerTask.cancel(false);
                cleanerTask = null;
            }
        }
    }

    /**
     * Destroy "cleanup" scheduler.
     */
    @Override
    public synchronized void destroy() {
        started = false;
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Destroying InMemoryRegistrationStore was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try {
                Collection<Registration> allRegs = new ArrayList<>();
                try {
                    lock.readLock().lock();
                    allRegs.addAll(regsByEp.values());
                } finally {
                    lock.readLock().unlock();
                }

                for (Registration reg : allRegs) {
                    if (!reg.isAlive()) {
                        // force de-registration
                        Deregistration removedRegistration = removeRegistration(reg.getId());
                        expirationListener.registrationExpired(removedRegistration.getRegistration(),
                                removedRegistration.getObservations());
                    }
                }
            } catch (Exception e) {
                log.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }

    // boolean remove(Object key, Object value) exist only since java8
    // So this method is here only while we want to support java 7
    protected <K, V> boolean removeFromMap(Map<K, V> map, K key, V value) {
        if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
            map.remove(key);
            return true;
        } else
            return false;
    }
}
