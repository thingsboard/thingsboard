/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcWithObjectId19_SingleResourceLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_5;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

public class RpcLwm2mIntegrationObserveWriteRead_ObjectId19_SingleResourceTest extends AbstractRpcWithObjectId19_SingleResourceLwM2MIntegrationTest {

    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9", "19/0/0/0"]} - Ok
     *  Return: "... 19/0/0/0=null"
     *  Observe {"id":"19/0/0"} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_Value_LwM2mSingleResource_LwM2mResourceInstanceNull_AfterRepeatOnlyObserve_Result_Ok() throws Exception {
        String actualResultCancelAll = sendObserve("ObserveCancelAll", null);
        ObjectNode rpcActualResultCancelAll = JacksonUtil.fromString(actualResultCancelAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultCancelAll.get("result").asText());
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5= objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_0_0_0 = idVer_19_0_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_0_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_7) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_3) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_5 + "=LwM2mSingleResource")));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(idVer_3_0_9) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer19_0_0_0) + "=null"));
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualReadAllValues =  rpcActualResultReadAll.get("value").asText();
        assertTrue(actualReadAllValues.contains(fromVersionedIdToObjectId(expectedIdVer19_0_0_0)));
        String actualResultObserve = sendObserve("Observe", idVer_19_0_0);
        ObjectNode rpcActualResultObserve = JacksonUtil.fromString(actualResultObserve, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultObserve.get("result").asText());
        String actualObserveValue =  rpcActualResultObserve.get("value").asText();
        String expected = String.format("LwM2mSingleResource id=%d", RESOURCE_ID_0);
        assertTrue(actualObserveValue.contains(expected));
    }

    /**
     *  ObserveComposite with keyName {"keys":["batteryLevel", "UtfOffset", "dataRead", "dataWrite"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeWithKeyName_Result_CONTENT_Value_SingleResources() throws Exception {
        String actualResultCancelAll = sendObserve("ObserveCancelAll", null);
        ObjectNode rpcActualResultCancelAll = JacksonUtil.fromString(actualResultCancelAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultCancelAll.get("result").asText());
        String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedKey19_0_0 = RESOURCE_ID_NAME_19_0_0;
        String expectedKey19_1_0 = RESOURCE_ID_NAME_19_1_0;
        String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_0 + "\", \"" + expectedKey19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByKeys("ObserveComposite", expectedKeys);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        String expectedIdVer3_0_14 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer3_0_14)));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(idVer_19_0_0)));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0)));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(idVer_3_0_9)));
    }

    /**
     *  ObserveReadAll
     * {"result":"CONTENT","value":"{"result":"CONTENT","value":"["SingleObservation:/3/0/9","SingleObservation:/3/0/14","SingleObservation:/19/1/0/0","SingleObservation:/19/0/0"]"} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_Result_CONTENT_Value_SingleObservation_Only() throws Exception {
        String actualResultCancelAll = sendObserve("ObserveCancelAll", null);
        ObjectNode rpcActualResultCancelAll = JacksonUtil.fromString(actualResultCancelAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultCancelAll.get("result").asText());
        String expectedIdVer3_0_14 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 ;
        String actualResult3_0_9 = sendObserve("Observe", idVer_3_0_9);
        ObjectNode rpcActualResult3_0_9 = JacksonUtil.fromString(actualResult3_0_9, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult3_0_9.get("result").asText());
        String actualResult3_0_14 = sendObserve("Observe", expectedIdVer3_0_14);
        ObjectNode rpcActualResult3_0_14 = JacksonUtil.fromString(actualResult3_0_14, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult3_0_14.get("result").asText());
        String actualResult19_1_0 = sendObserve("Observe", expectedIdVer19_1_0);
        ObjectNode rpcActualResult19_1_0 = JacksonUtil.fromString(actualResult19_1_0, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult19_1_0.get("result").asText());
        String actualResult19_0_0 = sendObserve("Observe", idVer_19_0_0);
        ObjectNode rpcActualResult19_0_0 = JacksonUtil.fromString(actualResult19_0_0, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult19_0_0.get("result").asText());
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues =  rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_3_0_9)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer3_0_14)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer19_1_0)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_19_0_0)));
    }

    /**
     * SingleResource
     * WriteReplace {"id": "/19_1.1/0/0","value": {"0":"0000ad45675600", "15":"1525ad45675600cdef"}}
     * {"result":"BAD_REQUEST"}
     */
    @Test
    public void testWriteReplaceMultipleResourceValueToSingleResource_Result_BAD_REQUEST_ValueSingleResourceMustBeOPAQUE() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;
        int resourceInstanceId0 = 0;
        int resourceInstanceId15 = 15;
        String expectedValue0 = "0000ad45675600";
        String expectedValue15 = "1525ad45675600cdef";
        String expectedValue = "{\"" + resourceInstanceId0 + "\":\"" + expectedValue0 + "\", \"" + resourceInstanceId15 + "\":\"" + expectedValue15 + "\"}";
        String actualResult = sendRPCWriteObjectById("WriteReplace", expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = String.format("Resource id=%s, value = {%s=%s, %s=%s}, class = LinkedHashMap. Format value is bad. " +
                        "Value for this Single Resource must be OPAQUE!",
                fromVersionedIdToObjectId(expectedPath), resourceInstanceId0, expectedValue0, resourceInstanceId15, expectedValue15);
        assertEquals(expected, rpcActualResult.get("error").asText());
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

    private String sendCompositeRPCByIds(String method, String paths) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"ids\":" + paths + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPCByKeys(String method, String keys) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"keys\":" + keys + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCWriteObjectById(String method, String path, Object value) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCReadById(String id) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + id + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
