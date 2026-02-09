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
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_4;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_8;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;


public class RpcLwm2mIntegrationExecuteTest extends AbstractRpcLwM2MIntegrationTest {


    /**
     * Update FW
     * Execute {"id":"5/0/2"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteUpdateFWById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Update SW
     * Execute {"id":"9/0/4"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteUpdateSWById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_9 + "/" + RESOURCE_ID_4;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Reboot
     * Execute {"id":"3/0/4"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteRebootById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_4;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Registration Update Trigger
     * Execute {"id":"1/0/8"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteRegistrationUpdateTriggerById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_1 + "/" + RESOURCE_ID_8;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }


    /**
     * execute_resource_with_parameters (execute reboot after 60 seconds on device)
     * Execute {"id":"3/0/4","value":60}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteResourceWithParametersById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_4;
        Object expectedValue = 60;
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Bootstrap-Request Trigger
     * Execute {"id":"1/0/9"}
     * {"result":"BAD_REQUEST","error":"probably no bootstrap server configured"}
     */
    @Test
    public void testExecuteBootstrapRequestTriggerById_Result_BAD_REQUEST_Error_NoBootstrapServerConfigured() throws Exception {
        String expectedPath = objectInstanceIdVer_1 + "/" + RESOURCE_ID_9;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "probably no bootstrap server configured";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * bad: resource operation not "E"
     * Execute {"id":"5_1.0/0/3"}
     * {"result":"BAD_REQUEST","error":"Resource with /5_1.0/0/3 is not executable."}
     */
    @Test
    public void testExecuteResourceWithOperationNotExecuteById_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedPath = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Resource with " + expectedPath + " is not executable.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * bad: execute_non_existing_resource_on_non_existing_object
     * Execute {"id":"50/0/3"}
     * {"result":"BAD_REQUEST","error":"Specified object id 50 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testExecuteNonExistingResourceOnNonExistingObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = OBJECT_ID_VER_50 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * bad: execute security object
     * Execute {"id":"0/0/3"}
     * {"result":"BAD_REQUEST","error":"Specified object id 0 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testExecuteSecurityObjectById_Result_NOT_FOUND() throws Exception {
        String expectedPath = objectIdVer_0 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }


    private String sendRPCExecuteById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCExecuteWithValueById(String path, Object value) throws Exception {
        String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

}
