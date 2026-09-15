// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum TransportLimitsType {

    TENANT_LIMITS(
            DefaultTenantProfileConfiguration::getTransportTenantMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportTenantTelemetryMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportTenantTelemetryDataPointsRateLimit
    ),
    DEVICE_LIMITS(
            DefaultTenantProfileConfiguration::getTransportDeviceMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportDeviceTelemetryMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportDeviceTelemetryDataPointsRateLimit
    ),
    GATEWAY_LIMITS(
            DefaultTenantProfileConfiguration::getTransportGatewayMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportGatewayTelemetryMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportGatewayTelemetryDataPointsRateLimit
    ),
    GATEWAY_DEVICE_LIMITS(
            DefaultTenantProfileConfiguration::getTransportGatewayDeviceMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportGatewayDeviceTelemetryMsgRateLimit,
            DefaultTenantProfileConfiguration::getTransportGatewayDeviceTelemetryDataPointsRateLimit
    );

    private final Function<DefaultTenantProfileConfiguration, String> regularMsgRateLimit;
    private final Function<DefaultTenantProfileConfiguration, String> telemetryMsgRateLimit;
    private final Function<DefaultTenantProfileConfiguration, String> telemetryDataPointsRateLimit;

}
