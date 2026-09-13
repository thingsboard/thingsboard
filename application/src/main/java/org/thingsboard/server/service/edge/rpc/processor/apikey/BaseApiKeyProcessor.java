// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.apikey;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseApiKeyProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateApiKey(TenantId tenantId, ApiKeyId apiKeyId, ApiKeyUpdateMsg apiKeyUpdateMsg) {
        boolean isCreated = false;
        try {
            ApiKey apiKey = JacksonUtil.fromString(apiKeyUpdateMsg.getEntity(), ApiKey.class, true);
            if (apiKey == null) {
                throw new RuntimeException("[{" + tenantId + "}] apiKeyUpdateMsg {" + apiKeyUpdateMsg + " } cannot be converted to apiKey");
            }

            ApiKey existingApiKey = edgeCtx.getApiKeyService().findApiKeyById(tenantId, apiKeyId);
            if (existingApiKey == null) {
                apiKey.setCreatedTime(Uuids.unixTimestamp(apiKeyId.getId()));
                isCreated = true;
            }

            apiKey.setId(apiKeyId);
            edgeCtx.getApiKeyService().saveApiKey(tenantId, apiKey, apiKey.getValue(), false);
        } catch (Exception e) {
            log.error("[{}] Failed to process apiKey update msg [{}]", tenantId, apiKeyUpdateMsg, e);
            throw e;
        }
        return isCreated;
    }

    protected void deleteApiKey(TenantId tenantId, Edge edge, ApiKeyId apiKeyId) {
        ApiKey apiKey = edgeCtx.getApiKeyService().findApiKeyById(tenantId, apiKeyId);
        if (apiKey != null) {
            edgeCtx.getApiKeyService().deleteApiKey(tenantId, apiKey, false);
            pushEntityEventToRuleEngine(tenantId, edge, apiKey, TbMsgType.ENTITY_DELETED);
        }
    }

}
