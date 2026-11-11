// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@TbCoreComponent
public class IntegrationEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        IntegrationId integrationId = new IntegrationId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                Integration integration = edgeCtx.getIntegrationService().findIntegrationById(edgeEvent.getTenantId(), integrationId);
                if (integration != null) {
                    JsonNode updatedConfiguration = replaceAttributePlaceholders(edgeEvent, integration.getConfiguration());
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addIntegrationMsg(EdgeMsgConstructorUtils.constructIntegrationUpdateMsg(msgType, integration, updatedConfiguration));

                    Converter uplinkConverter = edgeCtx.getConverterService().findConverterById(edgeEvent.getTenantId(), integration.getDefaultConverterId());
                    builder.addConverterMsg(EdgeMsgConstructorUtils.constructConverterUpdateMsg(msgType, uplinkConverter));

                    if (integration.getDownlinkConverterId() != null) {
                        Converter converter = edgeCtx.getConverterService().findConverterById(edgeEvent.getTenantId(), integration.getDownlinkConverterId());
                        builder.addConverterMsg(EdgeMsgConstructorUtils.constructConverterUpdateMsg(msgType, converter));
                    }

                    downlinkMsg = builder.build();
                }
            }
            case ENTITY_DELETED_RPC_MESSAGE -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addIntegrationMsg(EdgeMsgConstructorUtils.constructIntegrationDeleteMsg(integrationId))
                    .build();
        }
        return downlinkMsg;
    }

    private JsonNode replaceAttributePlaceholders(EdgeEvent edgeEvent, JsonNode originalConfiguration) {
        try {
            Set<String> attributeKeysFromConfiguration =
                    EdgeUtils.getAttributeKeysFromConfiguration(originalConfiguration.toString());
            if (attributeKeysFromConfiguration.isEmpty()) {
                return originalConfiguration;
            }
            List<AttributeKvEntry> attributeKvEntries =
                    edgeCtx.getAttributesService().find(edgeEvent.getTenantId(),
                            edgeEvent.getEdgeId(),
                            AttributeScope.SERVER_SCOPE,
                            attributeKeysFromConfiguration).get();
            String updatedConfiguration = originalConfiguration.toString();
            for (AttributeKvEntry attributeKvEntry : attributeKvEntries) {
                updatedConfiguration =
                        updatedConfiguration.replaceAll(EdgeUtils.formatAttributeKeyToRegexpPlaceholderFormat(attributeKvEntry.getKey()), attributeKvEntry.getValueAsString());
            }
            return JacksonUtil.toJsonNode(updatedConfiguration);
        } catch (Exception e) {
            log.warn("Failed to replace attribute placeholders in configuration [{}]", originalConfiguration, e);
            return originalConfiguration;
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.INTEGRATION;
    }

}
