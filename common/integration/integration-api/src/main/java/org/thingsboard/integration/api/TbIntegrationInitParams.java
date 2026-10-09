// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.server.common.data.integration.Integration;

@Data
@AllArgsConstructor
public class TbIntegrationInitParams {

    private final IntegrationContext context;

    private final Integration configuration;

    private final TBUplinkDataConverter uplinkConverter;

    private final TBDownlinkDataConverter downlinkConverter;
}
