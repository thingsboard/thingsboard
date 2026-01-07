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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.TokenGenerator;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
        this(config, ThingsBoardExecutors.newSingleThreadScheduledExecutor(String.format("TbInMemoryRegistrationStore Cleaner (%ds)", cleanPeriodInSec)), cleanPeriodInSec, modelProvider);
    }

    public TbInMemoryRegistrationStore(LwM2MTransportServerConfig config, ScheduledExecutorService schedExecutor, long cleanPeriodInSec, LwM2mVersionedModelProvider modelProvider) {
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.modelProvider = modelProvider;
        this.config = config;
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
        log.trace("updateRegistration [{}]", update);
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
            updateObservation(registrationId, observation, addIfAbsent, removed);
        } finally {
            lock.writeLock().unlock();
        }
        return removed;
    }

    private void updateObservation(String registrationId, Observation observation, boolean addIfAbsent, List<Observation> removed) {

        // Absorption by existing Observations
        Observation previousObservation = null;
        ObservationIdentifier id = observation.getId();
        if (addIfAbsent) {
            if (!obsByToken.containsKey(id)) {
                previousObservation = obsByToken.put(id, observation);
            } else {
                obsByToken.put(id, observation);
            }
        } else {
            previousObservation = obsByToken.put(id, observation);
        }

        if (!tokensByRegId.containsKey(registrationId)) {
            tokensByRegId.put(registrationId, new HashSet<ObservationIdentifier>());
        }
        tokensByRegId.get(registrationId).add(id);

        // log any collisions
        if (previousObservation != null) {
            removed.add(previousObservation);
            log.warn("Token collision ? observation [{}] will be replaced by observation [{}] ",
                    previousObservation, observation);
        }
        // cancel existing observations for the same path and registration id.
        for (Observation obs : unsafeGetObservations(registrationId)) {
            if (areTheSamePaths(observation, obs) && !observation.getId().equals(obs.getId())) {
                unsafeRemoveObservation(obs.getId());
                removed.add(obs);
            }
        }
    }

    private boolean areTheSamePaths(Observation observation, Observation obs) {
        if (observation instanceof SingleObservation && obs instanceof SingleObservation) {
            return ((SingleObservation) observation).getPath().equals(((SingleObservation) obs).getPath());
        }
        if (observation instanceof CompositeObservation && obs instanceof CompositeObservation) {
            return ((CompositeObservation) observation).getPaths().equals(((CompositeObservation) obs).getPaths());
        }
        return false;
    }

    @Override
    public Observation removeObservation(String registrationId, ObservationIdentifier observationId) {
        try {
            lock.writeLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation instanceof SingleObservation){
                log.trace("(SingleObservation) removeObservation: [{}]", ((SingleObservation)observation).getPath());
            } else {
                log.trace("(CompositeObservation) removeObservation: [{}]", ((CompositeObservation)observation).getPaths());
            }

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
