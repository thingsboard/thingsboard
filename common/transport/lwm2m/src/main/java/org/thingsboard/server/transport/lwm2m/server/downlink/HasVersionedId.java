// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;

public interface HasVersionedId {

    String getVersionedId();

    default String getObjectId(){
        return LwM2MTransportUtil.fromVersionedIdToObjectId(getVersionedId());
    }

}
