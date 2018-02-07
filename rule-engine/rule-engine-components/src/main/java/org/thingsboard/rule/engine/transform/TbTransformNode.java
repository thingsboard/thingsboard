package org.thingsboard.rule.engine.transform;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.List;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
public class TbTransformNode implements TbNode {

    TbGetAttributesNodeConfiguration config;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetAttributesNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            //TODO: refactor this to work async and fetch attributes from cache.
            AttributesService service = ctx.getAttributesService();
            fetchAttributes(msg, service, config.getClientAttributeNames(), DataConstants.CLIENT_SCOPE, "cs.");
            fetchAttributes(msg, service, config.getServerAttributeNames(), DataConstants.SERVER_SCOPE, "ss.");
            fetchAttributes(msg, service, config.getSharedAttributeNames(), DataConstants.SHARED_SCOPE, "shared.");
            ctx.tellNext(msg);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to fetch attributes", msg.getOriginator(), msg.getId(), e);
            throw new TbNodeException(e);
        }
    }

    private void fetchAttributes(TbMsg msg, AttributesService service, List<String> attributeNames, String scope, String prefix) throws InterruptedException, java.util.concurrent.ExecutionException {
        if (attributeNames != null && attributeNames.isEmpty()) {
            List<AttributeKvEntry> attributes = service.find(msg.getOriginator(), scope, attributeNames).get();
            attributes.forEach(attr -> msg.getMetaData().putValue(prefix + attr.getKey(), attr.getValueAsString()));
        }
    }

    @Override
    public void destroy() {

    }
}
