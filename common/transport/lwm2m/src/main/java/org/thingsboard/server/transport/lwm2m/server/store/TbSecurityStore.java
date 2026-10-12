// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

public interface TbSecurityStore extends SecurityStore {

    TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint);

    /**
     * Like {@link #getByIdentity(String)}, but tells a missing pre-shared key from a core that did not answer.
     *
     * @throws org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException if the core did not answer
     */
    default SecurityInfo getByPskIdentity(String pskIdentity) {
        return getByIdentity(pskIdentity);
    }

}
