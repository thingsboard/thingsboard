/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.RandomTokenGenerator;
import org.eclipse.californium.core.network.TokenGenerator;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.redis.serialization.ObservationSerDes;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class TbLwM2mRedisRegistrationStore implements RegistrationStore, Startable, Stoppable, Destroyable {
    /** Default time in seconds between 2 cleaning tasks (used to remove expired registration). */
    public static final long DEFAULT_CLEAN_PERIOD = 60;
    public static final int DEFAULT_CLEAN_LIMIT = 500;
    /** Defaut Extra time for registration lifetime in seconds */
    public static final long DEFAULT_GRACE_PERIOD = 0;

    // Redis key prefixes
    public static final String REG_EP = "REG:EP:"; // (Endpoint => Registration)
    private static final String REG_EP_REGID_IDX = "EP:REGID:"; // secondary index key (Registration ID => Endpoint)
    private static final String REG_EP_ADDR_IDX = "EP:ADDR:"; // secondary index key (Socket Address => Endpoint)
    private static final String REG_EP_IDENTITY = "EP:IDENTITY:"; // secondary index key (Identity => Endpoint)
    private static final String LOCK_EP = "LOCK:EP:";
    private static final byte[] OBS_TKN = "OBS:TKN:".getBytes(UTF_8);
    private static final byte[] OBS_TKN_GET_ALL = "OBS:TKN:*".getBytes(UTF_8);
    private static final String OBS_TKNS_REGID_IDX = "TKNS:REGID:"; // secondary index (token list by registration)
    private static final byte[] EXP_EP = "EXP:EP".getBytes(UTF_8); // a sorted set used for registration expiration
    // (expiration date, Endpoint)

    private final RegistrationSerDes registrationSerDes = new RegistrationSerDes();
    private final ObservationSerDes observationSerDes = new ObservationSerDes();
    private final org.eclipse.leshan.server.californium.observation.ObservationSerDes observationSerDesCoap =
            new org.eclipse.leshan.server.californium.observation.ObservationSerDes(new UdpDataParser(), new UdpDataSerializer());
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

    private final LwM2MTransportServerConfig config;
    private TokenGenerator tokenGenerator;

    private final LwM2mVersionedModelProvider modelProvider;

    public TbLwM2mRedisRegistrationStore(LwM2MTransportServerConfig config, RedisConnectionFactory connectionFactory, LwM2mVersionedModelProvider modelProvider) {
        this(config, connectionFactory, DEFAULT_CLEAN_PERIOD, DEFAULT_GRACE_PERIOD, DEFAULT_CLEAN_LIMIT, modelProvider); // default clean period 60s
    }

    public TbLwM2mRedisRegistrationStore(LwM2MTransportServerConfig config, RedisConnectionFactory connectionFactory, long cleanPeriodInSec, long lifetimeGracePeriodInSec, int cleanLimit, LwM2mVersionedModelProvider modelProvider) {
        this(config, connectionFactory, ThingsBoardExecutors.newSingleThreadScheduledExecutor(String.format("RedisRegistrationStore Cleaner (%ds)", cleanPeriodInSec)), cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit, modelProvider);
    }

    public TbLwM2mRedisRegistrationStore(LwM2MTransportServerConfig config, RedisConnectionFactory connectionFactory, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
                                         long lifetimeGracePeriodInSec, int cleanLimit, LwM2mVersionedModelProvider modelProvider) {
        this.connectionFactory = connectionFactory;
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.cleanLimit = cleanLimit;
        this.gracePeriod = lifetimeGracePeriodInSec;
        this.redisLock = new RedisLockRegistry(connectionFactory, "Registration");
        this.config = config;
        this.modelProvider = modelProvider;
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
                byte[] identity_idx = toRegIdentityKey(registration.getClientTransportData().getIdentity());
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
                    if (!oldRegistration.getClientTransportData().getIdentity().equals(registration.getClientTransportData().getIdentity())) {
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
        log.trace("updateRegistration [{}]", update);
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
                if (!r.getClientTransportData().getIdentity().equals(updatedRegistration.getClientTransportData().getIdentity())) {
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
    public Registration getRegistrationByIdentity(LwM2mIdentity identity) {
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
        removeSecondaryIndex(connection, toRegIdentityKey(r.getClientTransportData().getIdentity()), r.getEndpoint());
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

    private byte[] toRegIdentityKey(LwM2mIdentity identity) {
        return toKey(REG_EP_IDENTITY, identity.toString());
    }

    private byte[] toEndpointKey(String endpoint) {
        return toKey(REG_EP, endpoint);
    }

    private byte[] toEndpointKey(byte[] endpoint) {
        return toKey(REG_EP.getBytes(UTF_8), endpoint);
    }

    private byte[] serializeReg(Registration registration) {
        return registrationSerDes.bSerialize(registration);
    }

    private Registration deserializeReg(byte[] data) {
        return registrationSerDes.deserialize(data);
    }

    /* *************** Leshan Observation API **************** */

    /*
     * The observation is not persisted here, it is done by the Californium layer (in the implementation of the
     * org.eclipse.californium.core.observe.ObservationStore#add method)
     */

    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation, boolean addIfAbsent) {
        List<Observation> removed = new ArrayList<>();
        try (var connection = connectionFactory.getConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = connection.commands().get(toRegIdKey(registrationId));
            if (ep == null) {
                throw new IllegalStateException(String.format(
                        "can not add observation %s there is no registration with id %s", observation, registrationId));
            }

            Lock lock = null;
            String lockKey = toLockKey(ep);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();
                updateObservation(registrationId, observation, addIfAbsent, removed, connection);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
        return removed;
    }
    private void updateObservation(String registrationId, Observation observation, boolean addIfAbsent,
                                   List<Observation> removed, RedisConnection connection) {

        // Add and Get previous observation
        byte[] previousValue;
        byte[] key = toKey(OBS_TKN, observation.getId().getBytes());
        byte[] serializeObs = serializeObs(observation);
        // we analyze the present previous value
        if (addIfAbsent) {
            previousValue = connection.stringCommands().get(key);
            if (previousValue == null) {
                connection.stringCommands().set(key, serializeObs);
                previousValue = serializeObs;
            } else {
                connection.stringCommands().set(key, serializeObs);
            }
        } else {
            previousValue = connection.stringCommands().getSet(key, serializeObs);
        }

        // secondary index to get the list by registrationId
        connection.listCommands().lPush(toKey(OBS_TKNS_REGID_IDX, registrationId), observation.getId().getBytes());

        // log any collisions
        Observation previousObservation;
        if (previousValue != null && previousValue.length != 0) {
            previousObservation = deserializeObs(previousValue);
            log.warn("Token collision ? observation [{}] will be replaced by observation [{}] ",
                    previousObservation, observation);
        }

        // cancel existing observations for the same path and registration id.
        for (Observation obs : getObservations(connection, registrationId)) {
            if (areTheSamePaths(observation, obs) && !observation.getId().equals(obs.getId())) {
                removed.add(obs);
                unsafeRemoveObservation(connection, registrationId, obs.getId().getBytes());
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
    public Collection<Observation> getObservations(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return getObservations(connection, registrationId);
        }
    }

    @Override
    public Observation getObservation(String registrationId, ObservationIdentifier observationId) {
        return getObservations(registrationId).stream().filter(
                o -> o.getId().getAsHexString().equals(observationId.getAsHexString())).findFirst().get();
    }

    @Override
    public Observation getObservation(ObservationIdentifier observationId) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] observationValue = connection.get(toKey(OBS_TKN, observationId.getBytes()));
            return deserializeObs(observationValue);
        }
    }

    @Override
    public Observation removeObservation(String registrationId, ObservationIdentifier observationId) {
        return removeObservation(registrationId, observationId.getBytes());
    }

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

                Observation observation = get(new Token(observationId));
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


    private Collection<Observation> getObservations(RedisConnection connection, String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        for (byte[] token : connection.listCommands().lRange(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, -1)) {
            byte[] obs = connection.stringCommands().get(toKey(OBS_TKN, token));
            if (obs != null) {
                result.add(deserializeObs(obs));
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

    public Observation get(Token token) {
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

    private TokenGenerator getTokenGenerator() {
        if (this.tokenGenerator == null) {
            this.tokenGenerator = new RandomTokenGenerator(config.getCoapConfig());
        }
        return this.tokenGenerator;
    }

    private void unsafeRemoveObservation(RedisConnection connection, String registrationId, byte[] observationId) {
        if (connection.commands().del(toKey(OBS_TKN, observationId)) > 0L) {
            connection.listCommands().lRem(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, observationId);
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(RedisConnection connection, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(OBS_TKNS_REGID_IDX, registrationId);

        // fetch all observations by token
        for (byte[] token : connection.lRange(regIdKey, 0, -1)) {
            byte[] obs = connection.get(toKey(OBS_TKN, token));
            if (obs != null) {
                removed.add(deserializeObs(obs));
            }
            connection.del(toKey(OBS_TKN, token));
        }
        connection.del(regIdKey);

        return removed;
    }

    private byte[] serializeObs(Observation obs) {
        return observationSerDes.serialize(obs);
    }

    private void cancelObservation(Observation observation, String registrationId, List<Observation> removed, RedisConnection connection) {
        for (Observation obs : getObservations(connection, registrationId)) {
            cancelExistingObservation(connection, observation, obs, removed);
        }
    }

    private void cancelExistingObservation(RedisConnection connection, Observation observation, Observation obs, List<Observation> removed) {
        LwM2mPath pathObservation = ((SingleObservation) observation).getPath();
        LwM2mPath pathObs = ((SingleObservation) obs).getPath();
        if ((!pathObservation.equals(pathObs) && pathObs.startWith(pathObservation)) ||        // pathObservation = "3", pathObs = "3/0/9"
                (pathObservation.equals(pathObs) && !observation.getId().equals(obs.getId()))) {
            unsafeRemoveObservation(connection, obs.getRegistrationId(), obs.getId().getBytes());
            removed.add(obs);
        } else if (!pathObservation.equals(pathObs) && pathObservation.startWith(pathObs)) {    // pathObservation = "3/0/9", pathObs = "3"
            unsafeRemoveObservation(connection, obs.getRegistrationId(), observation.getId().getBytes());
        }
    }

    private Observation deserializeObs(byte[] data) {
        return data == null ? null : observationSerDes.deserialize(data);
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
            log.warn("Destroying RedisRegistrationStore was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try (var connection = connectionFactory.getConnection()) {
                Set<byte[]> endpointsExpired = connection.zRangeByScore(EXP_EP, Double.NEGATIVE_INFINITY,
                        System.currentTimeMillis(), 0, cleanLimit);

                for (byte[] endpoint : endpointsExpired) {
                    byte[] data = connection.get(toEndpointKey(endpoint));
                    if (data != null && data.length > 0) {
                        Registration r = deserializeReg(data);
                        if (!r.isAlive(gracePeriod)) {
                            Deregistration dereg = removeRegistration(connection, r.getId(), true);
                            if (dereg != null)
                                expirationListener.registrationExpired(dereg.getRegistration(), dereg.getObservations());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        expirationListener = listener;
    }

    public void setExecutor(ScheduledExecutorService executor) {
        // TODO should we reuse californium executor ?
    }
}
