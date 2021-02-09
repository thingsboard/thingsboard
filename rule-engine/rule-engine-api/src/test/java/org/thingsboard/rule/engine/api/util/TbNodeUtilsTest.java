/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.api.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

@RunWith(MockitoJUnitRunner.class)
public class TbNodeUtilsTest {

    @Test
    public void testSimpleReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("metadata_key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("data_key", "data_value");

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value data_value", result);
    }

    @Test
    public void testNoReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals(pattern, result);
    }

    @Test
    public void testSameKeysReplacement() {
        String pattern = "ABC ${key} $[key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value data_value", result);
    }

    @Test
    public void testComplexObjectReplacement() {
        String pattern = "ABC ${key} $[key1.key2.key3]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode key2Node = JacksonUtil.newObjectNode();
        key2Node.put("key3", "value3");

        ObjectNode key1Node = JacksonUtil.newObjectNode();
        key1Node.set("key2", key2Node);


        ObjectNode node = JacksonUtil.newObjectNode();
        node.set("key1", key1Node);

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value value3", result);
    }

    @Test
    public void testArrayReplacementDoesNotWork() {
        String pattern = "ABC ${key} $[key1.key2[0].key3]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode key2Node = JacksonUtil.newObjectNode();
        key2Node.put("key3", "value3");

        ObjectNode key1Node = JacksonUtil.newObjectNode();
        key1Node.set("key2", key2Node);


        ObjectNode node = JacksonUtil.newObjectNode();
        node.set("key1", key1Node);

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value $[key1.key2[0].key3]", result);
    }

}
