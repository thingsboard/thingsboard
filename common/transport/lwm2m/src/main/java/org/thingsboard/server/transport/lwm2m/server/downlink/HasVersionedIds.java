// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface HasVersionedIds {

    String[] getVersionedIds();

    default String[] getObjectIds() {
        Set<String> objectIds = ConcurrentHashMap.newKeySet();
        for (String versionedId : getVersionedIds()) {
            objectIds.add(LwM2MTransportUtil.fromVersionedIdToObjectId(versionedId));
        }
        return objectIds.toArray(String[]::new);
    }

}
