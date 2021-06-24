package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

public interface TbLwM2MClientStore {

    LwM2mClient get(String endpoint);

    void put(LwM2mClient client);

    void remove(String endpoint);
}
