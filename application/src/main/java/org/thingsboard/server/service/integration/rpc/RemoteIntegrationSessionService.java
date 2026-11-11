// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import org.thingsboard.server.common.data.id.IntegrationId;

public interface RemoteIntegrationSessionService {

    IntegrationSession findIntegrationSession(IntegrationId integrationId);

    IntegrationSession putIntegrationSession(IntegrationId integrationId, IntegrationSession session);

    void removeIntegrationSession(IntegrationId integrationId);
}
