// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
class IntegrationCacheEvictEvent {

    private final IntegrationId integrationId;
    private Integration savedIntegration;

}
