// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.apiusage;

import org.springframework.context.ApplicationListener;
import org.thingsboard.rule.engine.api.RuleEngineApiUsageStateService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

public interface TbApiUsageStateService extends TbApiUsageStateClient, RuleEngineApiUsageStateService, ApplicationListener<PartitionChangeEvent> {

    void process(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback);

    void onTenantProfileUpdate(TenantProfileId tenantProfileId);

    void onTenantUpdate(TenantId tenantId);

    void onTenantDelete(TenantId tenantId);

    void onCustomerDelete(CustomerId customerId);

    void onApiUsageStateUpdate(TenantId tenantId);
}
