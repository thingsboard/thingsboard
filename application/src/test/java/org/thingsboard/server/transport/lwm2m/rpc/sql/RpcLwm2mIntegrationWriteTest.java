/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbUtils;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.*;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.*;

public class RpcLwm2mIntegrationWriteTest extends AbstractRpcLwM2MIntegrationTest {


    /**
     * update SingleResource:
     * WriteReplace {"id":"3/0/14","value":"+12"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteReplaceValueSingleResourceById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedValue = "+12";
        String actualResult = sendRPCWriteStringById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=" + expectedValue + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * key
     * WriteReplace {"key":"timezone","value":"+10"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteReplaceValueSingleResourceByKey_Result_CHANGED() throws Exception {
        String expectedKey = RESOURCE_ID_NAME_3_14;
        String expectedValue = "+09";
        String actualResult = sendRPCWriteByKey("WriteReplace", expectedKey, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadByKey(expectedKey);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=" + expectedValue + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
    }

    @Test
    public void testWriteReplaceValueMultipleResource_Result_CHANGED_Multi_Instance_Resource_must_One_ValueJsonNodeToBase64() throws Exception {
        LwM2mObjectEnabler lwM2mObjectEnabler = lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnabler(BINARY_APP_DATA_CONTAINER);
        if (lwM2mObjectEnabler != null) {
            String expectedPath = objectIdVer_19 + "/" + FW_INSTANCE_ID + "/";
            String expectedValueStr = getJsonNodeBase64();
            String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"" + expectedValueStr + "\"}}";
            String actualResult = sendRPCreateById(expectedPath, expectedValue);
            ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
            assertEquals(ResponseCode.CREATED.getName(), rpcActualResult.get("result").asText());
            actualResult = sendRPCReadById(expectedPath);
            rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
            String actualValues = rpcActualResult.get("value").asText();
            byte[] expectedValue0 = TbUtils.base64ToBytes(expectedValueStr);
            String expectedInstance = "LwM2mObjectInstance [id=" + FW_INSTANCE_ID;
            assertTrue(actualValues.contains(expectedInstance));
            String expected = "{0=LwM2mMultipleResource [id=0, values={0=LwM2mResourceInstance [id=0, value=" + expectedValue0.length + "Bytes, type=OPAQUE]}";
            assertTrue(actualValues.contains(expected));
        }
    }

    @Test
    public void testWriteReplaceValueMultipleResource_Result_CHANGED_Multi_Instance_Resource_must_One() throws Exception {
        int resourceInstanceId0 = 0;
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId0;
            // base64/String
        String expectedValue = "QUJDREVGRw";
        String actualResult = sendRPCWriteStringById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        byte[] expectedValue0 = TbUtils.base64ToBytes(expectedValue);
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
            // base64/String
        expectedValue = "ABCDEFG";
        actualResult = sendRPCWriteStringById("WriteReplace", expectedPath, expectedValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expectedValue0 = TbUtils.base64ToBytes(expectedValue);
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
            // hexDecimal/String
        expectedValue = "01ABCDEF";
        actualResult = sendRPCWriteStringById("WriteReplace", expectedPath, expectedValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue.length()/2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
            // Integer
        Integer expectedIntegerValue = 1234566;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedIntegerValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedIntegerValue = Integer.MAX_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedIntegerValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedIntegerValue = Integer.MIN_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedIntegerValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
            // Long
        Long expectedLongValue = 4406483977L;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedLongValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 8 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedLongValue = Long.MAX_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedLongValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 8 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedLongValue = Long.MIN_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedLongValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 8 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
            // Float to byte[]: byte[] bytes = ByteBuffer.allocate(4).putFloat(((Float) value).floatValue()).array();
            // Float from byte[]: float f = ByteBuffer.wrap(bytes).getFloat();
        Float expectedFloatValue = 8.02f;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedFloatValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedFloatValue = Float.MAX_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedFloatValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedFloatValue = Float.MIN_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedFloatValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
           // Double to byte[]: byte[] bytes = ByteBuffer.allocate(8).putDouble(((Double) value).doubleValue()).array();
           // Double from byte[]: double d = ByteBuffer.wrap(bytes).getDouble();
        Double expectedDoubleValue = 1022.5906d;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedDoubleValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 4 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedDoubleValue = Double.MAX_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedDoubleValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 8 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        expectedDoubleValue = Double.MIN_VALUE;
        actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedDoubleValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + 8 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * id
     * WriteReplace {"id": "/19_1.1/0/0","value": {"0":"0000ad45675600", "15":"1525ad45675600cdef"}}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteReplaceValueMultipleResource_Result_CHANGED_Value_Multi_Instance_Resource_must_in_Json_format() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;
        int resourceInstanceId0 = 0;
        int resourceInstanceId15 = 15;
        String expectedValue0 = "1525ad45675600cdef";
        Integer expectedValue15 = Integer.MAX_VALUE;
        String expectedValue = "{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\", \"" + resourceInstanceId15 + "\":" + expectedValue15 + "}";
        String actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        String expectedPath0 = expectedPath + "/" + resourceInstanceId0;
        String expectedPath15 = expectedPath + "/" + resourceInstanceId15;
        actualResult = sendRPCReadById(expectedPath0);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath15);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId15 + ", value=" +  Integer.toHexString(expectedValue15).length()/2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * Resource:
     * <Type>Opaque</Type>
     * Input type String (not HexDec)
     * return Hex.decodeHex(((String)value).toCharArray()) - error
     * return Base64.getDecoder().decode(((String) value).getBytes()) - ok;
     * WriteReplace {"id": "/19_1.1/0/0","value": {"0":"01234567890"}}
     * {"result":"CHANGED"} -> Actual Value Not Equals expectedValue0
     */
    @Test
    public void testWriteReplaceValueMultipleResource_Error_InputFormatData_Result_CHANGED_Value_Not_Equals_expectedValue0() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;
        int resourceInstanceId0 = 0;
        String expectedValue0 = "01234567890";
        String expectedValue = "{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\"}";
        String actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        String expectedPath0 = expectedPath + "/" + resourceInstanceId0;
        actualResult = sendRPCReadById(expectedPath0);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length() / 2 + "Bytes, type=OPAQUE]";
        assertFalse(actualValues.contains(expected));
    }

    /**
     * bad: singleResource, operation="R" - only read
     * WriteReplace {"id":"/3/0/9","value":90}
     * {"result":"METHOD_NOT_ALLOWED"}
     */
    @Test
    public void testWriteReplaceValueSingleResourceR_ById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_9;
        Integer expectedValue = 90;
        String actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * ids
     * WriteUpdate  {"id":"/3/0","value":{"14":"+5","15":"Kiyv/Europe"}}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteUpdateValueSingleResourceById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3;
        String expectedValue14 = "+5";
        String expectedValue15 = "Kiyv/Europe";
        String expectedValue = "{\"" + RESOURCE_ID_14 + "\":\"" + expectedValue14 + "\",\"" + RESOURCE_ID_15 + "\":\"" + expectedValue15 + "\"}";
        String actualResult = sendRPCWriteObjectById("WriteUpdate", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        String expectedPath14 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedPath15 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_15;
        actualResult = sendRPCReadById(expectedPath14);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=" + expectedValue14 + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath15);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mSingleResource [id=" + RESOURCE_ID_15 + ", value=" + expectedValue15 + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * id
     * WriteUpdate {"id": "/19_1.1/0","value": {"0":{"0":"00ad456756", "25":"25ad456756"}}}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteUpdateValueMultipleResourceById_Result_CHANGED() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        int resourceInstanceId0 = 0;
        int resourceInstanceId25 = 25;
        String expectedValue0 = "00ad45675600";
        String expectedValue25 = "25ad45675600cdef";
        String expectedValueResourcesObject19 = "{\"" + RESOURCE_ID_0 + "\":{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\", \"" + resourceInstanceId25 + "\":\"" + expectedValue25 + "\"}}";
        String actualResult = sendRPCWriteObjectById("WriteUpdate", expectedPath, expectedValueResourcesObject19);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        String expectedPath0 = expectedPath + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId0;
        String expectedPath25 = expectedPath + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId25;
        actualResult = sendRPCReadById(expectedPath0);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath25);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId25 + ", value=" + expectedValue25.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * ResourceInstance + KeySingleResource + IdSingleResource
     * WriteComposite {"/19_1.1/0":{"0":{"0":"00ad45675600", "25":"25ad45675600cdef"}}, "UtfOffset":"+04", "/3_1.0/0/15":"Kiyv/Europe"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteCompositeValueSingleResourceWithMultiResourceInstanceByIdKey_Result_CHANGED() throws Exception {
        String expectedPath_19_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        int resourceInstanceId0 = 0;
        int resourceInstanceId25 = 25;
        String expectedValue0 = "00ad45675600";
        String expectedValue25 = "25ad45675600cdef";
        String expectedValue_19_Resources = "{\"" + RESOURCE_ID_0 + "\":{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\", \"" + resourceInstanceId25 + "\":\"" + expectedValue25 + "\"}}";

        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedValue3_0_14 = "+04";
        String expectedPath3_0_15 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_15;
        String expectedValue3_0_15 = "Kiyv/Europe";
        String nodes = "{\"" + expectedPath_19_0 + "\":" + expectedValue_19_Resources + ", \"" + expectedKey3_0_14 +
                "\":\"" + expectedValue3_0_14 + "\", \"" + expectedPath3_0_15 + "\":\"" + expectedValue3_0_15 + "\"}";
        String actualResult = sendCompositeRPC(nodes);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath_19_0 + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId0);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath_19_0 + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId25);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId25 + ", value=" + expectedValue25.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadByKey(expectedKey3_0_14);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=" + expectedValue3_0_14 + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath3_0_15);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mSingleResource [id=" + RESOURCE_ID_15 + ", value=" + expectedValue3_0_15 + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * multipleResource == error
     * WriteComposite {"/19_1.1/0/0":{"0":"00ad45675600", "25":"25ad45675600cdef"}}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteCompositeValueSingleMultipleResourceOpaqueValueInputHexStringByIdKey_Result_CHANGED() throws Exception {
        String expectedPath_19_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        int resourceInstanceId0 = 0;
        int resourceInstanceId25 = 25;
        String expectedValue0 = "00ad45675600";
        String expectedValue25 = "25ad45675600cdef";
        String nodes = "{\"" + expectedPath_19_0 + "/" + RESOURCE_ID_0 + "\":{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\", \"" + resourceInstanceId25 + "\":\"" + expectedValue25 + "\"}}";

        String actualResult = sendCompositeRPC(nodes);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath_19_0 + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId0);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath_19_0 + "/" + RESOURCE_ID_0 + "/" + resourceInstanceId25);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mResourceInstance [id=" + resourceInstanceId25 + ", value=" + expectedValue25.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
    }


    /**
     * update_resourceInstances&update_singleResource
     * WriteComposite {"nodes":{"/19_1.1/1/0/2":"00001234", "UtfOffset":"+04", "/3/0/15":"Kiyv/Europe"}}}
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteCompositeCreateResourceInstanceUpdateSingleResourceByIdKey_Result_CHANGED() throws Exception {
        String expectedPath19_1_0_2 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_2;
        String expectedValue19_1_0_2 = "00001234";
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedValue3_0_14 = "+04";
        String expectedPath3_0_15 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_15;
        String expectedValue3_0_15 = "Kiyv/Europe";
        String nodes = "{\"" + expectedPath19_1_0_2 + "\":\"" + expectedValue19_1_0_2 + "\", \"" + expectedKey3_0_14 +
                "\":\"" + expectedValue3_0_14 + "\", \"" + expectedPath3_0_15 + "\":\"" + expectedValue3_0_15 + "\"}";
        String actualResult = sendCompositeRPC(nodes);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        actualResult = sendRPCReadById(expectedPath19_1_0_2);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + RESOURCE_INSTANCE_ID_2 + ", value=" + expectedValue19_1_0_2.length() / 2 + "Bytes, type=OPAQUE]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadByKey(expectedKey3_0_14);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=" + expectedValue3_0_14 + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
        actualResult = sendRPCReadById(expectedPath3_0_15);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        actualValues = rpcActualResult.get("value").asText();
        expected = "LwM2mSingleResource [id=" + RESOURCE_ID_15 + ", value=" + expectedValue3_0_15 + ", type=STRING]";
        assertTrue(actualValues.contains(expected));
    }

    /**
     * composite_not created_new_instance...
     * new ObjectInstance if Object is Multiple - bad
     *  - WriteReplace  {"id":"/19_1.2/2/0","value":{"2":ddff12"}}
     *  - WriteReplace {"key":"UtfOffset","value":"+04"}
     *  - WriteReplace {"id":"/3/0/15","value":"Kiyv/Europe"}
     * WriteComposite {"nodes":{"/19_1.1/1/0/2":"00001234", "UtfOffset":"+04", "/3/0/15":"Kiyv/Europe"}}}
     * {"result":"BAD_REQUEST","error":"object instance /19/2 not found"}
     */
    @Test
    public void testWriteCompositeCreateObjectInstanceUpdateSingleResourceByIdKey_Result_BAD_REQUEST() throws Exception {
        String expectedPath19_1_2_2 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_2 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_2;
        String expectedValue19_1_0_2 = "00001234";
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedValue3_0_14 = "+04";
        String expectedPath3_0_15 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_15;
        String expectedValue3_0_15 = "Kiyv/Europe";
        String nodes = "{\"" + expectedPath19_1_2_2 + "\":\"" + expectedValue19_1_0_2 + "\", \"" + expectedKey3_0_14 +
                "\":\"" + expectedValue3_0_14 + "\", \"" + expectedPath3_0_15 + "\":\"" + expectedValue3_0_15 + "\"}";
        String actualResult = sendCompositeRPC(nodes);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath19_1_2_2);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "object instance " + "/" + expectedPathId.getObjectId() + "/" + expectedPathId.getObjectInstanceId() + " not found";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    private String sendRPCWriteStringById(String method, String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + path + "\", \"value\": \"" + value + "\" }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCWriteObjectById(String method, String path, Object value) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCReadById(String id) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + id + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCWriteByKey(String method, String key, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"key\": \"" + key + "\", \"value\": \"" + value + "\" }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCReadByKey(String key) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"key\": \"" + key + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPC(String nodes) throws Exception {
        String setRpcRequest = "{\"method\": \"WriteComposite\", \"params\": {\"nodes\":" + nodes + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    /**
     * ObjectId = 19/65533/0
     * {
     *   "title" : "My firmware",
     *   "version" : "fw.v.1.5.0-update",
     *   "checksum" : "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a",
     *   "fileSize" : 1,
     *   "fileName" : "filename.txt"
     * }
     */
    private String getJsonNodeBase64() {
        ObjectNode objectNodeInfoOta = JacksonUtil.newObjectNode();
        byte[] firmwareChunk = new byte[]{1};
        String fileChecksumSHA256 = Hashing.sha256().hashBytes(firmwareChunk).toString();
        objectNodeInfoOta.put(OTA_INFO_19_TITLE, "My firmware");
        objectNodeInfoOta.put(OTA_INFO_19_VERSION, "fw.v.1.5.0-update");
        objectNodeInfoOta.put(OTA_INFO_19_FILE_CHECKSUM256, fileChecksumSHA256);
        objectNodeInfoOta.put(OTA_INFO_19_FILE_SIZE, firmwareChunk.length);
        objectNodeInfoOta.put(OTA_INFO_19_FILE_NAME, "filename.txt");
        String objectNodeInfoOtaStr = JacksonUtil.toString(objectNodeInfoOta);
        assert objectNodeInfoOtaStr != null;
        return Base64.getEncoder().encodeToString(objectNodeInfoOtaStr.getBytes());
    }

    private String sendRPCreateById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"Create\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }
}
