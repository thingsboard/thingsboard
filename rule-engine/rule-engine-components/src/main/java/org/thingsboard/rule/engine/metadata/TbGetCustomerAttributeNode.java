// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.NoSuchElementException;

import static com.google.common.util.concurrent.Futures.immediateFuture;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "customer attributes",
        configClazz = TbGetCustomerAttributeNodeConfiguration.class,
        version = 2,
        nodeDescription = "Adds message originator customer attributes or latest telemetry into message or message metadata",
        nodeDetails = "Useful in multi-customer solutions where each customer has a different configuration or threshold set " +
                "that is stored as customer attributes or telemetry data and used for dynamic message filtering, transformation, " +
                "or actions such as alarm creation if the threshold is exceeded.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeCustomerAttributesConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/customer-attributes/"
)
public class TbGetCustomerAttributeNode extends TbAbstractGetEntityDataNode<CustomerId> {

    private boolean preserveOriginatorIfCustomer;

    @Override
    protected TbGetCustomerAttributeNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetCustomerAttributeNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        preserveOriginatorIfCustomer = config.isPreserveOriginatorIfCustomer();
        return config;
    }

    @Override
    protected ListenableFuture<CustomerId> findEntityAsync(TbContext ctx, EntityId originator) {
        if (preserveOriginatorIfCustomer && originator.getEntityType() == EntityType.CUSTOMER) {
            return immediateFuture((CustomerId) originator);
        }
        return ctx.getEntityService().fetchEntityCustomerIdAsync(ctx.getTenantId(), originator)
                .transform(customerIdOpt -> {
                    if (customerIdOpt.isEmpty()) {
                        throw new NoSuchElementException("Originator not found");
                    }
                    if (customerIdOpt.get().isNullUid()) {
                        throw new IllegalStateException("Originator is not assigned to any customer");
                    }
                    return customerIdOpt.get();
                }, ctx.getDbCallbackExecutor());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        ObjectNode config = (ObjectNode) oldConfiguration;
        switch (fromVersion) {
            case 0:
                config = upgradeConfigToUseFetchToAndDataToFetch((ObjectNode) oldConfiguration);
                hasChanges = true;
            case 1:
                if (!config.has("preserveOriginatorIfCustomer")) {
                    config.put("preserveOriginatorIfCustomer", true);
                    hasChanges = true;
                }
                break;
        }
        return new TbPair<>(hasChanges, config);
    }

}
