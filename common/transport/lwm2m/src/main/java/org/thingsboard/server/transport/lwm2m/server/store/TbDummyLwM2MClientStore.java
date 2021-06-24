package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

public class TbDummyLwM2MClientStore implements TbLwM2MClientStore {
    @Override
    public LwM2mClient get(String endpoint) {
        return null;
    }

    @Override
    public void put(LwM2mClient client) {

    }

    @Override
    public void remove(String endpoint) {

    }
}
