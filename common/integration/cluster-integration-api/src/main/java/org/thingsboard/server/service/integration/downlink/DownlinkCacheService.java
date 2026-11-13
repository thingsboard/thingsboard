// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.downlink;

import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;

/**
 * Created by ashvayka on 22.02.18.
 */
public interface DownlinkCacheService {

    DownLinkMsg get(IntegrationId integrationId, EntityId entityId);

    DownLinkMsg put(IntegrationDownlinkMsg msg);

    void remove(IntegrationId integrationId, EntityId entityId);

}
