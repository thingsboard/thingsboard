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

public class RpcLwm2mIntegrationTest extends RpcAbstractLwM2MIntegrationTest {


    /**
     * DiscoverAll
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
     * ObserveReadAll
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_Result_CONTENT_Return_Count_0() throws Exception {
        String setRpcRequest = "{\"method\": \"ObserveReadAll\"}";
        String actualResult = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("[]", rpcActualResult.get("value").asText());
    }

    /**
     * Observe {"id":"/2001/0/3"}
     * @throws Exception
     */
    @Test
    public void testObserve_Result_CONTENT_Return_SingleResource() throws Exception {
        String actualResult = sendObserve ("/2001_1.1/0/3");
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
    }

    @Test
    public void testObserveWithBadVersion_Result_BadRequest_Return_MsgBadVersionMustBe1_1() throws Exception {
        String actualResult = sendObserve ("/2001/0/3");
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Specified resource id /2001/0/3 is not valid version! Must be version: 1.1";
        assertEquals(expected, rpcActualResult.get("error").asText());
    }

    /**
     * ObserveReadAll
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_Result_CONTENT_Return_Count_1() throws Exception {
        sendObserve ("/2001_1.1/0/3");
        String setRpcRequest = "{\"method\": \"ObserveReadAll\"}";
        String actualResult = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("[\"/2001/0/3\"]", rpcActualResult.get("value").asText());
    }

    private String sendObserve (String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Observe\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
