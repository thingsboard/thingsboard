// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.model;

import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

public interface LwM2MModelConfigService {

    void sendUpdates(LwM2mClient lwM2mClient);

    void sendUpdates(LwM2mClient lwM2mClient, LwM2MModelConfig modelConfig);

    void persistUpdates(String endpoint);

    void removeUpdates(String endpoint);
}
