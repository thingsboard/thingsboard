// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@Component
@Data
public class ConverterContextComponent {
    @Lazy
    @Autowired
    private EventService eventService;

    @Lazy
    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private IntegrationRateLimitService rateLimitService;
}
