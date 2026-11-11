// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.Data;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Optional;

@Data
public class LocalConverterContext implements ConverterContext {

    private final ConverterContextComponent ctx;
    private final TenantId tenantId;
    private final ConverterId converterId;

    @Override
    public String getServiceId() {
        return ctx.getServiceInfoProvider().getServiceId();
    }

    @Override
    public void saveEvent(Event event, IntegrationCallback<Void> callback) {
        DonAsynchron.withCallback(ctx.getEventService().saveAsync(event), res -> callback.onSuccess(null), callback::onError);
    }

    @Override
    public Optional<IntegrationRateLimitService> getRateLimitService() {
        return Optional.of(ctx.getRateLimitService());
    }
}
