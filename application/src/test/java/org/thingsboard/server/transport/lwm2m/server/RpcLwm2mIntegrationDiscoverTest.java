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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.objectInstanceId_0;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceId_2;

public class RpcLwm2mIntegrationDiscoverTest extends RpcAbstractLwM2MIntegrationTest {

    /**
     * DiscoverAll
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverAll_Return_CONTENT_LinksAllObjectsAllInstancesOfClient() throws Exception {
        String setRpcRequest = "{\"method\":\"DiscoverAll\"}";
        String actualResult = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        JsonNode rpcActualValue = JacksonUtil.toJsonNode(rpcActualResult.get("value").asText());
        Set actualObjects = ConcurrentHashMap.newKeySet();
        Set actualInstances = ConcurrentHashMap.newKeySet();
        rpcActualValue.forEach(node -> {
            if (!node.get("url").asText().equals("/")) {
                LwM2mPath path = new LwM2mPath(node.get("url").asText());
                actualObjects.add("/" + path.getObjectId());
                if (path.isObjectInstance()) {
                    actualInstances.add("/" + path.getObjectId() + "/" + path.getObjectInstanceId());
                }
            }
        });
        assertEquals(expectedInstances, actualInstances);
        assertEquals(expectedObjects, actualObjects);
    }

    /**
     * Discover {"id":"/3"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverObject_Return_CONTENT_LinksInstancesAndResourcesOnLyExpectedObject() {
        expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendDiscover((String) expected);
                String expectedObjectId = objectIdVerToObjectId ((String) expected);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String[] actualValues = rpcActualResult.get("value").asText().split(",");
                assertTrue(actualValues.length > 0);
                assertEquals(0, Arrays.stream(actualValues).filter(path -> !path.contains(expectedObjectId)).collect(Collectors.toList()).size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Discover {"id":"3/0"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverInstance_Return_CONTENT_LinksResourcesOnLyExpectedInstance() throws Exception {
        String expected = (String) expectedObjectIdVerInstances.stream().findAny().get();
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedObjectInstanceId = objectInstanceIdVerToObjectInstanceId (expected);
        String[] actualValues = rpcActualResult.get("value").asText().split(",");
        assertTrue(actualValues.length > 0);
        assertEquals(0, Arrays.stream(actualValues).filter(path -> !path.contains(expectedObjectInstanceId)).collect(Collectors.toList()).size());
    }

    /**
     * Discover {"id":"3/0/14"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverResource_Return_CONTENT_LinksResourceOnLyExpectedResource() throws Exception {
        String expectedInstance = (String) expectedInstances.stream().findFirst().get();
        String expectedObjectInstanceId = objectInstanceIdVerToObjectInstanceId (expectedInstance);
        LwM2mPath expectedPath = new LwM2mPath(expectedObjectInstanceId);
        int expectedResource = client.getClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().resources.entrySet().stream().findAny().get().getKey();
        String expected = expectedInstance + "/" + expectedResource;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedResourceId = "<" + expectedObjectInstanceId + "/" + expectedResource + ">";
        String actualValue = rpcActualResult.get("value").asText();
        assertEquals(expectedResourceId, actualValue );

    }

    /**
     * Discover {"id":"2/0/2"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverInstanceAbsentInObject_Return_NOT_FOUND() throws Exception {
        String expected = objectIdVer_2 + "/" + objectInstanceId_0;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }
    /**
     * Discover {"id":"2/0/2"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverResourceAbsentInObject_Return_NOT_FOUND() throws Exception {
         String expected = objectIdVer_2 + "/" + objectInstanceId_0 + "/" + resourceId_2;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    private String sendDiscover(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Discover\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
