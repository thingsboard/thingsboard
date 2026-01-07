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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;


public class RpcLwm2mIntegrationCreateTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19_1.1/12","value":{"0":{"0":"00AC", "1":1}}}
     * {"{"result":"CREATED"}"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdByIdKey_Result_CREATED() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_12;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CREATED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19_1.1/0","value":{"0":{"0":"00AC", "1":1}}}
     * {"result":"BAD_REQUEST","error":"instance 0 already exists"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdAlreadyExistsById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "instance " + OBJECT_INSTANCE_ID_0 + " already exists";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * failed: cannot_create_mandatory_single_object
     * Create  {"id":"/3/1,"value":{"0":"00AC"}}
     * {"result":"BAD_REQUEST","error":"Path /3/1. Object must be Multiple !"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdMandatorySingleObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_1;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Path " + expectedPath + ". Object must be Multiple !";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * failed:  cannot_create_instance_of_security_object
     * Create  {"id":"/0/2","value":{"2":4}}
     * {"result":"BAD_REQUEST","error":"Specified object id 0 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdSecurityObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_0 + "/" + OBJECT_INSTANCE_ID_1;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"2\":4}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId(expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * failed: cannot_create_instance_of_absent_object
     * Create  {"id":"/50/1","value":{"0":"00AC"}}
     * {"result":"BAD_REQUEST","error":"Specified object id 50 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdAbsentObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = OBJECT_ID_VER_50 + "/" + OBJECT_INSTANCE_ID_1;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId(expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    private String sendRPCreateById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"Create\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

}
