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
package org.thingsboard.server.transport.lwm2m.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RpcLwm2mIntegrationCreateTest extends RpcAbstractLwM2MIntegrationTest {


    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19/2","value":{"1":2}}
     * Create  {"id":"/19/3","value":{"0":{"0":"00AC", "1":"ddff12"}}}
     */
    @Ignore // in developing
    @Test
    public void testCreateObjectInstanceWithoutInstanceIdByIdKey_Result_CHANGED_Location() throws Exception {
        String expectedPath = "/19_1.1";
        String expectedValue = "{\"0\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * failed: cannot_create_mandatory_single_object
     * Create  {"id":"/3/2","value":{"0":"00AC"}}
     */


    /**
     * failed:  cannot_create_instance_of_security_object
     * Create  {"id":"/0/2","value":{"0":"00AC"}}
     */

    /**
     * failed: cannot_create_instance_of_absent_object
     * Create  {"id":"/50/2","value":{"0":"00AC"}}
     */


    private String sendRPCreateById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"Create\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
