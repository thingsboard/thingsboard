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
package org.thingsboard.server.transport.lwm2m.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RpcLwm2mIntegrationObserveTest extends RpcAbstractLwM2MIntegrationTest {

    /**
     * ObserveReadAll
     * @throws Exception
     */
    @Test
    public void testObserveReadAllNothingObservation_Result_CONTENT_Value_Count_0() throws Exception {
        String actualResult = sendObserve("ObserveCancelAll", null);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        actualResult = sendObserve("ObserveReadAll", null);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("[]", rpcActualResult.get("value").asText());
    }

    /**
     * Observe {"id":"/3/0/9"}
     * @throws Exception
     */
    @Test
    public void testObserveSingleResource_Result_CONTENT_Value_SingleResource() throws Exception {
        String expected = "/3/0/9";
        String actualResult = sendObserve("Observe", expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
    }

    /**
     * Observe {"id":"/3_1.1/0/13"}
     * @throws Exception
     */
    @Test
    public void testObserveWithBadVersion_Result_BadRequest_ErrorMsg_BadVersionMustBe1_0() throws Exception {
        String expectedInstance = (String) expectedInstances.stream().filter(path -> !((String)path).contains("_")).findFirst().get();
        LwM2mPath expectedPath = new LwM2mPath(expectedInstance);
        int expectedResource = client.getClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().resources.entrySet().stream().findAny().get().getKey();
        String expectedId = "/" + expectedPath.getObjectId() + "_1.2" + "/" + expectedPath.getObjectInstanceId() + "/" + expectedResource;
        String actualResult = sendObserve("Observe", expectedId);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Specified resource id " + expectedId +" is not valid version! Must be version: 1.0";
        assertEquals(expected, rpcActualResult.get("error").asText());
    }

    /**
     * Not implemented Instance
     * Observe {"id":"/2/0"}
     * @throws Exception
     */
    @Test
    public void testObserveNoImplementedInstanceOnDevice_Result_NotFound() throws Exception {
        String objectInstanceIdVer = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).contains("/2")).findFirst().get();
        String expected = objectInstanceIdVer + "/0";
        String actualResult = sendObserve("Observe", expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Not implemented Resource
     * Observe {"id":"/19_1.1/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveNoImplementedResourceOnDeviceValueNull_Result_BadRequest() throws Exception {
        String objectIdVer = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).contains("/19")).findFirst().get();
        String expected = objectIdVer + "/0/0";
        String actualResult = sendObserve("Observe", expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String expectedValue = "values MUST NOT be null";
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertEquals(expectedValue, rpcActualResult.get("error").asText());
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/5/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveRSourceNotRead_Result_METHOD_NOT_ALLOWED() throws Exception {
        String objectInstanceIdVer = (String) expectedObjectIdVerInstances.stream().filter(path -> !((String)path).contains("/2") && !((String)path).contains("/19")).findFirst().get();
        String expectedId = "/5/0/0";
        sendObserve("Observe", expectedId);
        String actualResult = sendObserve("Observe", expectedId);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/3/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveRepeatedRequestObserveOnDevice_Result_BAD_REQUEST_ErrorMsg_AlreadyRegistered() throws Exception {
        String expectedId = "/3/0/0";
        sendObserve("Observe", expectedId);
        String actualResult = sendObserve("Observe", expectedId);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Observation is already registered!";
        assertEquals(expected, rpcActualResult.get("error").asText());
    }

    /**
     * ObserveReadAll
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_Result_CONTENT_Value_Contains_Paths_Count_ObserveAll() throws Exception {
        sendObserve("ObserveCancelAll", null);
        String expectedId1 = "/3/0/0";
        String expectedId2 = "/3/0/9";
        sendObserve("Observe", expectedId1);
        sendObserve("Observe", expectedId2);
        String actualResult = sendObserve("ObserveReadAll", null);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expectedId1));
        assertTrue(actualValues.contains(expectedId2));
        assertEquals(2, actualValues.split(",").length);
    }


    /**
     * ObserveCancel {"id":"/2001_1.1/0/3"}
     * @throws Exception
     */
    @Test
    public void testObserveCancelOneResource_Result_CONTENT_Value_Count_1() throws Exception {
        sendObserve("ObserveCancelAll", null);
        String expectedId1 = "/3/0/0";
        String expectedId2 = "/5/0/3";
        sendObserve("Observe", expectedId1);
        sendObserve("Observe", expectedId2);
        String actualResult = sendObserve("ObserveCancel", expectedId1);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("1", rpcActualResult.get("value").asText());
    }

    private String sendObserve(String method, String params) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        }
        else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, sendRpcRequest, String.class, status().isOk());
    }
}
