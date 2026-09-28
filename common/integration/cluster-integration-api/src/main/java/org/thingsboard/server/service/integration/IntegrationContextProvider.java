// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.server.common.data.integration.Integration;

public interface IntegrationContextProvider {

    IntegrationContext buildIntegrationContext(Integration configuration);

}
