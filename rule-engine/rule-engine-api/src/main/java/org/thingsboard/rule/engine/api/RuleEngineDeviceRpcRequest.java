// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * Created by ashvayka on 02.04.18.
 */
@Data
@Builder
public final class RuleEngineDeviceRpcRequest {

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final int requestId;
    private final UUID requestUUID;
    private final String originServiceId;
    private final boolean oneway;
    private final boolean persisted;
    private final String method;
    private final String body;
    private final long expirationTime;
    private final boolean restApiCall;
    private final String additionalInfo;
    private final Integer retries;
}
