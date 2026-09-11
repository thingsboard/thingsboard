// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.AggIntervalEntry;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CalculatedFieldProcessingService {

    ListenableFuture<Map<String, ArgumentEntry>> fetchArguments(CalculatedFieldCtx ctx, EntityId entityId);

    Map<String, ArgumentEntry> fetchDynamicArgsFromDb(CalculatedFieldCtx ctx, EntityId entityId);

    Optional<PropagationArgumentEntry> fetchPropagationArgumentFromDb(CalculatedFieldCtx ctx, EntityId entityId);

    List<EntityId> fetchRelatedEntities(CalculatedFieldCtx ctx, EntityId entityId);

    Map<String, ArgumentEntry> fetchArgsFromDb(TenantId tenantId, EntityId entityId, Map<String, Argument> arguments);

    void processResult(TenantId tenantId, EntityId entityId, String cfName, CalculatedFieldResult result, List<CalculatedFieldId> cfIds, TbCallback callback);

    ArgumentEntry fetchMetricDuringInterval(TenantId tenantId, EntityId entityId, String argKey, AggMetric metric, AggIntervalEntry interval);

    void pushMsgToLinks(CalculatedFieldTelemetryMsg msg, List<CalculatedFieldEntityCtxId> linkedCalculatedFields, TbCallback callback);

}
