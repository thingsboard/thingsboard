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

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.*;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
          name = "originator attributes",
          configClazz = TbGetAttributesNodeConfiguration.class,
          nodeDescription = "Add Message Originator Attributes or Latest Telemetry into Message Metadata",
          nodeDetails = "If Attributes enrichment configured, <b>CLIENT/SHARED/SERVER</b> attributes are added into Message metadata " +
                "with specific prefix: <i>cs/shared/ss</i>. To access those attributes in other nodes this template can be used " +
                "<code>metadata.cs_temperature</code> or <code>metadata.shared_limit</code> " +
                "If Latest Telemetry enrichment configured, latest telemetry added into metadata without prefix.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeOriginatorAttributesConfig")
public class TbGetAttributesNode implements TbNode {

    private TbGetAttributesNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetAttributesNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        if (CollectionUtils.isNotEmpty(config.getLatestTsKeyNames())) {
            withCallback(getLatestTelemetry(ctx, msg, config.getLatestTsKeyNames()),
                    i -> ctx.tellNext(msg),
                    t -> ctx.tellError(msg, t));
        } else {
            ListenableFuture<List<Void>> future = Futures.allAsList(
                    putAttrAsync(ctx, msg, CLIENT_SCOPE, config.getClientAttributeNames(), "cs_"),
                    putAttrAsync(ctx, msg, SHARED_SCOPE, config.getSharedAttributeNames(), "shared_"),
                    putAttrAsync(ctx, msg, SERVER_SCOPE, config.getServerAttributeNames(), "ss_"));

            withCallback(future, i -> ctx.tellNext(msg), t -> ctx.tellError(msg, t));
        }
    }

    private ListenableFuture<Void> putAttrAsync(TbContext ctx, TbMsg msg, String scope, List<String> keys, String prefix) {
        if (keys == null) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(msg.getOriginator(), scope, keys);
        return Futures.transform(latest, (Function<? super List<AttributeKvEntry>, Void>) l -> {
            l.forEach(r -> msg.getMetaData().putValue(prefix + r.getKey(), r.getValueAsString()));
            return null;
        });
    }

    private ListenableFuture<Void> getLatestTelemetry(TbContext ctx, TbMsg msg, List<String> keys) {
        if (keys == null) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(msg.getOriginator(), keys);
        return Futures.transform(latest, (Function<? super List<TsKvEntry>, Void>) l -> {
            l.forEach(r -> msg.getMetaData().putValue(r.getKey(), r.getValueAsString()));
            return null;
        });
    }

    @Override
    public void destroy() {

    }
}
