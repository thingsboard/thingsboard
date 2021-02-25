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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.ObservationStoreException;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.redis.JedisLock;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.SingleInstanceJedisLock;
import org.eclipse.leshan.server.redis.serialization.ObservationSerDes;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Transaction;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private final JedisLock lock;

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
        this(connectionFactory, schedExecutor, cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit, new SingleInstanceJedisLock());
    }

    /**
     * @since 1.1
     */
    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
                                         long lifetimeGracePeriodInSec, int cleanLimit, JedisLock redisLock) {
        this.connectionFactory = connectionFactory;
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.cleanLimit = cleanLimit;
        this.gracePeriod = lifetimeGracePeriodInSec;
        this.lock = redisLock;
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

    private byte[] toLockKey(String endpoint) {
        return toKey(LOCK_EP, endpoint);
    }

    private byte[] toLockKey(byte[] endpoint) {
        return toKey(LOCK_EP.getBytes(UTF_8), endpoint);
    }

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] lockValue = null;
            byte[] lockKey = toLockKey(registration.getEndpoint());

            try {
                lockValue = lock.acquire(j, lockKey);

                // add registration
                byte[] k = toEndpointKey(registration.getEndpoint());
                byte[] old = j.getSet(k, serializeReg(registration));

                // add registration: secondary indexes
                byte[] regid_idx = toRegIdKey(registration.getId());
                j.set(regid_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] addr_idx = toRegAddrKey(registration.getSocketAddress());
                j.set(addr_idx, registration.getEndpoint().getBytes(UTF_8));

                // Add or update expiration
                addOrUpdateExpiration(j, registration);

                if (old != null) {
                    Registration oldRegistration = deserializeReg(old);
                    // remove old secondary index
                    if (!registration.getId().equals(oldRegistration.getId()))
                        j.del(toRegIdKey(oldRegistration.getId()));
                    if (!oldRegistration.getSocketAddress().equals(registration.getSocketAddress())) {
                        removeAddrIndex(j, oldRegistration);
                    }
                    // remove old observation
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(j, oldRegistration.getId());

                    return new Deregistration(oldRegistration, obsRemoved);
                }

                return null;
            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {

            // Fetch the registration ep by registration ID index
            byte[] ep = j.get(toRegIdKey(update.getRegistrationId()));
            if (ep == null) {
                return null;
            }

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(ep);
            try {
                lockValue = lock.acquire(j, lockKey);

                // Fetch the registration
                byte[] data = j.get(toEndpointKey(ep));
                if (data == null) {
                    return null;
                }

                Registration r = deserializeReg(data);

                Registration updatedRegistration = update.update(r);

                // Store the new registration
                j.set(toEndpointKey(updatedRegistration.getEndpoint()), serializeReg(updatedRegistration));

                // Add or update expiration
                addOrUpdateExpiration(j, updatedRegistration);

                // Update secondary index :
                // If registration is already associated to this address we don't care as we only want to keep the most
                // recent binding.
                byte[] addr_idx = toRegAddrKey(updatedRegistration.getSocketAddress());
                j.set(addr_idx, updatedRegistration.getEndpoint().getBytes(UTF_8));
                if (!r.getSocketAddress().equals(updatedRegistration.getSocketAddress())) {
                    removeAddrIndex(j, r);
                }

                return new UpdatedRegistration(r, updatedRegistration);

            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            return getRegistration(j, registrationId);
        }
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        Validate.notNull(endpoint);
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] data = j.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        Validate.notNull(address);
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] ep = j.get(toRegAddrKey(address));
            if (ep == null) {
                return null;
            }
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        return new TbLwM2mRedisRegistrationStore.RedisIterator(connectionFactory, new ScanParams().match(REG_EP + "*").count(100));
    }

    protected class RedisIterator implements Iterator<Registration> {

        private RedisConnectionFactory connectionFactory;
        private ScanParams scanParams;

        private String cursor;
        private List<Registration> scanResult;

        public RedisIterator(RedisConnectionFactory connectionFactory, ScanParams scanParams) {
            this.connectionFactory = connectionFactory;
            this.scanParams = scanParams;
            // init scan result
            scanNext("0");
        }

        private void scanNext(String cursor) {
            try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
                do {
                    ScanResult<byte[]> sr = j.scan(cursor.getBytes(), scanParams);

                    this.scanResult = new ArrayList<>();
                    if (sr.getResult() != null && !sr.getResult().isEmpty()) {
                        for (byte[] value : j.mget(sr.getResult().toArray(new byte[][]{}))) {
                            this.scanResult.add(deserializeReg(value));
                        }
                    }

                    cursor = sr.getCursor();
                } while (!"0".equals(cursor) && scanResult.isEmpty());

                this.cursor = cursor;
            }
        }

        @Override
        public boolean hasNext() {
            if (!scanResult.isEmpty()) {
                return true;
            }
            if ("0".equals(cursor)) {
                // no more elements to scan
                return false;
            }

            // read more elements
            scanNext(cursor);
            return !scanResult.isEmpty();
        }

        @Override
        public Registration next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return scanResult.remove(0);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            return removeRegistration(j, registrationId, false);
        }
    }

    private Deregistration removeRegistration(Jedis j, String registrationId, boolean removeOnlyIfNotAlive) {
        // fetch the client ep by registration ID index
        byte[] ep = j.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }

        byte[] lockValue = null;
        byte[] lockKey = toLockKey(ep);
        try {
            lockValue = lock.acquire(j, lockKey);

            // fetch the client
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            Registration r = deserializeReg(data);

            if (!removeOnlyIfNotAlive || !r.isAlive(gracePeriod)) {
                long nbRemoved = j.del(toRegIdKey(r.getId()));
                if (nbRemoved > 0) {
                    j.del(toEndpointKey(r.getEndpoint()));
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(j, r.getId());
                    removeAddrIndex(j, r);
                    removeExpiration(j, r);
                    return new Deregistration(r, obsRemoved);
                }
            }
            return null;
        } finally {
            lock.release(j, lockKey, lockValue);
        }
    }

    private void removeAddrIndex(Jedis j, Registration registration) {
        // Watch the key to remove.
        byte[] regAddrKey = toRegAddrKey(registration.getSocketAddress());
        j.watch(regAddrKey);

        byte[] epFromAddr = j.get(regAddrKey);
        // Delete the key if needed.
        if (Arrays.equals(epFromAddr, registration.getEndpoint().getBytes(UTF_8))) {
            // Try to delete the key
            Transaction transaction = j.multi();
            transaction.del(regAddrKey);
            transaction.exec();
            // if transaction failed this is not an issue as the socket address is probably reused and we don't neeed to
            // delete it anymore.
        } else {
            // the key must not be deleted.
            j.unwatch();
        }
    }

    private void addOrUpdateExpiration(Jedis j, Registration registration) {
        j.zadd(EXP_EP, registration.getExpirationTimeStamp(gracePeriod), registration.getEndpoint().getBytes(UTF_8));
    }

    private void removeExpiration(Jedis j, Registration registration) {
        j.zrem(EXP_EP, registration.getEndpoint().getBytes(UTF_8));
    }

    private byte[] toRegIdKey(String registrationId) {
        return toKey(REG_EP_REGID_IDX, registrationId);
    }

    private byte[] toRegAddrKey(InetSocketAddress addr) {
        return toKey(REG_EP_ADDR_IDX, addr.getAddress().toString() + ":" + addr.getPort());
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
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = j.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(ep);

            try {
                lockValue = lock.acquire(j, lockKey);

                // cancel existing observations for the same path and registration id.
                for (Observation obs : getObservations(j, registrationId)) {
                    if (observation.getPath().equals(obs.getPath())
                            && !Arrays.equals(observation.getId(), obs.getId())) {
                        removed.add(obs);
                        unsafeRemoveObservation(j, registrationId, obs.getId());
                    }
                }

            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
        return removed;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = j.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            // remove observation
            byte[] lockValue = null;
            byte[] lockKey = toLockKey(ep);
            try {
                lockValue = lock.acquire(j, lockKey);

                Observation observation = build(get(new Token(observationId)));
                if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                    unsafeRemoveObservation(j, registrationId, observationId);
                    return observation;
                }
                return null;

            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Observation getObservation(String registrationId, byte[] observationId) {
        return build(get(new Token(observationId)));
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            return getObservations(j, registrationId);
        }
    }

    private Collection<Observation> getObservations(Jedis j, String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        for (byte[] token : j.lrange(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, -1)) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs != null) {
                result.add(build(deserializeObs(obs)));
            }
        }
        return result;
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            // check registration exists
            Registration registration = getRegistration(j, registrationId);
            if (registration == null)
                return Collections.emptyList();

            // get endpoint and create lock
            String endpoint = registration.getEndpoint();
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = lock.acquire(j, lockKey);

                return unsafeRemoveAllObservations(j, registrationId);
            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    /* *************** Californium ObservationStore API **************** */

    @Override
    public org.eclipse.californium.core.observe.Observation putIfAbsent(Token token,
                                                                        org.eclipse.californium.core.observe.Observation obs) throws ObservationStoreException {
        return add(token, obs, true);
    }

    @Override
    public org.eclipse.californium.core.observe.Observation put(Token token,
                                                                org.eclipse.californium.core.observe.Observation obs) throws ObservationStoreException {
        return add(token, obs, false);
    }

    private org.eclipse.californium.core.observe.Observation add(Token token,
                                                                 org.eclipse.californium.core.observe.Observation obs, boolean ifAbsent) throws ObservationStoreException {
        String endpoint = ObserveUtil.validateCoapObservation(obs);
        org.eclipse.californium.core.observe.Observation previousObservation = null;

        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = lock.acquire(j, lockKey);

                String registrationId = ObserveUtil.extractRegistrationId(obs);
                if (!j.exists(toRegIdKey(registrationId)))
                    throw new ObservationStoreException("no registration for this Id");
                byte[] key = toKey(OBS_TKN, obs.getRequest().getToken().getBytes());
                byte[] serializeObs = serializeObs(obs);
                byte[] previousValue = null;
                if (ifAbsent) {
                    previousValue = j.get(key);
                    if (previousValue == null || previousValue.length == 0) {
                        j.set(key, serializeObs);
                    } else {
                        return deserializeObs(previousValue);
                    }
                } else {
                    previousValue = j.getSet(key, serializeObs);
                }

                // secondary index to get the list by registrationId
                j.lpush(toKey(OBS_TKNS_REGID_IDX, registrationId), obs.getRequest().getToken().getBytes());

                // log any collisions
                if (previousValue != null && previousValue.length != 0) {
                    previousObservation = deserializeObs(previousValue);
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
        return previousObservation;
    }

    @Override
    public void remove(Token token) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] tokenKey = toKey(OBS_TKN, token.getBytes());

            // fetch the observation by token
            byte[] serializedObs = j.get(tokenKey);
            if (serializedObs == null)
                return;

            org.eclipse.californium.core.observe.Observation obs = deserializeObs(serializedObs);
            String registrationId = ObserveUtil.extractRegistrationId(obs);
            Registration registration = getRegistration(j, registrationId);
            if (registration == null) {
                LOG.warn("Unable to remove observation {}, registration {} does not exist anymore", obs.getRequest(),
                        registrationId);
                return;
            }

            String endpoint = registration.getEndpoint();
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = lock.acquire(j, lockKey);

                unsafeRemoveObservation(j, registrationId, token.getBytes());
            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }

    }

    @Override
    public org.eclipse.californium.core.observe.Observation get(Token token) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] obs = j.get(toKey(OBS_TKN, token.getBytes()));
            if (obs == null) {
                return null;
            } else {
                return deserializeObs(obs);
            }
        }
    }

    /* *************** Observation utility functions **************** */

    private Registration getRegistration(Jedis j, String registrationId) {
        byte[] ep = j.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }
        byte[] data = j.get(toEndpointKey(ep));
        if (data == null) {
            return null;
        }

        return deserializeReg(data);
    }

    private void unsafeRemoveObservation(Jedis j, String registrationId, byte[] observationId) {
        if (j.del(toKey(OBS_TKN, observationId)) > 0L) {
            j.lrem(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, observationId);
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(Jedis j, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(OBS_TKNS_REGID_IDX, registrationId);

        // fetch all observations by token
        for (byte[] token : j.lrange(regIdKey, 0, -1)) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs != null) {
                removed.add(build(deserializeObs(obs)));
            }
            j.del(toKey(OBS_TKN, token));
        }
        j.del(regIdKey);

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
            cleanerTask = schedExecutor.scheduleAtFixedRate(new TbLwM2mRedisRegistrationStore.Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
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

            try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
                Set<byte[]> endpointsExpired = j.zrangeByScore(EXP_EP, Double.NEGATIVE_INFINITY,
                        System.currentTimeMillis(), 0, cleanLimit);

                for (byte[] endpoint : endpointsExpired) {
                    Registration r = deserializeReg(j.get(toEndpointKey(endpoint)));
                    if (!r.isAlive(gracePeriod)) {
                        Deregistration dereg = removeRegistration(j, r.getId(), true);
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
