package org.thingsboard.rule.engine.filter;

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
public class TbMsgTypeFilterNode implements TbNode {

    TbMsgTypeFilterNodeConfiguration config;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgTypeFilterNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ctx.tellNext(msg, Boolean.toString(config.getMessageTypes().contains(msg.getType())));
    }

    @Override
    public void destroy() {

    }
}
