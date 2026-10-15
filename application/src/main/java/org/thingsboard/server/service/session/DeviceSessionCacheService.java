// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.session;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;

/**
 * Created by ashvayka on 29.10.18.
 */
public interface DeviceSessionCacheService {

    DeviceSessionsCacheEntry get(DeviceId deviceId);

    DeviceSessionsCacheEntry put(DeviceId deviceId, DeviceSessionsCacheEntry sessions);

}
