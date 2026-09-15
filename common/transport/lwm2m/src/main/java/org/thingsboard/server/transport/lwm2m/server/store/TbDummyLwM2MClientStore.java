// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Collections;
import java.util.Set;

public class TbDummyLwM2MClientStore implements TbLwM2MClientStore {
    @Override
    public LwM2mClient get(String endpoint) {
        return null;
    }

    @Override
    public Set<LwM2mClient> getAll() {
        return Collections.emptySet();
    }

    @Override
    public void put(LwM2mClient client) {

    }

    @Override
    public void remove(String endpoint) {

    }
}
