// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Set;

public interface TbLwM2MClientStore {

    LwM2mClient get(String endpoint);

    Set<LwM2mClient> getAll();

    void put(LwM2mClient client);

    void remove(String endpoint);
}
