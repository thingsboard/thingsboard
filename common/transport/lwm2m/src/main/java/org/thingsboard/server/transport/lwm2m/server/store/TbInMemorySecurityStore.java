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

import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TbInMemorySecurityStore implements TbEditableSecurityStore {
    // lock for the two maps
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();

    // by client end-point
    protected Map<String, TbLwM2MSecurityInfo> securityByEp = new HashMap<>();

    // by PSK identity
    protected Map<String, TbLwM2MSecurityInfo> securityByIdentity = new HashMap<>();

    public TbInMemorySecurityStore() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        readLock.lock();
        try {
            TbLwM2MSecurityInfo securityInfo = securityByEp.get(endpoint);
            if (securityInfo != null ) {
                if (SecurityMode.NO_SEC.equals(securityInfo.getSecurityMode())) {
                    return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                            SecurityMode.NO_SEC.toString().getBytes());
                } else {
                    return securityInfo.getSecurityInfo();
                }
            }
            else {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityInfo getByIdentity(String identity) {
        readLock.lock();
        try {
            TbLwM2MSecurityInfo securityInfo = securityByIdentity.get(identity);
            if (securityInfo != null) {
                return securityInfo.getSecurityInfo();
            } else {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
        return null;
    }

    @Override
    public void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException {
        writeLock.lock();
        try {
            String identity = null;
            if (tbSecurityInfo.getSecurityInfo() != null) {
                identity = tbSecurityInfo.getSecurityInfo().getPskIdentity();
                if (identity != null) {
                    TbLwM2MSecurityInfo infoByIdentity = securityByIdentity.get(identity);
                    if (infoByIdentity != null && !tbSecurityInfo.getSecurityInfo().getEndpoint().equals(infoByIdentity.getEndpoint())) {
                        throw new NonUniqueSecurityInfoException("PSK Identity " + identity + " is already used");
                    }
                    securityByIdentity.put(tbSecurityInfo.getSecurityInfo().getPskIdentity(), tbSecurityInfo);
                }
            }

            TbLwM2MSecurityInfo previous = securityByEp.put(tbSecurityInfo.getEndpoint(), tbSecurityInfo);
            if (previous != null && previous.getSecurityInfo() != null) {
                String previousIdentity = previous.getSecurityInfo().getPskIdentity();
                if (previousIdentity != null && !previousIdentity.equals(identity)) {
                    securityByIdentity.remove(previousIdentity);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String endpoint) {
        writeLock.lock();
        try {
            TbLwM2MSecurityInfo securityInfo = securityByEp.remove(endpoint);
            if (securityInfo != null && securityInfo.getSecurityInfo() != null && securityInfo.getSecurityInfo().getPskIdentity() != null) {
                securityByIdentity.remove(securityInfo.getSecurityInfo().getPskIdentity());
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        readLock.lock();
        try {
            return securityByEp.get(endpoint);
        } finally {
            readLock.unlock();
        }
    }

}
