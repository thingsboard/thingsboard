// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class TbQueueCalculatedFieldSettings {

    @Value("${queue.calculated_fields.event_topic}")
    private String eventTopic;

    @Value("${queue.calculated_fields.state_topic}")
    private String stateTopic;

    @Value("${queue.calculated_fields.init_tenant_fetch_pack_size:1000}")
    private int initTenantFetchPackSize;

}
