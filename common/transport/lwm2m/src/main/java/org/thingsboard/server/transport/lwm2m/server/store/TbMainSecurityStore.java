// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import java.util.UUID;

public interface TbMainSecurityStore extends TbSecurityStore {

    void putX509(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException;

    void registerX509(String endpoint, String registrationId);

    void remove(String endpoint, String registrationId);

    void removeByDeviceId(UUID deviceId);
}
