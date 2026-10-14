// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

public interface IntegrationStatisticsService {

    void onIntegrationStateUpdate(IntegrationType integrationType, ComponentLifecycleEvent state, boolean success);

    void onIntegrationsCountUpdate(IntegrationType integrationType, int started, int failed);

    void onUplinkMsg(IntegrationType integrationType, boolean success);

    void onDownlinkMsg(IntegrationType integrationType, boolean success);

    void printStats();

    void reset();

}
