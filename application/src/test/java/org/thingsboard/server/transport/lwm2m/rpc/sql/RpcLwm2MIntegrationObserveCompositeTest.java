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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_5;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

@Slf4j
public class RpcLwm2MIntegrationObserveCompositeTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9", "19/1/0/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_Value_LwM2mSingleResource_LwM2mResourceInstance() throws Exception {
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
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
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String expectedIdVer5_0 = objectInstanceIdVer_5;
        String expectedIds = "[\"" + expectedIdVer19_1_0 + "\", \"" + expectedIdVer5_0 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actual = rpcActualResult.get("value").asText();
        assertTrue(actual.contains(fromVersionedIdToObjectId(expectedIdVer19_1_0) + "=LwM2mMultipleResource"));
        assertTrue(actual.contains(fromVersionedIdToObjectId(expectedIdVer5_0) + "=LwM2mObjectInstance"));
    }

    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/2"]} - Ok
     *  "5/0/2" - Execute result == null
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_AfterCompositeObservation_WithResourceNotReadable_Result_CONTENT_ObserveResourceNotReadableIsNull() throws Exception {
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_2 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_2 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
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
        String expectedIdVer5_0 = objectInstanceIdVer_5;
        String expectedIdVer5_0_2 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String expectedIds = "[\"" + expectedIdVer5_0 + "\", \"" + expectedIdVer5_0_2 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String actual = rpcActualResult.get("error").asText();
        String expected = "Invalid path list :  /5/0 and /5/0/2 are overlapped paths";
        assertTrue(expected.equals(actual));
    }

    /**
     *  Previous -> "3/0/9" SingleObservation;
     *  if at least one of the resource objectIds (Composite) in SingleObservation or CompositeObservation is already registered - return BAD REQUEST
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9"]}
     * @throws Exception
     */
    @Test
    public void  testObserveComposite_IfLeastOneResourceIsAlreadyRegistered_return_BadRequest() throws Exception {
        // Verify after start
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues = rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("[]"));
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("error").asText().contains(fromVersionedIdToObjectId(idVer_3_0_9)));
        assertTrue(rpcActualResult.get("error").asText().contains("is already registered"));
        // verify after send Observe composite
        actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        actualValues = rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("SingleObservation:" + fromVersionedIdToObjectId(idVer_3_0_9)));
    }
    /**
     *  Previous -> ["5/0/7", "5/0/5", "5/0/3"], CompositeObservation     *
     *  if the resource SingleObservation is already registered in CompositeObservation - return BAD REQUEST
     *  SingleObservation {"id":["5/0/7"}
     * @throws Exception
     */
    @Test
    public void testObserveSingle_IfResourceIsAlreadyRegisteredInComposite_return_BadRequest() throws Exception {
        // Send Observe Composite
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedId5_0_7 = fromVersionedIdToObjectId(expectedIdVer5_0_7);
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedId5_0_5 = fromVersionedIdToObjectId(expectedIdVer5_0_5);
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedId5_0_3 = fromVersionedIdToObjectId(expectedIdVer5_0_3);

        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expectedId5_0_7));
        assertTrue(actualValues.contains(expectedId5_0_5));
        assertTrue(actualValues.contains(expectedId5_0_3));
        // Send Observe Single
        actualResult = sendObserve("Observe", expectedIdVer5_0_7);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("error").asText().contains(expectedId5_0_7));
        assertTrue(rpcActualResult.get("error").asText().contains("is already registered"));
        // verify after send Observe Single
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        actualValues = rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("CompositeObservation:"));
        assertTrue(actualValues.contains(expectedId5_0_7));
        assertTrue(actualValues.contains(expectedId5_0_5));
        assertTrue(actualValues.contains(expectedId5_0_3));
    }

    /**
     *  ObserveComposite {"ids":["5/0/7", "5/0/5", "5/0/3", "3/0/9", "19/1/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCompositeAnyResources_Result_CONTENT_Value_LwM2mSingleResource_LwM2mMultipleResource() throws Exception {
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
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
    public void testObserveCompositeWithKeyName_IfLeastOneResourceIsAlreadyRegistered_return_BadRequest() throws Exception {
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);
        String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedKey19_0_0 = RESOURCE_ID_NAME_19_0_0;
        String expectedKey19_1_0 = RESOURCE_ID_NAME_19_1_0;
        String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_0 + "\", \"" + expectedKey19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByKeys("ObserveComposite", expectedKeys);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("error").asText().contains("is already registered"));
    }

    /**
     *  ObserveReadAll
     * {"result":"CONTENT","value":"[\"CompositeObservation: [/19/1/0\",\"/19/0/0\",\"/3/0/14\",\"/3/0/9]\"]"} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveReadAll_AfterbserveCancelAllAndCompositeObservation_Result_CONTENT_Value_CompositeObservation_Only() throws Exception {
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
        String actualValues = rpcActualResultReadAll.get("value").asText();
        String expectedIdVer3_0_14 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedIdVer19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        assertTrue(actualValues.contains("CompositeObservation:"));
        assertFalse(actualValues.contains("SingleObservation"));
        assertTrue(actualValues.contains(Objects.requireNonNull(fromVersionedIdToObjectId(idVer_3_0_9))));
        assertTrue(actualValues.contains(Objects.requireNonNull(fromVersionedIdToObjectId(expectedIdVer3_0_14))));
        assertTrue(actualValues.contains(Objects.requireNonNull(fromVersionedIdToObjectId(expectedIdVer19_1_0))));
        assertTrue(actualValues.contains(Objects.requireNonNull(fromVersionedIdToObjectId(idVer_19_0_0))));
    }

    /**
     *  ObserveComposite {"ids":["/5/0/7", "/5/0/5", "/5/0/3", "/3/0/9", "/19/1/0/0"]} - Ok
     *  ObserveCompositeCancel {"ids":["/5/0/7", "/5/0/5", "/5/0/3", "/3/0/9", "/19/1/0/0"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserveCancelAllThenObserveCompositeAnyResources_Result_CONTENT_CancelObserveComposite_This_Result_Content_Count_1() throws Exception {
        // ObserveComposite
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + idVer_3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        // ObserveCompositeCancel
        actualResult = sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("1", rpcActualResult.get("value").asText());

        assertEquals(0, (Object) getCntObserveAll(lwM2MTestClient.getDeviceIdStr()));
    }

    /**
     *  ObserveComposite {"ids":["/3/0/9", "/5/0/5", "/5/0/3", "/5/0/7", "/19/1/0/0"]} - Ok
     *  ObserveCompositeCancel {"ids":["/5", "/19/1/0/0"]} - BadRequest
     * @throws Exception
     */
    @Test
    public void testObserveCompositeFiveResources_Result_CONTENT_CancelObserveComposite_TwoAnyResource_Result_BadRequest() throws Exception {
        // ObserveComposite five
        String expectedIdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String expectedIdVer5_0_5 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_5;
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer3_0_9 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_9;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + expectedIdVer5_0_7 + "\", \"" + expectedIdVer5_0_5 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + expectedIdVer3_0_9 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        awaitObserveReadAll(1, lwM2MTestClient.getDeviceIdStr());

        // ObserveCompositeCancel two
        expectedIds = "[\"" + objectInstanceIdVer_5 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        actualResult = sendCompositeRPCByIds("ObserveCompositeCancel", expectedIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("error").asText().contains(objectInstanceIdVer_5));   // CNT = 4 ("/5/0/5", "/5/0/3", "/5/0/7", "/19/1/0/0"9)
        assertTrue(rpcActualResult.get("error").asText().contains(expectedIdVer19_1_0_0));   // CNT = 4 ("/5/0/5", "/5/0/3", "/5/0/7", "/19/1/0/0"9)
    }

    /**
     * ObserveComposite {"ids":["/3", "/5/0/3", "/19/1/0/0"]} - Ok
     * ObserveCompositeCancel {"ids":["/19/1/0/0", "/3/0/9"} -> BAD_REQUEST
     */
    @Test
    public void testObserveOneObjectAnyResources_Result_CONTENT_Cancel_OneResourceFromObjectAnyResource_Result_BAD_REQUEST_Cancel_OneObject_Result_CONTENT() throws Exception {
        // ObserveComposite
        String expectedIdVer5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String expectedIdVer19_1_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "/" + RESOURCE_INSTANCE_ID_0;
        String expectedIds = "[\"" + objectIdVer_3 + "\", \"" + expectedIdVer5_0_3 + "\", \"" + expectedIdVer19_1_0_0 + "\"]";
        String actualResult = sendCompositeRPCByIds("ObserveComposite", expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());

        // ObserveCompositeCancel
        String sendIds = "[\"" + expectedIdVer19_1_0_0 + "\", \"" + idVer_3_0_9 + "\"]";
        actualResult = sendCompositeRPCByIds("ObserveCompositeCancel", sendIds);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedValue = "Could not find active Observe Composite component with paths: [/19_1.1/1/0/0, /3_1.2/0/9]";
        assertTrue(rpcActualResult.get("error").asText().contains(expectedValue));

        // "ObserveReadAll"
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues = rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("CompositeObservation:"));
    }


    @Test
    public void testObserveCompositeResource_Update_After_Registration_UpdateRegistration() throws Exception {
        String id_3_0_9 = fromVersionedIdToObjectId(idVer_3_0_9);
        String id_19_0_0 = fromVersionedIdToObjectId(idVer_19_0_0);
        String idVer_19_1_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0;
        String id_19_1_0 = fromVersionedIdToObjectId(idVer_19_1_0);
        String idVer_19_0_2 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2;
        String id_19_0_2 = fromVersionedIdToObjectId(idVer_19_0_2);

        // 1 - Verify after start
        String actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValues = rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("[]"));
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_19_0_0);
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_19_1_0);
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_19_0_2);

        actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        actualValues = rpcActualResultReadAll.get("value").asText();
        assertTrue(actualValues.contains("SingleObservation:" + id_3_0_9));
        assertTrue(actualValues.contains("SingleObservation:" + id_19_1_0));
        assertTrue(actualValues.contains("SingleObservation:" + id_19_0_2));
        assertTrue(actualValues.contains("SingleObservation:" + id_19_0_0));
        long initAttrTelemetryAtCount = countUpdateAttrTelemetryAll();
        long initAttrTelemetryAtCount_3_0_9 = countUpdateAttrTelemetryResource(idVer_3_0_9);
        long initAttrTelemetryAtCount_19_0_0 = countUpdateAttrTelemetryResource(idVer_19_0_0);
        long initAttrTelemetryAtCount_19_1_0 = countUpdateAttrTelemetryResource(idVer_19_1_0);
        long initAttrTelemetryAtCount_19_0_2 = countUpdateAttrTelemetryResource(idVer_19_0_2);
        updateRegAtLeastOnceAfterAction();
        updateAttrTelemetryAllAtLeastOnceAfterAction(initAttrTelemetryAtCount);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_3_0_9, idVer_3_0_9);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_19_0_0, idVer_19_0_0);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_19_1_0, idVer_19_1_0);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_19_0_2, idVer_19_0_2);

        // 2 - "ObserveReadAll": No update of all resources we are observing - after "ObserveReadCancelAll"
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
        updateRegAtLeastOnceAfterAction();
        actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String rpcActualVValuesReadAll = rpcActualResultReadAll.get("value").asText();
        ArrayNode rpcactualValues = JacksonUtil.fromString(rpcActualVValuesReadAll, ArrayNode.class);
        assertEquals(rpcactualValues.size(), 0);
        // 2.1 - ObserveComposite: observeCancelAll verify"
        initAttrTelemetryAtCount = countUpdateAttrTelemetryAll();
        initAttrTelemetryAtCount_3_0_9 = countUpdateAttrTelemetryResource(idVer_3_0_9);
        initAttrTelemetryAtCount_19_0_0 = countUpdateAttrTelemetryResource(idVer_19_0_0);
        initAttrTelemetryAtCount_19_1_0 = countUpdateAttrTelemetryResource(idVer_19_1_0);
        initAttrTelemetryAtCount_19_0_2 = countUpdateAttrTelemetryResource(idVer_19_0_2);
        updateRegAtLeastOnceAfterAction();
        assertEquals(countUpdateAttrTelemetryAll(), initAttrTelemetryAtCount);
        assertEquals(countUpdateAttrTelemetryResource(idVer_3_0_9), initAttrTelemetryAtCount_3_0_9);
        assertEquals(countUpdateAttrTelemetryResource(idVer_19_0_0), initAttrTelemetryAtCount_19_0_0);
        assertEquals(countUpdateAttrTelemetryResource(idVer_19_1_0), initAttrTelemetryAtCount_19_1_0);
        assertEquals(countUpdateAttrTelemetryResource(idVer_19_0_2), initAttrTelemetryAtCount_19_0_2);

        // 3 - ObserveComposite: at least one update value of all resources we observe - after ObserveComposite"
        String expectedKeys = "[\"" + RESOURCE_ID_NAME_3_9 + "\", \"" + RESOURCE_ID_NAME_19_0_0 + "\", \"" + RESOURCE_ID_NAME_19_0_2 + "\", \"" + RESOURCE_ID_NAME_19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByKeys("ObserveComposite", expectedKeys);
        assertTrue(actualResult.contains(id_3_0_9  + "=LwM2mSingleResource"));
        assertTrue(actualResult.contains(id_19_0_0 + "=LwM2mMultipleResource"));
        assertTrue(actualResult.contains(id_19_1_0 + "=LwM2mMultipleResource"));
        assertTrue(actualResult.contains(id_19_0_2 + "=LwM2mSingleResource"));
        // 3.1 - ObserveComposite: - verify");
        actualResultReadAll = sendCompositeRPCByKeys("ObserveReadAll", null);
        rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        rpcActualVValuesReadAll = rpcActualResultReadAll.get("value").asText();
        rpcactualValues = JacksonUtil.fromString(rpcActualVValuesReadAll, ArrayNode.class);
        assertEquals(rpcactualValues.size(), 1);
        assertFalse(actualResultReadAll.contains("SingleObservation"));
        assertTrue(actualResultReadAll.contains("CompositeObservation:"));
        assertTrue(actualResultReadAll.contains(id_19_0_2));
        assertTrue(actualResultReadAll.contains(id_19_1_0));
        assertTrue(actualResultReadAll.contains(id_19_0_0));
        assertTrue(actualResultReadAll.contains(id_3_0_9));
        initAttrTelemetryAtCount = countUpdateAttrTelemetryAll();
        initAttrTelemetryAtCount_3_0_9 = countUpdateAttrTelemetryResource(idVer_3_0_9);
        initAttrTelemetryAtCount_19_0_0 = countUpdateAttrTelemetryResource(idVer_19_0_0);
        initAttrTelemetryAtCount_19_1_0 = countUpdateAttrTelemetryResource(idVer_19_1_0);
        initAttrTelemetryAtCount_19_0_2 = countUpdateAttrTelemetryResource(idVer_19_0_2);
        updateRegAtLeastOnceAfterAction();
        updateAttrTelemetryAllAtLeastOnceAfterAction(initAttrTelemetryAtCount);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_3_0_9, idVer_3_0_9);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_19_0_0, idVer_19_0_0);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_19_1_0, idVer_19_1_0);
        updateAttrTelemetryResourceAtLeastOnceAfterAction(initAttrTelemetryAtCount_19_0_2, idVer_19_0_2);
    }

    private String sendObserve(String method, String params) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        } else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), sendRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPCByIds(String method, String paths) throws Exception {
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"ids\":" + paths + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPCByKeys(String method, String keys) throws Exception {
        String sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"keys\":" + keys + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), sendRpcRequest, String.class, status().isOk());
    }

    private void updateAttrTelemetryAllAtLeastOnceAfterAction(long initialInvocationCount) {
        AtomicLong newInvocationCount = new AtomicLong(initialInvocationCount);
        log.warn("countUpdateAttrTelemetryAllAtLeastOnceAfterAction: initialInvocationCount [{}]", initialInvocationCount);
        await("Update AttrTelemetryAll at-least-once after action")
                .atMost(50, TimeUnit.SECONDS)
                .until(() -> {
                    newInvocationCount.set(countUpdateAttrTelemetryAll());
                    return newInvocationCount.get() > initialInvocationCount;
                });
        log.warn("countUpdateAttrTelemetryAllAtLeastOnceAfterAction: newInvocationCount [{}]", newInvocationCount.get());
    }

    private void updateAttrTelemetryResourceAtLeastOnceAfterAction(long initialInvocationCount, String idVerRez) {
        AtomicLong newInvocationCount = new AtomicLong(initialInvocationCount);
        log.warn("countUpdateAttrTelemetryResourceAtLeastOnceAfterAction: initialInvocationCount [{}]", initialInvocationCount);
        await("Update AttrTelemetryResource at-least-once after action")
                .atMost(50, TimeUnit.SECONDS)
                .until(() -> {
                    newInvocationCount.set(countUpdateAttrTelemetryResource(idVerRez));
                    return newInvocationCount.get() > initialInvocationCount;
                });
        log.warn("countUpdateAttrTelemetryResourceAtLeastOnceAfterAction: newInvocationCount [{}]", newInvocationCount.get());
    }
}
