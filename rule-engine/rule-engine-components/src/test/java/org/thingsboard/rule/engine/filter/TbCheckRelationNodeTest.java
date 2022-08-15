package org.thingsboard.rule.engine.filter;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbCheckRelationNodeTest {

    public static final TenantId TENANT_ID = TenantId.SYS_TENANT_ID;
    TbCheckRelationNode node;
    TbCheckRelationNodeConfiguration config;
    @Mock
    RelationService relationService;
    @Mock
    TbContext ctx;

    @Before
    public void setUp() {
        config = new TbCheckRelationNodeConfiguration();
        config = config.defaultConfiguration();
        config.setEntityId(TENANT_ID.toString());
        config.setEntityType(TENANT_ID.getEntityType().name());
        node = new TbCheckRelationNode();

        when(ctx.getTenantId()).thenReturn(TENANT_ID);

        when(ctx.getRelationService()).thenReturn(relationService);
        when(relationService.checkRelationAsync(any(), any(), any(), anyString(), eq(RelationTypeGroup.COMMON))).thenReturn(Futures.immediateFuture(true));
    }

    @Test
    public void testRelationTypeWithoutPattern() throws TbNodeException {
        TbMsg msg = TbMsg.newMsg("CUSTOM", TENANT_ID, new TbMsgMetaData(), "");

        String type = "TYPE";
        config.setRelationType(type);
        callAndVerifyMsg(msg, type);
    }

    @Test
    public void testRelationTypeWithPatternFromDataKey() throws TbNodeException {
        TbMsg msg = TbMsg.newMsg("CUSTOM",
                TENANT_ID,
                new TbMsgMetaData(),
                JacksonUtil.toString(JacksonUtil.newObjectNode().put("data_key", "data_value")));

        config.setRelationType("$[data_key]");
        callAndVerifyMsg(msg, "data_value");
    }

    @Test
    public void testRelationTypeWithPatternFromMetaDataKey() throws TbNodeException {
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData();
        tbMsgMetaData.putValue("metadata_key", "metadata_value");
        TbMsg msg = TbMsg.newMsg("CUSTOM", TENANT_ID, tbMsgMetaData, "");

        config.setRelationType("${metadata_key}");

        callAndVerifyMsg(msg, "metadata_value");
    }

    private void callAndVerifyMsg(TbMsg msg, String type) throws TbNodeException {
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        node.onMsg(ctx, msg);
        verify(relationService, times(1))
                .checkRelationAsync(any(), any(), any(), eq(type), any());
    }
}
