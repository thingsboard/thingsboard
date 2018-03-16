/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.*;

@Slf4j
public class TbGetTenantAttributeNode extends TbEntityGetAttrNode<TenantId> {

    @Override
    protected ListenableFuture<TenantId> findEntityAsync(TbContext ctx, EntityId originator) {

        switch (originator.getEntityType()) {
            case TENANT:
                return Futures.immediateFuture((TenantId) originator);
            case CUSTOMER:
                return getTenantAsync(ctx.getCustomerService().findCustomerByIdAsync((CustomerId) originator));
            case USER:
                return getTenantAsync(ctx.getUserService().findUserByIdAsync((UserId) originator));
            case RULE:
                return getTenantAsync(ctx.getRuleService().findRuleByIdAsync((RuleId) originator));
            case PLUGIN:
                return getTenantAsync(ctx.getPluginService().findPluginByIdAsync((PluginId) originator));
            case ASSET:
                return getTenantAsync(ctx.getAssetService().findAssetByIdAsync((AssetId) originator));
            case DEVICE:
                return getTenantAsync(ctx.getDeviceService().findDeviceByIdAsync((DeviceId) originator));
            case ALARM:
                return getTenantAsync(ctx.getAlarmService().findAlarmByIdAsync((AlarmId) originator));
            case RULE_CHAIN:
                return getTenantAsync(ctx.getRuleChainService().findRuleChainByIdAsync((RuleChainId) originator));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType " + originator));
        }
    }

    private <T extends HasTenantId> ListenableFuture<TenantId> getTenantAsync(ListenableFuture<T> future) {
        return Futures.transform(future, (AsyncFunction<HasTenantId, TenantId>) in -> Futures.immediateFuture(in.getTenantId()));
    }

}
