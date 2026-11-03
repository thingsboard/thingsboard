// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.secure.TbX509DtlsSessionInfo;

import java.util.concurrent.ConcurrentHashMap;

public class TbL2M2MDtlsSessionInMemoryStore implements TbLwM2MDtlsSessionStore {

    private final ConcurrentHashMap<String, TbX509DtlsSessionInfo> store = new ConcurrentHashMap<>();

    @Override
    public void put(String endpoint, TbX509DtlsSessionInfo msg) {
        store.put(endpoint, msg);
    }

    @Override
    public TbX509DtlsSessionInfo get(String endpoint) {
        return store.get(endpoint);
    }

    @Override
    public void remove(String endpoint) {
        store.remove(endpoint);
    }
}
