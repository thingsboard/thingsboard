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
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.objectInstanceId_1;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceId_1;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceId_11;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceId_14;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceId_2;

public class RpcLwm2mIntegrationReadTest extends RpcAbstractLwM2MIntegrationTest {

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
       String expectedIdVer = objectInstanceIdVer_3 +"/" + resourceId_11 ;
        String actualResult = sendRPC(expectedIdVer);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mMultipleResource [id=" + resourceId_11 + ", values={";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * Read {"id":"/3/0/14"}
     */
    @Test
    public void testReadSingleResource_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
         String expectedIdVer = objectInstanceIdVer_3 +"/" + resourceId_14 ;
        String actualResult = sendRPC(expectedIdVer);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mSingleResource [id=" + resourceId_14 + ", value=";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * ReadComposite {"ids":["/1_1.2", "/3_1.0/0/1", "/3_1.0/0/11"]}
     */
    @Test
    public void testReadCompositeSingleResource_Result_CONTENT_Value_IsObjectIsLwM2mSingleResourceIsLwM2mMultipleResource() throws Exception {
        String expectedIdVer_1 = (String) expectedObjectIdVers.stream().filter(path -> (!((String)path).contains("/" + BINARY_APP_DATA_CONTAINER) && ((String)path).contains("/" + SERVER))).findFirst().get();
        String objectId_1 = objectIdVerToObjectId(expectedIdVer_1);
        String expectedIdVer3_0_1 = objectInstanceIdVer_3 + "/" +  resourceId_1;
        String expectedIdVer3_0_11 = objectInstanceIdVer_3 + "/" +  resourceId_11;
        String objectInstanceId_3 = objectIdVerToObjectId(objectInstanceIdVer_3);
        String expectedIds = "[\"" + expectedIdVer_1 + "\", \"" + expectedIdVer3_0_1 + "\", \"" + expectedIdVer3_0_11 + "\"]";
        String actualResult = sendCompositeRPC(expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected1 = objectId_1 + "=LwM2mObject [id=" + new LwM2mPath(objectId_1).getObjectId() + ", instances={";
        String expected3_0_1 = objectInstanceId_3 + "/" +  resourceId_1 + "=LwM2mSingleResource [id=" + resourceId_1 + ", value=";
        String expected3_0_11 = objectInstanceId_3 + "/" +  resourceId_11 + "=LwM2mMultipleResource [id=" + resourceId_11 + ", values={";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected1));
        assertTrue(actualValues.contains(expected3_0_1));
        assertTrue(actualValues.contains(expected3_0_11));
    }

    /**
     * ReadComposite {"ids":["/1_1.2/0/1", "/1_1.2/0/2", "/3_1.0/0"]}
     */
    @Test
    public void testReadCompositeSingleResource_Result_CONTENT_Value_IsObjectInstanceIsLwM2mSingleResource() throws Exception {
        String expectedIdVer3_0 = objectInstanceIdVer_3;
        String expectedIdVer1_0_1 = objectInstanceIdVer_1 + "/" + resourceId_1;
        String expectedIdVer1_0_2 = objectInstanceIdVer_1 + "/" + resourceId_2;
        String expectedIds = "[\"" + expectedIdVer1_0_1 + "\", \"" + expectedIdVer1_0_2 + "\", \"" + expectedIdVer3_0 + "\"]";
        String actualResult = sendCompositeRPC(expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String objectInstanceId_3 = objectIdVerToObjectId(objectInstanceIdVer_3);
        LwM2mPath path = new LwM2mPath(objectInstanceId_3);
        String expected3_0 = objectInstanceId_3 + "=LwM2mObjectInstance [id=" + path.getObjectInstanceId() + ", resources={";
        String objectInstanceId_1 = objectInstanceIdVerToObjectInstanceId(objectInstanceIdVer_1);
        String expected1_0_1 = objectInstanceId_1 + "/" +  resourceId_1 + "=LwM2mSingleResource [id=" + resourceId_1 + ", value=";
        String expected1_0_2 = objectInstanceId_1 + "/" +  resourceId_2 + "=null";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected3_0));
        assertTrue(actualValues.contains(expected1_0_1));
        assertTrue(actualValues.contains(expected1_0_2));

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
