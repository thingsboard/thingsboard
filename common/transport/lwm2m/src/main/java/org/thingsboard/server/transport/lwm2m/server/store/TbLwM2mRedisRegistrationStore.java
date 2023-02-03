/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.ObservationStoreException;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.serialization.IdentitySerDes;
import org.eclipse.leshan.server.redis.serialization.ObservationSerDes;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TbLwM2mRedisRegistrationStore implements CaliforniumRegistrationStore, Startable, Stoppable, Destroyable {
    /** Default time in seconds between 2 cleaning tasks (used to remove expired registration). */
    public static final long DEFAULT_CLEAN_PERIOD = 60;
    public static final int DEFAULT_CLEAN_LIMIT = 500;
    /** Defaut Extra time for registration lifetime in seconds */
    public static final long DEFAULT_GRACE_PERIOD = 0;

    private static final Logger LOG = LoggerFactory.getLogger(RedisRegistrationStore.class);

    // Redis key prefixes
    private static final String REG_EP = "REG:EP:"; // (Endpoint => Registration)
    private static final String REG_EP_REGID_IDX = "EP:REGID:"; // secondary index key (Registration ID => Endpoint)
    private static final String REG_EP_ADDR_IDX = "EP:ADDR:"; // secondary index key (Socket Address => Endpoint)
    private static final String REG_EP_IDENTITY = "EP:IDENTITY:"; // secondary index key (Identity => Endpoint)
    private static final String LOCK_EP = "LOCK:EP:";
    private static final byte[] OBS_TKN = "OBS:TKN:".getBytes(UTF_8);
    private static final String OBS_TKNS_REGID_IDX = "TKNS:REGID:"; // secondary index (token list by registration)
    private static final byte[] EXP_EP = "EXP:EP".getBytes(UTF_8); // a sorted set used for registration expiration
    // (expiration date, Endpoint)

    private final RedisConnectionFactory connectionFactory;

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private ScheduledFuture<?> cleanerTask;
    private boolean started = false;

    private final long cleanPeriod; // in seconds
    private final int cleanLimit; // maximum number to clean in a clean period
    private final long gracePeriod; // in seconds

    private final RedisLockRegistry redisLock;

    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, DEFAULT_CLEAN_PERIOD, DEFAULT_GRACE_PERIOD, DEFAULT_CLEAN_LIMIT); // default clean period 60s
    }

    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory, long cleanPeriodInSec, long lifetimeGracePeriodInSec, int cleanLimit) {
        this(connectionFactory, Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("RedisRegistrationStore Cleaner (%ds)", cleanPeriodInSec))),
                cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit);
    }

    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
                                         long lifetimeGracePeriodInSec, int cleanLimit) {
        this.connectionFactory = connectionFactory;
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.cleanLimit = cleanLimit;
        this.gracePeriod = lifetimeGracePeriodInSec;
        this.redisLock = new RedisLockRegistry(connectionFactory, "Registration");
    }

    /* *************** Redis Key utility function **************** */

    private byte[] toKey(byte[] prefix, byte[] key) {
        byte[] result = new byte[prefix.length + key.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(key, 0, result, prefix.length, key.length);
        return result;
    }

    private byte[] toKey(String prefix, String registrationID) {
        return (prefix + registrationID).getBytes();
    }

    private String toLockKey(String endpoint) {
        return new String(toKey(LOCK_EP, endpoint));
    }

    private String toLockKey(byte[] endpoint) {
        return new String(toKey(LOCK_EP.getBytes(UTF_8), endpoint));
    }

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            String lockKey = toLockKey(registration.getEndpoint());

            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();
                // add registration
                byte[] k = toEndpointKey(registration.getEndpoint());
                byte[] old = connection.getSet(k, serializeReg(registration));

                // add registration: secondary indexes
                byte[] regid_idx = toRegIdKey(registration.getId());
                connection.set(regid_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] addr_idx = toRegAddrKey(registration.getSocketAddress());
                connection.set(addr_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] identity_idx = toRegIdentityKey(registration.getIdentity());
                connection.set(identity_idx, registration.getEndpoint().getBytes(UTF_8));

                // Add or update expiration
                addOrUpdateExpiration(connection, registration);

                if (old != null) {
                    Registration oldRegistration = deserializeReg(old);
                    // remove old secondary index
                    if (!registration.getId().equals(oldRegistration.getId()))
                        connection.del(toRegIdKey(oldRegistration.getId()));
                    if (!oldRegistration.getSocketAddress().equals(registration.getSocketAddress())) {
                        removeAddrIndex(connection, oldRegistration);
                    }
                    if (!oldRegistration.getIdentity().equals(registration.getIdentity())) {
                        removeIdentityIndex(connection, oldRegistration);
                    }
                    // remove old observation
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(connection, oldRegistration.getId());

                    return new Deregistration(oldRegistration, obsRemoved);
                }

                return null;
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {

            // Fetch the registration ep by registration ID index
            byte[] ep = connection.get(toRegIdKey(update.getRegistrationId()));
            if (ep == null) {
                return null;
            }

            String lockKey = toLockKey(ep);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                // Fetch the registration
                byte[] data = connection.get(toEndpointKey(ep));
                if (data == null) {
                    return null;
                }

                Registration r = deserializeReg(data);

                Registration updatedRegistration = update.update(r);

                // Store the new registration
                connection.set(toEndpointKey(updatedRegistration.getEndpoint()), serializeReg(updatedRegistration));

                // Add or update expiration
                addOrUpdateExpiration(connection, updatedRegistration);

                /** Update secondary index :
                 * If registration is already associated to this address we don't care as we only want to keep the most
                 * recent binding. */
                byte[] addr_idx = toRegAddrKey(updatedRegistration.getSocketAddress());
                connection.set(addr_idx, updatedRegistration.getEndpoint().getBytes(UTF_8));
                if (!r.getSocketAddress().equals(updatedRegistration.getSocketAddress())) {
                    removeAddrIndex(connection, r);
                }
                if (!r.getIdentity().equals(updatedRegistration.getIdentity())) {
                    removeIdentityIndex(connection, r);
                }

                return new UpdatedRegistration(r, updatedRegistration);

            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return getRegistration(connection, registrationId);
        }
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        Validate.notNull(endpoint);
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        Validate.notNull(address);
        try (var connection = connectionFactory.getConnection()) {
            byte[] ep = connection.get(toRegAddrKey(address));
            if (ep == null) {
                return null;
            }
            byte[] data = connection.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Registration getRegistrationByIdentity(Identity identity) {
        Validate.notNull(identity);
        try (var connection = connectionFactory.getConnection()) {
            byte[] ep = connection.get(toRegIdentityKey(identity));
            if (ep == null) {
                return null;
            }
            byte[] data = connection.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        try (var connection = connectionFactory.getConnection()) {
            Collection<Registration> list = new LinkedList<>();
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(REG_EP + "*").build();
            List<Cursor<byte[]>> scans = new ArrayList<>();
            if (connection instanceof RedisClusterConnection) {
                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
                });
            } else {
                scans.add(connection.scan(scanOptions));
            }

            scans.forEach(scan -> {
                scan.forEachRemaining(key -> {
                    byte[] element = connection.get(key);
                    list.add(deserializeReg(element));
                });
            });
            return list.iterator();
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return removeRegistration(connection, registrationId, false);
        }
    }

    private Deregistration removeRegistration(RedisConnection connection, String registrationId, boolean removeOnlyIfNotAlive) {
        // fetch the client ep by registration ID index
        byte[] ep = connection.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }

        Lock lock = null;
        String lockKey = toLockKey(ep);
        try {
            lock = redisLock.obtain(lockKey);
            lock.lock();

            // fetch the client
            byte[] data = connection.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            Registration r = deserializeReg(data);

            if (!removeOnlyIfNotAlive || !r.isAlive(gracePeriod)) {
                long nbRemoved = connection.del(toRegIdKey(r.getId()));
                if (nbRemoved > 0) {
                    connection.del(toEndpointKey(r.getEndpoint()));
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(connection, r.getId());
                    removeAddrIndex(connection, r);
                    removeIdentityIndex(connection, r);
                    removeExpiration(connection, r);
                    return new Deregistration(r, obsRemoved);
                }
            }
            return null;
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void removeAddrIndex(RedisConnection connection, Registration r) {
        removeSecondaryIndex(connection, toRegAddrKey(r.getSocketAddress()), r.getEndpoint());
    }

    private void removeIdentityIndex(RedisConnection connection, Registration r) {
        removeSecondaryIndex(connection, toRegIdentityKey(r.getIdentity()), r.getEndpoint());
    }

    //TODO: JedisCluster didn't implement Transaction, maybe should use some advanced key creation strategies
    private void removeSecondaryIndex(RedisConnection connection, byte[] indexKey, String endpointName) {
        // Watch the key to remove.
//        connection.watch(indexKey);

        byte[] epFromAddr = connection.get(indexKey);
        // Delete the key if needed.
        if (Arrays.equals(epFromAddr, endpointName.getBytes(UTF_8))) {
            // Try to delete the key
//            connection.multi();
            connection.del(indexKey);
//            connection.exec();
            // if transaction failed this is not an issue as the index is probably reused and we don't need to
            // delete it anymore.
        } else {
            // the key must not be deleted.
//            connection.unwatch();
        }
    }

    private void addOrUpdateExpiration(RedisConnection connection, Registration registration) {
        connection.zAdd(EXP_EP, registration.getExpirationTimeStamp(gracePeriod), registration.getEndpoint().getBytes(UTF_8));
    }

    private void removeExpiration(RedisConnection connection, Registration registration) {
        connection.zRem(EXP_EP, registration.getEndpoint().getBytes(UTF_8));
    }

    private byte[] toRegIdKey(String registrationId) {
        return toKey(REG_EP_REGID_IDX, registrationId);
    }

    private byte[] toRegAddrKey(InetSocketAddress addr) {
        return toKey(REG_EP_ADDR_IDX, addr.getAddress().toString() + ":" + addr.getPort());
    }

    private byte[] toRegIdentityKey(Identity identity) {
        return toKey(REG_EP_IDENTITY, IdentitySerDes.serialize(identity).toString());
    }

    private byte[] toEndpointKey(String endpoint) {
        return toKey(REG_EP, endpoint);
    }

    private byte[] toEndpointKey(byte[] endpoint) {
        return toKey(REG_EP.getBytes(UTF_8), endpoint);
    }

    private byte[] serializeReg(Registration registration) {
        return RegistrationSerDes.bSerialize(registration);
    }

    private Registration deserializeReg(byte[] data) {
        return RegistrationSerDes.deserialize(data);
    }

    /* *************** Leshan Observation API **************** */

    /*
     * The observation is not persisted here, it is done by the Californium layer (in the implementation of the
     * org.eclipse.californium.core.observe.ObservationStore#add method)
     */
    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation) {
        List<Observation> removed = new ArrayList<>();
        try (var connection = connectionFactory.getConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = connection.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            Lock lock = null;
            String lockKey = toLockKey(ep);

            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                // cancel existing observations for the same path and registration id.
                for (Observation obs : getObservations(connection, registrationId)) {
                    //TODO: should be able to use CompositeObservation
                    if (((SingleObservation)observation).getPath().equals(((SingleObservation)obs).getPath())
                            && !Arrays.equals(observation.getId(), obs.getId())) {
                        removed.add(obs);
                        unsafeRemoveObservation(connection, registrationId, obs.getId());
                    }
                }

            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
        return removed;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try (var connection = connectionFactory.getConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = connection.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            // remove observation
            Lock lock = null;
            String lockKey = toLockKey(ep);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                Observation observation = build(get(new Token(observationId)));
                if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                    unsafeRemoveObservation(connection, registrationId, observationId);
                    return observation;
                }
                return null;

            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    public Observation getObservation(String registrationId, byte[] observationId) {
        return build(get(new Token(observationId)));
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return getObservations(connection, registrationId);
        }
    }

    private Collection<Observation> getObservations(RedisConnection connection, String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        for (byte[] token : connection.lRange(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, -1)) {
            byte[] obs = connection.get(toKey(OBS_TKN, token));
            if (obs != null) {
                result.add(build(deserializeObs(obs)));
            }
        }
        return result;
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            // check registration exists
            Registration registration = getRegistration(connection, registrationId);
            if (registration == null)
                return Collections.emptyList();

            // get endpoint and create lock
            String endpoint = registration.getEndpoint();
            Lock lock = null;
            String lockKey = toLockKey(endpoint);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();
                return unsafeRemoveAllObservations(connection, registrationId);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    /* *************** Californium ObservationStore API **************** */

    @Override
    public org.eclipse.californium.core.observe.Observation putIfAbsent(Token token,
                                                                        org.eclipse.californium.core.observe.Observation obs) throws ObservationStoreException {
        return add(obs, true);
    }

    @Override
    public org.eclipse.californium.core.observe.Observation put(Token token,
                                                                org.eclipse.californium.core.observe.Observation obs) throws ObservationStoreException {
        return add(obs, false);
    }

    private org.eclipse.californium.core.observe.Observation add(org.eclipse.californium.core.observe.Observation obs, boolean ifAbsent) throws ObservationStoreException {
        String endpoint = ObserveUtil.validateCoapObservation(obs);
        org.eclipse.californium.core.observe.Observation previousObservation = null;

        try (var connection = connectionFactory.getConnection()) {
            Lock lock = null;
            String lockKey = toLockKey(endpoint);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                String registrationId = ObserveUtil.extractRegistrationId(obs);
                if (!connection.exists(toRegIdKey(registrationId)))
                    throw new ObservationStoreException("no registration for this Id");
                byte[] key = toKey(OBS_TKN, obs.getRequest().getToken().getBytes());
                byte[] serializeObs = serializeObs(obs);
                byte[] previousValue;
                if (ifAbsent) {
                    previousValue = connection.get(key);
                    if (previousValue == null || previousValue.length == 0) {
                        connection.set(key, serializeObs);
                    } else {
                        return deserializeObs(previousValue);
                    }
                } else {
                    previousValue = connection.getSet(key, serializeObs);
                }

                // secondary index to get the list by registrationId
                connection.lPush(toKey(OBS_TKNS_REGID_IDX, registrationId), obs.getRequest().getToken().getBytes());

                // log any collisions
                if (previousValue != null && previousValue.length != 0) {
                    previousObservation = deserializeObs(previousValue);
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
        return previousObservation;
    }

    @Override
    public void remove(Token token) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] tokenKey = toKey(OBS_TKN, token.getBytes());

            // fetch the observation by token
            byte[] serializedObs = connection.get(tokenKey);
            if (serializedObs == null)
                return;

            org.eclipse.californium.core.observe.Observation obs = deserializeObs(serializedObs);
            String registrationId = ObserveUtil.extractRegistrationId(obs);
            Registration registration = getRegistration(connection, registrationId);
            if (registration == null) {
                LOG.warn("Unable to remove observation {}, registration {} does not exist anymore", obs.getRequest(),
                        registrationId);
                return;
            }

            String endpoint = registration.getEndpoint();
            Lock lock = null;
            String lockKey = toLockKey(endpoint);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                unsafeRemoveObservation(connection, registrationId, token.getBytes());
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

    }

    @Override
    public org.eclipse.californium.core.observe.Observation get(Token token) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] obs = connection.get(toKey(OBS_TKN, token.getBytes()));
            if (obs == null) {
                return null;
            } else {
                return deserializeObs(obs);
            }
        }
    }

    /* *************** Observation utility functions **************** */

    private Registration getRegistration(RedisConnection connection, String registrationId) {
        byte[] ep = connection.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }
        byte[] data = connection.get(toEndpointKey(ep));
        if (data == null) {
            return null;
        }

        return deserializeReg(data);
    }

    private void unsafeRemoveObservation(RedisConnection connection, String registrationId, byte[] observationId) {
        if (connection.del(toKey(OBS_TKN, observationId)) > 0L) {
            connection.lRem(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, observationId);
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(RedisConnection connection, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(OBS_TKNS_REGID_IDX, registrationId);

        // fetch all observations by token
        for (byte[] token : connection.lRange(regIdKey, 0, -1)) {
            byte[] obs = connection.get(toKey(OBS_TKN, token));
            if (obs != null) {
                removed.add(build(deserializeObs(obs)));
            }
            connection.del(toKey(OBS_TKN, token));
        }
        connection.del(regIdKey);

        return removed;
    }

    @Override
    public void setContext(Token token, EndpointContext correlationContext) {
        // In Leshan we always set context when we send the request, so this should not be needed to implement this.
    }

    private byte[] serializeObs(org.eclipse.californium.core.observe.Observation obs) {
        return ObservationSerDes.serialize(obs);
    }

    private org.eclipse.californium.core.observe.Observation deserializeObs(byte[] data) {
        return ObservationSerDes.deserialize(data);
    }

    private Observation build(org.eclipse.californium.core.observe.Observation cfObs) {
        if (cfObs == null)
            return null;

        return ObserveUtil.createLwM2mObservation(cfObs.getRequest());
    }

    /* *************** Expiration handling **************** */

    /**
     * Start regular cleanup of dead registrations.
     */
    @Override
    public synchronized void start() {
        if (!started) {
            started = true;
            cleanerTask = schedExecutor.scheduleAtFixedRate(new Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
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
            LOG.warn("Destroying RedisRegistrationStore was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try (var connection = connectionFactory.getConnection()) {
                Set<byte[]> endpointsExpired = connection.zRangeByScore(EXP_EP, Double.NEGATIVE_INFINITY,
                        System.currentTimeMillis(), 0, cleanLimit);

                for (byte[] endpoint : endpointsExpired) {
                    Registration r = deserializeReg(connection.get(toEndpointKey(endpoint)));
                    if (!r.isAlive(gracePeriod)) {
                        Deregistration dereg = removeRegistration(connection, r.getId(), true);
                        if (dereg != null)
                            expirationListener.registrationExpired(dereg.getRegistration(), dereg.getObservations());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        expirationListener = listener;
    }

    @Override
    public void setExecutor(ScheduledExecutorService executor) {
        // TODO should we reuse californium executor ?
    }
}
