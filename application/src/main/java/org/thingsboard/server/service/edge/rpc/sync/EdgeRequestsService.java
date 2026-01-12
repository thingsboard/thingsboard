/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.sync;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;

public interface EdgeRequestsService {

    @Deprecated(since = "3.9.1", forRemoval = true)
    ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg);

    ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg);

    ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg);

    ListenableFuture<Void> processCalculatedFieldRequestMsg(TenantId tenantId, Edge edge, CalculatedFieldRequestMsg calculatedFieldRequestMsg);

    @Deprecated(since = "3.9.1", forRemoval = true)
    ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg);

    @Deprecated(since = "3.9.1", forRemoval = true)
    ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg);

    @Deprecated(since = "3.9.1", forRemoval = true)
    ListenableFuture<Void> processWidgetBundleTypesRequestMsg(TenantId tenantId, Edge edge, WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg);

    @Deprecated(since = "3.9.1", forRemoval = true)
    ListenableFuture<Void> processEntityViewsRequestMsg(TenantId tenantId, Edge edge, EntityViewsRequestMsg entityViewsRequestMsg);

}
