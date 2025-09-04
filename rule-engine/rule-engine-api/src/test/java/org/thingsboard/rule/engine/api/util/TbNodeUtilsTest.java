/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
public class TbNodeUtilsTest {

    private static final String DATA_VARIABLE_TEMPLATE = "$[%s]";
    private static final String METADATA_VARIABLE_TEMPLATE = "${%s}";

    @Test
    public void testSimpleReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("metadata_key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("data_key", "data_value");

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(md)
                .data(JacksonUtil.toString(node))
                .build();
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assertions.assertEquals("ABC metadata_value data_value", result);
    }

    @Test
    public void testNoReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(md)
                .data(JacksonUtil.toString(node))
                .build();
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assertions.assertEquals(pattern, result);
    }

    @Test
    public void testSameKeysReplacement() {
        String pattern = "ABC ${key} $[key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(md)
                .data(JacksonUtil.toString(node))
                .build();
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assertions.assertEquals("ABC metadata_value data_value", result);
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

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(md)
                .data(JacksonUtil.toString(node))
                .build();
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assertions.assertEquals("ABC metadata_value value3", result);
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

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(md)
                .data(JacksonUtil.toString(node))
                .build();
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assertions.assertEquals("ABC metadata_value $[key1.key2[0].key3]", result);
    }

    @Test
    public void givenKey_whenFormatDataVarTemplate_thenReturnTheSameStringAsFormat() {
        assertThat(TbNodeUtils.formatDataVarTemplate("key"), is("$[key]"));
        assertThat(TbNodeUtils.formatDataVarTemplate("key"), is(String.format(DATA_VARIABLE_TEMPLATE, "key")));

        assertThat(TbNodeUtils.formatDataVarTemplate(""), is("$[]"));
        assertThat(TbNodeUtils.formatDataVarTemplate(""), is(String.format(DATA_VARIABLE_TEMPLATE, "")));

        assertThat(TbNodeUtils.formatDataVarTemplate(null), is("$[null]"));
        assertThat(TbNodeUtils.formatDataVarTemplate(null), is(String.format(DATA_VARIABLE_TEMPLATE, (String) null)));
    }

    @Test
    public void givenKey_whenFormatMetadataVarTemplate_thenReturnTheSameStringAsFormat() {
        assertThat(TbNodeUtils.formatMetadataVarTemplate("key"), is("${key}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate("key"), is(String.format(METADATA_VARIABLE_TEMPLATE, "key")));

        assertThat(TbNodeUtils.formatMetadataVarTemplate(""), is("${}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate(""), is(String.format(METADATA_VARIABLE_TEMPLATE, "")));

        assertThat(TbNodeUtils.formatMetadataVarTemplate(null), is("${null}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate(null), is(String.format(METADATA_VARIABLE_TEMPLATE, (String) null)));
    }

    @Test
    public void testAllMetadataTemplateReplacement() {
        // GIVEN
        String pattern = "META ${*}";
        var metadata = new TbMsgMetaData();
        metadata.putValue("meta_key", "meta_value");

        var msg = TbMsg.newMsg()
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(metadata)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "META {\"meta_key\":\"meta_value\"}";
        assertThat(actual, is(expected));
    }

    @Test
    public void testMultipleAllMetadataTemplatesReplacement() {
        // GIVEN
        String pattern = "${*} then again ${*}";
        var metadata = new TbMsgMetaData();
        metadata.putValue("meta_key", "meta_value");

        var msg = TbMsg.newMsg()
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(metadata)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "{\"meta_key\":\"meta_value\"} then again {\"meta_key\":\"meta_value\"}";
        assertThat(actual, is(expected));
    }

    @Test
    public void testAllDataTemplateReplacement() {
        // GIVEN
        String pattern = "DATA $[*]";
        var dataJson = JacksonUtil.newObjectNode().put("data_key", "data_value");

        var msg = TbMsg.newMsg()
                .data(JacksonUtil.toString(dataJson))
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "DATA {\"data_key\":\"data_value\"}";
        assertThat(actual, is(expected));
    }

    @Test
    public void testMultipleAllDataTemplatesReplacement() {
        // GIVEN
        String pattern = "$[*] then again $[*]";
        var dataJson = JacksonUtil.newObjectNode().put("data_key", "data_value");

        var msg = TbMsg.newMsg()
                .data(JacksonUtil.toString(dataJson))
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "{\"data_key\":\"data_value\"} then again {\"data_key\":\"data_value\"}";
        assertThat(actual, is(expected));
    }

    @Test
    public void testAllDataAndAllMetadataTemplatesSimultaneously() {
        // GIVEN
        String pattern = "META ${*} DATA $[*]";

        var metadata = new TbMsgMetaData(Map.of("meta_key", "meta_value"));
        var dataJson = JacksonUtil.newObjectNode().put("data_key", "data_value");

        var msg = TbMsg.newMsg()
                .data(JacksonUtil.toString(dataJson))
                .metaData(metadata)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "META {\"meta_key\":\"meta_value\"} DATA {\"data_key\":\"data_value\"}";
        assertThat(actual, is(expected));
    }

    @Test
    public void testAllDataAndAllMetadataTemplatesSimultaneouslyEmpty() {
        // GIVEN
        String pattern = "META ${*} DATA $[*]";

        var msg = TbMsg.newMsg()
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "META {} DATA {}";
        assertThat(actual, is(expected));
    }

    @Test
    public void testAllDataTemplateArray() {
        // GIVEN
        String pattern = "DATA $[*]";

        var msg = TbMsg.newMsg()
                .data("[1, \"two\", true]")
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "DATA [1,\"two\",true]";
        assertThat(actual, is(expected));
    }

    @Test
    public void testMixedAllDataMetadataAndNormalTemplates() {
        // GIVEN
        String pattern = "fullMeta=${*}, singleMeta=${meta_key}, fullData=$[*], singleData=$[data_key]";
        var metadata = new TbMsgMetaData(Map.of("meta_key", "meta_value"));
        var dataJson = JacksonUtil.newObjectNode().put("data_key", "data_value");

        var msg = TbMsg.newMsg()
                .data(JacksonUtil.toString(dataJson))
                .metaData(metadata)
                .build();

        // WHEN
        String actual = TbNodeUtils.processPattern(pattern, msg);

        // THEN
        String expected = "fullMeta={\"meta_key\":\"meta_value\"}, singleMeta=meta_value, fullData={\"data_key\":\"data_value\"}, singleData=data_value";
        assertThat(actual, is(expected));
    }

}
