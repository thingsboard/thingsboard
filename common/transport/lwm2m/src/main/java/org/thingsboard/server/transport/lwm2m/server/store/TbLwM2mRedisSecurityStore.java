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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.util.concurrent.locks.Lock;

import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.REG_EP;

@Slf4j
public class TbLwM2mRedisSecurityStore implements TbEditableSecurityStore {
    private static final String SEC_EP = "SEC#EP#";
    private static final String LOCK_EP = "LOCK#EP#";
    private static final String PSKID_SEC = "PSKID#SEC";

    private final RedisConnectionFactory connectionFactory;
    private final RedisLockRegistry redisLock;

    public TbLwM2mRedisSecurityStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        redisLock = new RedisLockRegistry(connectionFactory, "Security");
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(endpoint));
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data == null || data.length == 0) {
                return null;
            } else {
                TbLwM2MSecurityInfo tbLwM2MSecurityInfo = JavaSerDesUtil.decode(data);
                if (tbLwM2MSecurityInfo != null) {
                    if (SecurityMode.NO_SEC.equals(tbLwM2MSecurityInfo.getSecurityMode())){

                        // for tests: redis connect NoSec (securityInfo == null)
                        log.info("lwm2m redis securityStore (decode -ok). Endpoint: [{}], secMode: [NoSec] key: [{}], data [{}]", endpoint, SEC_EP, data);

                        return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                                SecurityMode.NO_SEC.toString().getBytes());
                    } else {
                        return tbLwM2MSecurityInfo.getSecurityInfo();
                    }
                } else if (SecurityMode.NO_SEC.equals(getSecurityModeByRegistration (connection,  endpoint))){

                    // for tests: redis connect NoSec (securityInfo == null)
                    log.info("lwm2m redis securityStore (decode - bad, registration - unsecure). Endpoint: [{}], secMode: [NoSec] key: [{}], data [{}]", endpoint, SEC_EP, data);

                    return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                            SecurityMode.NO_SEC.toString().getBytes());
                } else {

                    // for tests: redis connect NoSec (securityInfo == null)
                    log.info("lwm2m redis securityStore (decode - bad, registration is not unsecure) - return null. Endpoint: [{}], key: [{}], data [{}]", endpoint, SEC_EP, data);

                    return null;
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(identity));
            lock.lock();
            byte[] ep = connection.hGet(PSKID_SEC.getBytes(), identity.getBytes());
            if (ep == null) {
                return null;
            } else {
                byte[] data = connection.get((SEC_EP + new String(ep)).getBytes());
                if (data == null || data.length == 0) {
                    return null;
                } else {
                    return ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
        return null;
    }

    @Override
    public void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException {
        SecurityInfo info = tbSecurityInfo.getSecurityInfo();
        byte[] tbSecurityInfoSerialized = JavaSerDesUtil.encode(tbSecurityInfo);
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(tbSecurityInfo.getEndpoint());
            lock.lock();
            if (info != null && info.getPskIdentity() != null) {
                byte[] oldEndpointBytes = connection.hGet(PSKID_SEC.getBytes(), info.getPskIdentity().getBytes());
                if (oldEndpointBytes != null) {
                    String oldEndpoint = new String(oldEndpointBytes);
                    if (!oldEndpoint.equals(info.getEndpoint())) {
                        throw new NonUniqueSecurityInfoException("PSK Identity " + info.getPskIdentity() + " is already used");
                    }
                    connection.hSet(PSKID_SEC.getBytes(), info.getPskIdentity().getBytes(), info.getEndpoint().getBytes());
                }
            }

            byte[] previousData = connection.getSet((SEC_EP + tbSecurityInfo.getEndpoint()).getBytes(), tbSecurityInfoSerialized);

                // for tests: redis connect NoSec (securityInfo == null)
            log.info("lwm2m redis connect. Endpoint: [{}], secMode: [{}] key: [{}], tbSecurityInfoSerialized [{}]",
                    tbSecurityInfo.getEndpoint(), tbSecurityInfo.getSecurityMode().name(), SEC_EP, tbSecurityInfoSerialized);

            if (previousData != null && info != null) {
                String previousIdentity = ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(previousData)).getSecurityInfo().getPskIdentity();
                if (previousIdentity != null && !previousIdentity.equals(info.getPskIdentity())) {
                    connection.hDel(PSKID_SEC.getBytes(), previousIdentity.getBytes());
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                return JavaSerDesUtil.decode(data);
            } else {
                return null;
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void remove(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                SecurityInfo info = ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(data)).getSecurityInfo();
                if (info != null && info.getPskIdentity() != null) {
                    connection.hDel(PSKID_SEC.getBytes(), info.getPskIdentity().getBytes());
                }
                connection.del((SEC_EP + endpoint).getBytes());
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private String toLockKey(String endpoint) {
        return LOCK_EP + endpoint;
    }

    private SecurityMode getSecurityModeByRegistration (RedisConnection connection, String endpoint) {
        try {
            byte[] data = connection.get((REG_EP + endpoint).getBytes());
            JsonNode registrationNode = JacksonUtil.fromString(new String(data != null ? data : new byte[0]), JsonNode.class);
            String typeModeStr = registrationNode.get("transportdata").get("identity").get("type").asText();
            return "unsecure".equals(typeModeStr) ? SecurityMode.NO_SEC : null;
        } catch (Exception e) {
            log.error("Redis: Failed get SecurityMode by Registration, endpoint: [{}]", endpoint, e);
            return null;
        }

    }
}
