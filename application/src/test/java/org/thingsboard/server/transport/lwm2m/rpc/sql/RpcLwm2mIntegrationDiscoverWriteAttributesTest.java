/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_6;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;


public class RpcLwm2mIntegrationDiscoverWriteAttributesTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * WriteAttributes {"id":"/3_1.2/0/6","attributes":{"pmax":100, "pmin":10}}
     * if not implemented:
     * {"result":"INTERNAL_SERVER_ERROR","error":"not implemented"}
     * if implemented:
     * {"result":"BAD_REQUEST","error":"Attribute pmax can be used for only Resource/Object Instance/Object."}
     */
    @Test
    public void testWriteAttributesResourceWithParametersByResourceInstanceId_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6 + "/1";
        String expectedValue = "{\"pmax\":100, \"pmin\":10}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Attribute pmax can be used for only Resource/Object Instance/Object.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }
    /**
     * WriteAttributes {"id":"/3_1.2/0/6","attributes":{"pmax":100, "pmin":10}}
     * if not implemented:
     * {"result":"INTERNAL_SERVER_ERROR","error":"not implemented"}
     * if implemented:
     * {"result":"BAD_REQUEST","error":"Attribute pmax can be used for only Resource/Object Instance/Object."}
     */
    @Test
    public void testWriteAttributeResourceDimWithParametersByResourceId_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        String expectedValue = "{\"dim\":3}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Attribute dim is of class PROPERTIES but only NOTIFICATION attribute can be used in WRITE ATTRIBUTE request.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
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
    public void testWriteAttributesResourceServerUriWithParametersById_Result_BAD_REQUEST() throws Exception {
        String expectedPath =  objectInstanceIdVer_1;
        String actualResult = sendRPCReadById(expectedPath);
        String expectedValue = "{\"uri\":\"coaps://localhost:5690\"}";
        actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Attribute uri is of class PROPERTIES but only NOTIFICATION attribute can be used in WRITE ATTRIBUTE request.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
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
     * "ver" only for objectId
     */
    @Test
    public void testReadDIM_3_0_6_Only_R () throws Exception {
        String path = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        String actualResult = sendDiscover(path);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3/0/6>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().equals(expected));

    }


    /**
     * <PROPERTIES> Class Attributes
     * Object Version   ver   Object
     * Provide the  version of the associated Object.
     * "ver" only for objectId
     */
    @Test
    public void testReadVer () throws Exception {
        String path = objectIdVer_3;
        String actualResult = sendDiscover(path);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3>;ver=1.2";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * WriteAttributes {"id":"/3/0/14","attributes":{"pmax":100, "pmin":10}}
     * if not implemented:
     * {"result":"INTERNAL_SERVER_ERROR","error":"not implemented"}
     * if implemented:
     * {"result":"CHANGED"}
     */
    @Test
    public void testWriteAttributesResourceWithParametersById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedValue = "{\"pmax\":100, \"pmin\":10}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
            // result changed
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3/0/14>;pmax=100;pmin=10";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * <NOTIFICATION> Class Attributes
     * Minimum/Maximum Period pmin/pmax
     * Notes: The Minimum Period Attribute:
     * -- indicates the minimum time in seconds the LwM2M Client MUST wait between two notifications. If a notification of an observed Resource is supposed to be generated but it is before pmin expiry, notification MUST be sent as soon as pmin expires. In the absence of this parameter, the Minimum Period is defined by the Default Minimum Period set in the LwM2M Server Account.
     * Notes: The Maximum Period Attribute:
     * -- indicates the maximum time in seconds the LwM2M Client MAY wait between two notifications. When this "Maximum Period" expires after the last notification, a new notification MUST be sent. In the absence of this parameter, the "Maximum Period" is defined by the Default Maximum Period when set in the LwM2M Server Account or considered as 0 otherwise. The value of 0, means pmax MUST be ignored. The maximum period parameter MUST be greater than the minimum period parameter otherwise pmax will be ignored for the Resource to which such inconsistent timing conditions are applied.
     * Greater Than  gt Resource
     * Less Than     lt Resource
     * Step          st Resource
     *
     * Object Id = 1
     * Default Minimum Period 	Id = 2 		300 or 0
     * Default Maximum Period 	Id = 3 		6000 or "-"
     * </3/0>;pmax=65, </3/0/1>, <3/0/2>, </3/0/3>, </3/0/4>,
     * <3/0/6>;dim=8,<3/0/7>;gt=50;lt=42.2;st=0.5,<3/0/8>;...
     */
    @Test
    public void testWriteAttributesPeriodLtGt () throws Exception {
        String expectedPath = objectInstanceIdVer_3;
        String expectedValue = "{\"pmax\":60}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        expectedPath = objectInstanceIdVer_3;
        expectedValue = "{\"pmax\":65}";
        actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_7;
        expectedValue ="{\"gt\":50, \"lt\":42.2, \"st\":0.5}";
        actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
            // ObjectId
        expectedPath = objectIdVer_3;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
            // String expected = "</3>;ver=1.2,</3/0>;pmax=60,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/3>,</3/0/6>;dim=3,</3/0/7>;st=0.5;lt=42.2;gt=50.0,</3/0/8>,</3/0/9>,</3/0/10>,</3/0/11>;dim=1,</3/0/13>,</3/0/14>,</3/0/15>,</3/0/16>,</3/0/17>,</3/0/18>,</3/0/19>,</3/0/20>,</3/0/21>";
        String expected = "</3>;ver=1.2,</3/0>;pmax=65";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/6>;dim=3,</3/0/7>;st=0.5;lt=42.2;gt=50.0";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
            // ObjectInstanceId
        expectedPath = objectInstanceIdVer_3;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0>;pmax=65";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/6>;dim=3,</3/0/7>;st=0.5;lt=42.2;gt=50.0";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
            // ResourceId
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0/6>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_7;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0/7>;st=0.5;lt=42.2;gt=50.0";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
            // ResourceInstanceId
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6+ "/1";
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.getName(), rpcActualResult.get("result").asText());
        expected = "InvalidRequestException: Discover request cannot target resource instance path: /3/0/6/1";
        assertTrue(rpcActualResult.get("error").asText().contains(expected));
    }

    private String sendRPCExecuteWithValueById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"WriteAttributes\", \"params\": {\"id\": \"" + path + "\", \"attributes\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCReadById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendDiscover(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Discover\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
