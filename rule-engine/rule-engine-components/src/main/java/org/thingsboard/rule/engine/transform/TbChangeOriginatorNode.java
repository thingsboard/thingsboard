/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesAlarmOriginatorIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesCustomerIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesTenantIdAsyncLoader;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "change originator",
        configClazz = TbChangeOriginatorNodeConfiguration.class,
        nodeDescription = "Change Message Originator To Tenant/Customer/Related Entity/Alarm Originator/Device/Asset",
        nodeDetails = "Related Entity found using configured relation direction and Relation Type. " +
                "If multiple Related Entities are found, only first Entity is used as new Originator, other entities are discarded.<br/>" +
                "Alarm Originator found only in case original Originator is <code>Alarm</code> entity.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeChangeOriginatorConfig",
        icon = "find_replace"
)
public class TbChangeOriginatorNode extends TbAbstractTransformNode {

    protected static final String CUSTOMER_SOURCE = "CUSTOMER";
    protected static final String TENANT_SOURCE = "TENANT";
    protected static final String RELATED_SOURCE = "RELATED";
    protected static final String ALARM_ORIGINATOR_SOURCE = "ALARM_ORIGINATOR";
    protected static final String DEVICE = "DEVICE";
    protected static final String ASSET = "ASSET";

    private static final ReentrantLock entityCreationLock = new ReentrantLock();

    private TbChangeOriginatorNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbChangeOriginatorNodeConfiguration.class);
        validateConfig(config);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends EntityId> newOriginator = getNewOriginator(ctx, msg);
        return Futures.transform(newOriginator, n -> {
            if (n == null || n.isNullUid()) {
                return null;
            }
            return Collections.singletonList((ctx.transformMsg(msg, msg.getType(), n, msg.getMetaData(), msg.getData())));
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<? extends EntityId> getNewOriginator(TbContext ctx, TbMsg msg) {
        EntityId original = msg.getOriginator();
        switch (config.getOriginatorSource()) {
            case CUSTOMER_SOURCE:
                return EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctx, original);
            case TENANT_SOURCE:
                return EntitiesTenantIdAsyncLoader.findEntityIdAsync(ctx, original);
            case RELATED_SOURCE:
                return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, original, config.getRelationsQuery());
            case ALARM_ORIGINATOR_SOURCE:
                return EntitiesAlarmOriginatorIdAsyncLoader.findEntityIdAsync(ctx, original);
            case DEVICE:
                return getOrCreateDevice(ctx, msg);
            case ASSET:
                return getOrCreateAsset(ctx, msg);
            default:
                return Futures.immediateFailedFuture(new IllegalStateException("Unexpected originator source " + config.getOriginatorSource()));
        }
    }

    private ListenableFuture<DeviceId> getOrCreateDevice(TbContext ctx, TbMsg msg) {
        String deviceName = TbNodeUtils.processPattern(config.getEntityNamePattern(), msg);
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), deviceName);
        if (device == null) {
            try {
                entityCreationLock.lock();
                TenantId tenantId = ctx.getTenantId();
                device = ctx.getDeviceService().findDeviceByTenantIdAndName(tenantId, deviceName);
                if (device == null) {
                    device = new Device();
                    device.setName(deviceName);
                    device.setType(TbNodeUtils.processPattern(config.getEntityTypePattern(), msg));
                    device.setTenantId(tenantId);
                    String labelPattern = config.getEntityLabelPattern();
                    if (!StringUtils.isEmpty(labelPattern)) {
                        device.setLabel(TbNodeUtils.processPattern(labelPattern, msg));
                    }

                    device = ctx.getTbDeviceService().save(device);
                }
            } catch (ThingsboardException e) {
                return Futures.immediateFailedFuture(e);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return Futures.immediateFuture(device.getId());
    }

    private ListenableFuture<AssetId> getOrCreateAsset(TbContext ctx, TbMsg msg) {
        String assetName = TbNodeUtils.processPattern(config.getEntityNamePattern(), msg);
        Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), assetName);
        if (asset == null) {
            try {
                entityCreationLock.lock();
                TenantId tenantId = ctx.getTenantId();
                asset = ctx.getAssetService().findAssetByTenantIdAndName(tenantId, assetName);
                if (asset == null) {
                    asset = new Asset();
                    asset.setName(assetName);
                    asset.setType(TbNodeUtils.processPattern(config.getEntityTypePattern(), msg));
                    asset.setTenantId(tenantId);
                    String labelPattern = config.getEntityLabelPattern();
                    if (!StringUtils.isEmpty(labelPattern)) {
                        asset.setLabel(TbNodeUtils.processPattern(labelPattern, msg));
                    }
                    asset = ctx.getTbAssetService().save(asset);
                }
            } catch (ThingsboardException e) {
                return Futures.immediateFailedFuture(e);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return Futures.immediateFuture(asset.getId());
    }

    private void validateConfig(TbChangeOriginatorNodeConfiguration conf) {
        HashSet<String> knownSources =
                Sets.newHashSet(CUSTOMER_SOURCE, TENANT_SOURCE, RELATED_SOURCE, ALARM_ORIGINATOR_SOURCE, DEVICE, ASSET);
        if (!knownSources.contains(conf.getOriginatorSource())) {
            log.error("Unsupported source [{}] for TbChangeOriginatorNode", conf.getOriginatorSource());
            throw new IllegalArgumentException("Unsupported source TbChangeOriginatorNode " + conf.getOriginatorSource());
        }

        if (conf.getOriginatorSource().equals(RELATED_SOURCE)) {
            if (conf.getRelationsQuery() == null) {
                log.error("Related source for TbChangeOriginatorNode should have relations query. Actual [{}]",
                        conf.getRelationsQuery());
                throw new IllegalArgumentException("Wrong config for RElated Source in TbChangeOriginatorNode" + conf.getOriginatorSource());
            }
        }

    }

    @Override
    public void destroy() {

    }
}
