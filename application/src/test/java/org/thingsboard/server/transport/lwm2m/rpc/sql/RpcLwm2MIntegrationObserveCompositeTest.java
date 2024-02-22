/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationObserveTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_15;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_5;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

public class RpcLwm2MIntegrationObserveCompositeTest extends AbstractRpcLwM2MIntegrationObserveTest {


    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9", "19/1/0/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_Value_LwM2mSingleResource_LwM2mResourceInstance() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5= objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_7) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_3) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_5) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(idVer_3_0_9) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0_0) + "=LwM2mResourceInstance"));
    }

    /**
     *  ObserveComposite {"ids":["19/1/0/0", "5/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveComposite_ObjectInstanceWithOtherObjectResourceInstance_Result_CONTENT_Ok() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String expectedIdVer5_0 = objectInstanceIdVer_5;
        String expectedIds = "[\"" + expectedIdVer19_1_0 + "\", \"" + expectedIdVer5_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actual= rpcActualResult.get("value").asText();
        assertTrue(actual.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0) + "=LwM2mMultipleResource"));
        assertTrue(actual.contains(fromVersionedIdToObjectId(expectedIdVer5_0) + "=LwM2mObjectInstance"));
    }

    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/2"]} - Ok
     *  "5/0/2" - Execute^ result == null
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_Value_LwM2mSingleResource_If_Error_Null() throws Exception {
//        sendCancelObserveAllWithAwait(deviceId);
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_2 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_2 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_7) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_2) + "=null"));
    }


    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/2"]} - Ok
     *  "5/0" contains "5/0/2"
     * @throws Exception
     */
    @Test
    public void testObserveComposite_Result_BAD_REQUEST_ONE_PATH_CONTAINCE_OTHER() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        String expectedIdVer5_0 = objectInstanceIdVer_5;
        String expectedIdVer5_0_2 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String expectedIds = "[\"" + expectedIdVer5_0 + "\", \"" + expectedIdVer5_0_2 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String actual= rpcActualResult.get("error").asText();
        String expected = "Invalid path list :  /5/0 and /5/0/2 are overlapped paths";
        assertTrue(expected.equals(actual));
    }

    /**
     *  Previous -> "3/0/9"
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9"]} - CONTENT
     * @throws Exception
     */
    @Test
    public void testObserveCompositeThereAreObservationOneResource_Result_CONTENT_Value_ObservationAddIfAbsent() throws Exception {
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5= objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedResult = "/3/0/9=LwM2mSingleResource [id=9";
        assertTrue(rpcActualResult.get("value").asText().contains(expectedResult));
    }

    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9", "19/1/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_Value_LwM2mSingleResource_LwM2mMultipleResource() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5= objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_7) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_3) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_5 + "=LwM2mSingleResource")));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(idVer_3_0_9) + "=LwM2mSingleResource"));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0) + "=LwM2mMultipleResource"));
    }

    /**
     *  ObserveComposite with keyName {"keys":["batteryLevel", "UtfOffset", "dataRead", "dataWrite"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeWithKeyName_Result_CONTENT_Value_SingleResources() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

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
        String expectedIdVer19_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer3_0_14)));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer19_0_0)));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0)));
        assertTrue(actualValues.contains(fromVersionedIdToObjectId(idVer_3_0_9)));
    }

    /**
     *  ObserveComposite with keyName {"keys":["batteryLevel", "UtfOffset", "dataRead", "dataWrite"]} - - BAD_REQUEST
     * @throws Exception
     */
    @Test
    public void testObserveCompositeWithKeyNameThereAreObservationOneResource_Result_CONTENT_Value_ObservationAddIfAbsent() throws Exception {
        String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedKey19_0_0 = RESOURCE_ID_NAME_19_0_0;
        String expectedKey19_1_0 = RESOURCE_ID_NAME_19_1_0;
        String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_0 + "\", \"" + expectedKey19_1_0 + "\"]";
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String actualResult = sendCompositeRPCByKeys("ObserveComposite", expectedKeys);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actual = rpcActualResult.get("value").asText();
        assertTrue(actual.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0) + "=LwM2mMultipleResource"));
    }

    /**
     *  ObserveReadAll
     * {"result":"CONTENT","value":"[\"CompositeObservation: [/19/1/0\",\"/19/0/0\",\"/3/0/14\",\"/3/0/9]\"]"} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_AfterCompositeObservation_Result_CONTENT_Value_SingleObservation_Only() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

        String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedKey19_0_0 = RESOURCE_ID_NAME_19_0_0;
        String expectedKey19_1_0 = RESOURCE_ID_NAME_19_1_0;
        String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_0 + "\", \"" + expectedKey19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByKeys("ObserveComposite", expectedKeys);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues =  rpcActualResultReadAll.get("value").asText();
        String expectedIdVer3_0_14 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_3_0_9)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer3_0_14)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer19_1_0)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_19_0_0)));
    }

    /**
     *  ObserveReadAll
     * {"result":"CONTENT","value":"{"result":"CONTENT","value":"["SingleObservation:/3/0/9","SingleObservation:/3/0/14","SingleObservation:/19/1/0/0","SingleObservation:/19/0/0"]"} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_Result_CONTENT_Value_SingleObservation_Only() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        
        String expectedIdVer3_0_14 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String actualResult3_0_9 = sendObserve("Observe", idVer_3_0_9);
        ObjectNode rpcActualResult3_0_9 = JacksonUtil.fromString(actualResult3_0_9, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult3_0_9.get("result").asText());
        String actualResult3_0_14 = sendObserve("Observe", expectedIdVer3_0_14);
        ObjectNode rpcActualResult3_0_14 = JacksonUtil.fromString(actualResult3_0_14, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult3_0_14.get("result").asText());
        String actualResult19_1_0_0 = sendObserve("Observe", expectedIdVer19_1_0_0);
        ObjectNode rpcActualResult19_1_0_0 = JacksonUtil.fromString(actualResult19_1_0_0, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult19_1_0_0.get("result").asText());
        String actualResult19_0_0 = sendObserve("Observe", idVer_19_0_0);
        ObjectNode rpcActualResult19_0_0 = JacksonUtil.fromString(actualResult19_0_0, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult19_0_0.get("result").asText());
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues =  rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_3_0_9)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer3_0_14)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer19_1_0_0)));
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_19_0_0)));
    }

    /**
     *  ObserveReadAll
     * {"result":"CONTENT","value":"[\"CompositeObservation: [/19/1/0\",\"/19/0/0\",\"/3/0/14\",\"/3/0/9]\"]"} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_AfterCompositeObservation_WithResourceNotReadable_Result_CONTENT_Value_SingleObservation_Only() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_2= objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_2 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();

        assertTrue(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_2) + "=null"));

        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        actualValues =  rpcActualResultReadAll.get("value").asText();

        assertFalse(actualValues.contains(fromVersionedIdToObjectId(expectedIdVer5_0_2)));

    }

    /**
     *  ObserveComposite {"ids":["/5/0/7", "/5/0/5", "/5/0/3", "/3/0/9", "/19/1/0/0"]} - Ok
     *  ObserveCompositeCancel {"ids":["/5/0/7", "/5/0/5", "/5/0/3", "/3/0/9", "/19/1/0/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_CancelObserveComposite_This_Result_Content_Count_5() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
            // ObserveComposite
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5= objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
           // ObserveCompositeCancel
        actualResult =  sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("5", rpcActualResult.get("value").asText());

        assertEquals(0, (Object) getCntObserveAll(deviceId));
    }

    /**
     *  ObserveComposite {"ids":["/3", "/5/0/3", "/19/1/0/0"]} - Ok
     *  ObserveCompositeCancel {"ids":["/3", "/5/0/3", "/19/1/0/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeOneObjectAnyResources_Result_CONTENT_CancelObserveComposite_This_Result_Content_Count_3() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
            // ObserveComposite
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + idVer_3_0_9 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
           // ObserveCompositeCancel
        actualResult =  sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("3", rpcActualResult.get("value").asText());

        assertEquals(0, (Object) getCntObserveAll(deviceId));
    }

    /**
     *  ObserveComposite {"ids":["/3/0/9", "/5/0/5", "/5/0/3", "/5/0/7", "/19/1/0/0"]} - Ok
     *  ObserveCompositeCancel {"ids":["/5", "/19/1/0/0"]} - Ok
     *  last Observation
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_CancelObserveComposite_OneObjectAnyResource_Result_Content_Count_4() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
            // ObserveComposite
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer3_0_9 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_9;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" +  expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + expectedIdVer3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        awaitObserveReadAll(5, deviceId);

            // ObserveCompositeCancel
        expectedIds = "[\"" + objectInstanceIdVer_5 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        actualResult =  sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("4", rpcActualResult.get("value").asText());   // CNT = 4 ("/5/0/5", "/5/0/3", "/5/0/7", "/19/1/0/0"9)

        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues =  rpcActualResultReadAll.get("value").asText();
        assertEquals("[\"SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer3_0_9) + "\"]", actualValues);
    }

    /**
     * ObserveComposite {"ids":["/3", "/5/0/3", "/19/1/0/0"]} - Ok
     * ObserveCompositeCancel {"ids":["/3/0/9", "/5/0/3", "/19/1/0/0"} -> BAD_REQUEST
     * ObserveCompositeCancel {"ids":["/3"} -> CONTENT
     */
    @Test
    public void testObserveOneObjectAnyResources_Result_CONTENT_Cancel_OneResourceFromObjectAnyResource_Result_BAD_REQUEST_Cancel_OneObject_Result_CONTENT() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
            // ObserveComposite
        sendCancelObserveAllWithAwait(deviceId);
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" +  objectIdVer_3 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());

           // ObserveCompositeCancel
        expectedIds = "[\"" + expectedIdVer19_1_0_0 + "\", \"" + idVer_3_0_9 + "\"]";
        actualResult =  sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedValue = "for observation path [" + fromVersionedIdToObjectId(objectIdVer_3) + "], that includes this observation path [" + fromVersionedIdToObjectId(idVer_3_0_9);
        assertTrue(rpcActualResult.get("error").asText().contains(expectedValue));

            // ObserveCompositeCancel
        expectedIds = "[\"" + objectIdVer_3 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        actualResult =  sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("2", rpcActualResult.get("value").asText());

        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues =  rpcActualResultReadAll.get("value").asText();
        assertEquals("[\"SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer5_0_3) + "\"]", actualValues);
    }


   /**
     *  ObserveComposite {"ids":["/3/0/9", "/3/0/14", "/5/0/3", "/3/0/15", "/19/1/0/0"]} - Ok
     *  ObserveCancel {"id":"/3/0/9"} -> INTERNAL_SERVER_ERROR
     *  ObserveCompositeCancel {"ids":["/3/0/9", "/19/1/0/0", "/3]} - Ok
     *  last Observation
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_CancelObserveComposite_OneResource_OneObjectAnyResource_Result_Content_Count_4() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
            // ObserveComposite
        sendCancelObserveAllWithAwait(deviceId);
        String expectedIdVer3_0_14 = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14;
        String expectedIdVer3_0_15= objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_15;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" +  idVer_3_0_9 + "\", \"" + expectedIdVer3_0_14 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + expectedIdVer3_0_15 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult =  sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
           // ObserveCompositeCancel
        expectedIds = "[\"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\", \"" + objectIdVer_3 + "\"]";
        actualResult =  sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("4", rpcActualResult.get("value").asText());

        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues =  rpcActualResultReadAll.get("value").asText();
        assertEquals("[\"SingleObservation:" + fromVersionedIdToObjectId(expectedIdVer5_0_3) + "\"]", actualValues);
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
}
