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

public class RpcLwm2mIntegrationReadWriteTest extends RpcAbstractLwM2MIntegrationTest {

    /**
     * Read {"id":"/3"}
     * Read {"id":"/6"}...
     */
    @Test
    public void testReadAllObjectsInClient_Result_CONTENT_Value_IsLwM2mObject_IsInstances() throws Exception {
                expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendRPC((String) expected);
                String expectedObjectId = objectIdVerToObjectId ((String) expected);
                LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String expectedObjectInstances = "LwM2mObject [id=" + expectedPath.getObjectId() + ", instances={0=LwM2mObjectInstance [id=0, resources=";
                if (expectedPath.getObjectId() == 2) {
                    expectedObjectInstances = "LwM2mObject [id=2, instances={}]";
                }
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Read {"id":"/5/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadAllInstancesInClient_Result_CONTENT_Value_IsInstances_IsResources() throws Exception{
        expectedObjectIdVerInstances.forEach(expected -> {
            try {
                String actualResult  = sendRPC((String) expected);
                String expectedObjectId = objectInstanceIdVerToObjectInstanceId ((String) expected);
                LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String expectedObjectInstances = "LwM2mObjectInstance [id=" + expectedPath.getObjectInstanceId() + ", resources={";
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Read {"id":"/19/1/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadMultipleResource_Result_CONTENT_Value_IsLwM2mMultipleResource() throws Exception {
        String objecIdVer = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).equals("/3") || ((String)path).contains("/3_")).findFirst().get();
         int resourceId = 11;
        String expectedIdVer = objecIdVer + "/0/" + resourceId ;
        String actualResult = sendRPC(expectedIdVer);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mMultipleResource [id=" + resourceId + ", values={";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * Read {"id":"/3/0/9"}
     *
     * @throws Exception
     */
    @Test
    public void testReadSingleResource_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
        String objecIdVer = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).equals("/3") || ((String)path).contains("/3_")).findFirst().get();
        int resourceId = 9;
        String expectedIdVer = objecIdVer + "/0/" + resourceId ;
        String actualResult = sendRPC(expectedIdVer);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mSingleResource [id=9, value=";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * ReadComposite {"ids":["/3_1.0/0/9", "/1_1.2/0/2"]}
     *
     * @throws Exception
     */
    @Test
    public void testReadCompositeSingleResource_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
        String objectIdVer1 = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).equals("/1") || ((String)path).contains("/1_")).findFirst().get();
        String objectIdVer3 = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).equals("/3") || ((String)path).contains("/3_")).findFirst().get();
        int resourceId1_1 = 1;
        int resourceId1_2 = 2;
        int resourceId3_1 = 1;
        int resourceId3_11 = 11;
        String expectedIdVer1_1 = objectIdVer1 + "/0/" + resourceId1_1;
        String expectedIdVer1_2 = objectIdVer1 + "/0/" + resourceId1_2;
        String expectedIdVer3_1 = objectIdVer3 + "/0/" + resourceId3_1;
        String expectedIdVer3_11 = objectIdVer3 + "/0/" + resourceId3_11;
        String expectedIds = "[\"" + expectedIdVer1_1 + "\", \"" + expectedIdVer3_1 + "\", \"" + expectedIdVer3_11 + "\", \"" + expectedIdVer1_2 + "\"]";
        String actualResult = sendCompositeRPC(expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected1_1 = "/1/0/1=LwM2mSingleResource [id=1, value=";
        String expected1_2 = "/1/0/2=null";
        String expected3_1 = "/3/0/1=LwM2mSingleResource [id=1, value=";
        String expected3_11 = "/3/0/11=LwM2mMultipleResource [id=11, values={";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected1_1));
        assertTrue(actualValues.contains(expected1_2));
        assertTrue(actualValues.contains(expected3_1));
        assertTrue(actualValues.contains(expected3_11));
    }


    private String sendRPC(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPC(String paths) throws Exception {
        String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"ids\":" + paths + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
