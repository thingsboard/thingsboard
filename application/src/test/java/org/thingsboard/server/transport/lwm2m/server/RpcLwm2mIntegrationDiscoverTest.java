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
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.TEST_OBJECT_MULTI_WITH_RESOURCE_RW_ID;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.TEST_OBJECT_SINGLE_WITH_RESOURCE_R_ID;

public class RpcLwm2mIntegrationDiscoverTest extends RpcAbstractLwM2MIntegrationTest {

    private final  String method = "Discover";

    /**
     * DiscoverAll
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverAll() throws Exception {
        String setRpcRequest = "{\"method\":\"DiscoverAll\"}";
        String actualResult = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Discover {"id":"2000"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverObject_Return_CONTENT() throws Exception {
        String expected = "/" + TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "/" + TEST_OBJECT_MULTI_WITH_RESOURCE_RW_ID + "_1.1";
        actualResult = sendDiscover(expected);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "/" + TEST_OBJECT_SINGLE_WITH_RESOURCE_R_ID;
        actualResult = sendDiscover(expected);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "/" + TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID + "_1.1";
        actualResult = sendDiscover(expected);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Discover {"id":"2000/0"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverInstance_Return_CONTENT() throws Exception {
        String actualResult = sendDiscover("/2000/0");
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Discover {"id":"2000/0/1"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverResource_Return_CONTENT() throws Exception {
        String expected = "/2000/0/1";
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }


    private String sendDiscover(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"" + this.method + "\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
