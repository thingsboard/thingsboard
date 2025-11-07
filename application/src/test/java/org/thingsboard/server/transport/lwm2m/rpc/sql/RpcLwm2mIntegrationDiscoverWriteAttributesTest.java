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
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_6;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;


public class RpcLwm2mIntegrationDiscoverWriteAttributesTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     *  <PROPERTIES> Class Attributes
     * - dim              (0-65535)      Integer: Multiple-Instance Resource; R, Number of instances existing for a Multiple-Instance Resource
     *  <NOTIFICATION> Class Attributes
     * - pmin             (def = 0(sec)) Integer: Object; Object Instance; Resource; Resource Instance; RW, Readable Resource
     * - pmax             (def = -- )    Integer: Object; Object Instance; Resource; Resource Instance; RW, Readable Resource
     * - Greater Than  gt (def = -- )    Float:   Resource; Resource Instance; RW, Numerical&Readable Resource
     * - Less Than     lt (def = -- )    Float:   Resource; Resource Instance; RW, Numerical&Readable Resource
     * - Step          st (def = -- )    Float:   Resource; Resource Instance; RW, Numerical&Readable Resource
     */


    /**
     * <PROPERTIES> Class Attributes
     * Object Version   ver   Object
     * Provide the  version of the associated Object.
     * "ver" only for objectId
     * <PROPERTIES> Class Attributes
     * Dimension 	dim Integer [0:255]
     * Number of instances existing for a Multiple-Instance Resource
     * <ObjectID>3</ObjectID>
     * 			<Item ID="6">
     * 				<Name>Available Power Sources</Name>
     *         <Operations>R</Operations>
     *         <MultipleInstances>Multiple</MultipleInstances>
     * 				<Type>Integer</Type>
     * 				<RangeEnumeration>0..7</RangeEnumeration>
     * WriteAttributes  implemented:	Discover {"id":"3/0/6"} ->  'dim' = 3
     */
    @Test
    public void testReadDIM_3_0_6_Only_R() throws Exception {
        String path = objectIdVer_3;
        String actualResult = sendDiscover(path);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3>;ver=1.2";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/6>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/7>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/8>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/11>;dim=1";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * WriteAttributes {"id":"/3/0/14","attributes":{"pmax":100, "pmin":10}}
     * {"result":"CHANGED"}
     * result changed:
     *
     */
    @Test
    public void testWriteAttributesResourceWithParametersById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        String expectedValue = "{\"pmax\":100, \"pmin\":10}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        // result changed
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3/0/6>;pmax=100;pmin=10;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    @Test
    public void testWriteAttributesResourceVerWithParametersById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_3;
        String expectedValue = "{\"ver\":1.3}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Attribute ver is of class PROPERTIES but only NOTIFICATION attribute can be used in WRITE ATTRIBUTE request.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    @Test
    public void testWriteAttributesObjectInstanceResourcePeriodLtGt_Return_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3;
        String expectedValue = "{\"pmax\":65, \"pmin\":5}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_7;
        String expectedValueStr = "gt=50;lt=42.2;st=0.5";
        JsonUtils.parse("{" + expectedValueStr + "}").toString();
        expectedValue = JsonUtils.parse("{" + expectedValueStr + "}").toString();
        actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        // ObjectId
        expectedPath = objectIdVer_3;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValue = rpcActualResult.get("value").asText();
        String expected = "</3>;ver=1.2,</3/0>;pmax=65;pmin=5";
        assertTrue(actualValue.contains(expected));
        expected = "</3/0/6>;dim=3";
        assertTrue(actualValue.contains(expected));
        expected = "</3/0/7>;" + expectedValueStr + ";dim=3";
        assertTrue(actualValue.contains(expected));
        // ObjectInstanceId
        expectedPath = objectInstanceIdVer_3;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        actualValue = rpcActualResult.get("value").asText();
        expected = "</3/0>;pmax=65;pmin=5";
        assertTrue(actualValue.contains(expected));
        expected = "</3/0/7>;" + expectedValueStr + ";dim=3";
        assertTrue(actualValue.contains(expected));
        // ResourceId
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0/6>;dim=3,</3/0/6/0>,</3/0/6/1>,</3/0/6/2>";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_7;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0/7>;" + expectedValueStr;
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    private String sendRPCExecuteWithValueById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"WriteAttributes\", \"params\": {\"id\": \"" + path + "\", \"attributes\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }
}
