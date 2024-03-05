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

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.DEFAULT_CLEAN_LIMIT;
import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.DEFAULT_CLEAN_PERIOD;
import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.DEFAULT_GRACE_PERIOD;


@ExtendWith(MockitoExtension.class)
class TbLwM2mRedisRegistrationStoreTest {

    RedisConnectionFactory connectionFactory;
    RedisConnection connection;
    RedisLockRegistry lockRegistry;

    TbLwM2mRedisRegistrationStore registrationStore;

    @BeforeEach
    void setUp() {
        lockRegistry = mock(RedisLockRegistry.class);
        lenient().when(lockRegistry.obtain(any())).thenReturn(mock(Lock.class));
        connection = mock(RedisConnection.class);
        //when(connection.set(any(byte[].class), any(byte[].class))).
        connectionFactory = mock(RedisConnectionFactory.class);
        lenient().when(connectionFactory.getConnection()).thenReturn(connection);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("RedisRegistrationStore Cleaner (%ds)", DEFAULT_CLEAN_PERIOD)));
        registrationStore = new TbLwM2mRedisRegistrationStore(connectionFactory, executorService,
                DEFAULT_CLEAN_PERIOD, DEFAULT_GRACE_PERIOD, DEFAULT_CLEAN_LIMIT, lockRegistry);
    }

    @Test
    void testAddRegistrationWithNoOldRegistration() {
        setOldRegistration(null);
        Registration registration = buildRegistration();

        assertThat(registrationStore.addRegistration(registration)).isNull();

        byte[] endpoint = registration.getEndpoint().getBytes(UTF_8);
        verify(connection, times(1)).set(getRegIdKey(registration), endpoint);
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(1)).set(getRegIdentityKey(registration), endpoint);
        verify(connection, times(3)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(0)).del(any(byte[].class));
    }

    @Test
    void testAddRegistrationWithOldRegistrationEqualToCurrent(){
        var oldRegistration = buildRegistration();
        setOldRegistration(oldRegistration);
        Registration registration = buildRegistration();

        var deregistration = registrationStore.addRegistration(registration);

        assertThat(deregistration.getRegistration()).isEqualTo(oldRegistration);

        byte[] endpoint = registration.getEndpoint().getBytes(UTF_8);
        verify(connection, times(1)).set(getRegIdKey(registration), endpoint);
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(1)).set(getRegIdentityKey(registration), endpoint);
        verify(connection, times(3)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(1)).del(getTknsRegIdKey(oldRegistration));
        verify(connection, times(1)).del(any(byte[].class));
    }

    @Test
    void testAddRegistrationRemovesIndexes(){
        var oldRegistration = buildRegistration(Identity.unsecure(getTestAddress(1234)));
        setOldRegistration(oldRegistration);
        var registration = buildRegistration(Identity.unsecure(getTestAddress(2345)));

        var deregistration = registrationStore.addRegistration(registration);

        assertThat(deregistration.getRegistration()).isEqualTo(oldRegistration);
        byte[] endpoint = registration.getEndpoint().getBytes(UTF_8);
        verify(connection, times(1)).set(getRegIdKey(registration), endpoint);
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(1)).set(getRegIdentityKey(registration), endpoint);
        verify(connection, times(3)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(1)).del(getRegAddrKey(oldRegistration));
        verify(connection, times(1)).del(getRegIdentityKey(oldRegistration));
        verify(connection, times(1)).del(getTknsRegIdKey(oldRegistration));
        verify(connection, times(3)).del(any(byte[].class));
    }

    @Test
    void testUpdateRegistrationWhenNoRegistrationFound() {
        setOldRegistration(null);
        Registration registration = buildRegistration();
        RegistrationUpdate update = createUpdateFromRegistration(registration);

        assertThat(registrationStore.updateRegistration(update)).isNull();

        verify(connection, times(1)).get(getRegIdKey(registration));
        verify(connection, times(1)).get(any(byte[].class));
        verify(connection, times(0)).del(any(byte[].class));
    }

    @Test
    void testUpdateRegistrationWithSameRegistration() {
        Registration registration = buildRegistration();
        setOldRegistration(registration);
        RegistrationUpdate update = createUpdateFromRegistration(registration);

        assertThat(registrationStore.updateRegistration(update)).isNotNull();

        var endpoint = registration.getEndpoint().getBytes(UTF_8);
        // check registration and addressIndex here updated
        verify(connection, times(1)).set(eq(getEndpointKey(endpoint)), any(byte[].class));
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(2)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(0)).del(any(byte[].class));
    }

    @Test
    void testUpdateRegistrationWithRegistrationFromSecureIdentitiesWithDifferentAddress() {
        Registration oldRegistration = buildRegistration(Identity.psk(getTestAddress(1234), "my:psk"));
        setOldRegistration(oldRegistration);
        Registration newRegistration = buildRegistration(Identity.psk(getTestAddress(2345), "my:psk"));
        RegistrationUpdate update = createUpdateFromRegistration(newRegistration);
        assertThat(oldRegistration.getEndpoint()).isEqualTo(newRegistration.getEndpoint());

        assertThat(registrationStore.updateRegistration(update)).isNotNull();

        var endpoint = newRegistration.getEndpoint().getBytes(UTF_8);
        // check registration and addressIndex here updated
        verify(connection, times(1)).set(eq(getEndpointKey(endpoint)), any(byte[].class));
        verify(connection, times(1)).set(getRegAddrKey(newRegistration), endpoint);
        // check old AddrIndex has been removed
        verify(connection, times(1)).del(getRegAddrKey(oldRegistration));
        // check identityIndex has not been removed
        verify(connection, times(0)).del(getRegIdentityKey(oldRegistration));
        // check only one key (AddrIndex) in total was removed
        verify(connection, times(1)).del(any(byte[].class));
    }

    @Test
    void testGetRegistrationByIdentityReturnsRegistrationForSecureIdentityWithDifferentAddress() {
        Registration registration = buildRegistration(Identity.psk(getTestAddress(1234), "my:psk"));
        setOldRegistration(registration);
        Identity sameIdentityWithDifferentAddress = Identity.psk(getTestAddress(2345), "my:psk");

        Registration retrievedRegistration = registrationStore.getRegistrationByIdentity(sameIdentityWithDifferentAddress);

        assertThat(retrievedRegistration).isEqualTo(registration);
    }

    private void setOldRegistration(Registration oldRegistration){
        byte[] serializedRegistration = null;
        if (oldRegistration != null){
            byte[] endpoint = oldRegistration.getEndpoint().getBytes(UTF_8);
            // set the AddrIndex
            byte[] regAddrKey = getRegAddrKey(oldRegistration);
            lenient().when(connection.get(eq(regAddrKey))).thenReturn(endpoint);
            // set the IdentityIndex
            byte[] regIdentityKey = getRegIdentityKey(oldRegistration);
            lenient().when(connection.get(eq(regIdentityKey))).thenReturn(endpoint);
            // set the IdIndex
            byte[] regIdKey = getRegIdKey(oldRegistration);
            lenient().when(connection.get(eq(regIdKey))).thenReturn(endpoint);
            // set the registration
            serializedRegistration = RegistrationSerDes.bSerialize(oldRegistration);
            lenient().when(connection.get(eq(getEndpointKey(endpoint)))).thenReturn(serializedRegistration);
        }
        lenient().when(connection.getSet(any(byte[].class), any(byte[].class))).thenReturn(serializedRegistration);
    }

    private byte[] getRegAddrKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toRegAddrKey", registration.getSocketAddress());
    }

    private byte[] getRegIdentityKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toRegIdentityKey", registration.getIdentity());
    }

    private byte[] getRegIdKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toRegIdKey", registration.getId());
    }

    private byte[] getEndpointKey(byte[] endpoint){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toEndpointKey", endpoint);
    }

    private byte[] getTknsRegIdKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toKey", "TKNS:REGID:", registration.getId());
    }

    private static Registration buildRegistration() {
        return buildRegistration(Identity.psk(getTestAddress(), "my:psk"));
    }

    private static Registration buildRegistration(Identity identity){
        return new Registration.Builder("my_reg_id", "abcde", identity)
                .objectLinks(new Link[]{})
                .build();
    }

    private static RegistrationUpdate createUpdateFromRegistration(Registration registration){
        return new RegistrationUpdate(
                registration.getId(),
                registration.getIdentity(),
                registration.getLifeTimeInSec(),
                registration.getSmsNumber(),
                registration.getBindingMode(),
                registration.getObjectLinks(),
                registration.getAdditionalRegistrationAttributes()
        );
    }

    private static InetSocketAddress getTestAddress() {
        return getTestAddress(5684);
    }

    private static InetSocketAddress getTestAddress(int port) {
        try {
            return new InetSocketAddress(InetAddress.getByName("1.2.3.4"), port);
        } catch (UnknownHostException e) {
            throw new AssertionError("Cannot create test address");
        }
    }
}