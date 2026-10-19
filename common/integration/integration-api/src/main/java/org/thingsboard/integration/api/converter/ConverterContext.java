// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.server.common.data.event.Event;

import java.util.Optional;

public interface ConverterContext {

    /**
     * Returns current server address that is used mostly for logging.
     *
     * @return server address
     */
    String getServiceId();

    /**
     * Saves event to ThingsBoard based on provided type and body on behalf of the converter
     */
    void saveEvent(Event event, IntegrationCallback<Void> callback);

    Optional<IntegrationRateLimitService> getRateLimitService();
}
