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
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;

public class RpcLwm2mIntegrationWriteCborTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Client with ContentFormat ={}
     */

    /**
     * Resource:
     * <Type>Opaque</Type>
     * Input type String (HexDec)
     * WriteReplace {"id": "/19_1.1/0/0","value": {"0":"01234567890CAB"}}
     * return Hex.decodeHex(((String)value).toCharArray()) - ok [1, 35, 69, 103, -119, 12, -85, -1]
     * {"result":"CHANGED"} -> Actual Value Equals expectedValue0
     */
    @Test
    public void testWriteReplaceValueMultipleResource_InputFormatData_Ok_Result_CHANGED_Value_Equals_expectedValue0() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;
        int resourceInstanceId0 = 0;
        String expectedValue0 = "01234567890CABFF";
        String expectedValue = "{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\"}";
        String actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        String expectedPath0 = expectedPath + "/" + resourceInstanceId0;
        actualResult = sendRPCReadById(expectedPath0);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String actualValues = rpcActualResult.get("value").asText();
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length()/2 + "Bytes, type=OPAQUE]";
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
        String expected = "LwM2mResourceInstance [id=" + resourceInstanceId0 + ", value=" + expectedValue0.length()/2 + "Bytes, type=OPAQUE]";
        assertFalse(actualValues.contains(expected));
    }


    private String sendRPCWriteObjectById(String method, String path, Object value) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCReadById(String id) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + id + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }
}
