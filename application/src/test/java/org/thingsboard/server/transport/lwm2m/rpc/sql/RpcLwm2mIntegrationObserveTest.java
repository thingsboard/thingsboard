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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceUpdateResult;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

@Slf4j
public class RpcLwm2mIntegrationObserveTest extends AbstractRpcLwM2MIntegrationTest {

    @Before
    public void setupObserveTest() throws Exception {
        awaitObserveReadAll(4,lwM2MTestClient.getDeviceIdStr());
    }


    @Test
    public void testObserveReadAll_Count_4_CancelAll_Count_0_Ok() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
    }

    /**
     * Observe "3_1.2/0/9"
     * @throws Exception
     */
    @Test
    public void testObserveOneResource_Result_CONTENT_Value_Count_3_After_Cancel_Count_2() throws Exception {
        long initSendTelemetryAtCount = countSendParametersOnThingsboardTelemetryResource(RESOURCE_ID_NAME_3_9);
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);
        updateRegAtLeastOnceAfterAction();
        long lastSendTelemetryAtCount = countSendParametersOnThingsboardTelemetryResource(RESOURCE_ID_NAME_3_9);
        assertTrue(lastSendTelemetryAtCount > initSendTelemetryAtCount);
    }

    /**
     * Observe "3_1.2/0"
     * @throws Exception
     */
    @Test
    public void testObserveOneObjectInstance_Result_CONTENT_Value_Count_Equal_Greater_3_After_Cancel_Count_2() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
        String idVer_3_0 = objectInstanceIdVer_3;
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0);

        int cntUpdate = 3;
        verify(defaultUplinkMsgHandlerTest, timeout(10000).atLeast(cntUpdate))
                .updateAttrTelemetry(Mockito.any(ResourceUpdateResult.class), eq(null));
    }

    /**
     * Observe "3_1.2"
     * @throws Exception
     */
    @Test
    public void testObserveOneObject_Result_CONTENT_Value_Count_Equal_Greater_3_After_Cancel_Count_2() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
        String idVer_3_0 = objectInstanceIdVer_3;
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0);

        int cntUpdate = 3;
        verify(defaultUplinkMsgHandlerTest, timeout(10000).atLeast(cntUpdate))
                .updateAttrTelemetry(Mockito.any(ResourceUpdateResult.class), eq(null));
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/3_1.2/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveRepeated_Result_CONTENT_AddIfAbsent() throws Exception {
        sendRpcObserveOkWithResultValue("Observe", idVer_3_0_0);
        String rpcActualResult = sendRpcObserveOkWithResultValue("Observe", idVer_3_0_0);
        String expected = "LwM2mSingleResource [id=0";
        assertTrue(rpcActualResult.contains(expected));
    }

    /**
     * Observe {"id":"/3_1.?/0/13"}
     * @throws Exception
     */
    @Test
    public void testObserveWithBadVersion_Result_BadRequest_ErrorMsg_BadVersionMustBe_Ver() throws Exception {
        String expectedInstance = (String) expectedInstances.stream().filter(path -> !((String) path).contains("_")).findFirst().get();
        LwM2mPath expectedPath = new LwM2mPath(expectedInstance);
        int expectedResource = lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().resources.entrySet().stream().findAny().get().getKey();
        String ver = lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().version;
        String expectedId = "/" + expectedPath.getObjectId() + "_" + Version.MAX + "/" + expectedPath.getObjectInstanceId() + "/" + expectedResource;
        ObjectNode rpcActualResult = sendRpcObserveWithResult("Observe", expectedId);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Specified resource id " + expectedId + " is not valid version! Must be version: " + ver;
        assertEquals(expected, rpcActualResult.get("error").asText());
    }

    /**
     * Not implemented Instance
     * Observe {"id":"/2/0"}
     * @throws Exception
     */
    @Test
    public void testObserveNoImplementedInstanceOnDevice_Result_NotFound() throws Exception {
        String objectInstanceIdVer = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).contains("/" + ACCESS_CONTROL)).findFirst().get();
        String expected = objectInstanceIdVer + "/" + OBJECT_INSTANCE_ID_0;
        ObjectNode rpcActualResult = sendRpcObserveWithResult("Observe", expected);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Not implemented Resource
     * Observe {"id":"/19_1.1/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveNoImplementedResourceOnDeviceValueNull_Result_BadRequest() throws Exception {
        String expected = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        ObjectNode rpcActualResult = sendRpcObserveWithResult("Observe", expected);
        String expectedValue = "value MUST NOT be null";
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertEquals(expectedValue, rpcActualResult.get("error").asText());
    }

    /**
     * Observe resource Write -> "/5/0/02"
     * @throws Exception
     */
    @Test
    public void testObserveResourceNotRead_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedId = objectInstanceIdVer_5 + "/" + RESOURCE_ID_0;
        ObjectNode rpcActualResult = sendRpcObserveWithResult("Observe", expectedId);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Observe resource Execute -> "/5/0/2"
     * @throws Exception
     */
    @Test
    public void testObserveExecuteResource_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedId = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        ObjectNode rpcActual = sendRpcObserveWithResult("Observe", expectedId);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActual.get("result").asText());
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/3_1.2/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveRepeatedRequestObserveOnDevice_Result_CONTENT_PutIfAbsent() throws Exception {
        sendRpcObserveOkWithResultValue("Observe", idVer_3_0_0);
        String rpcActualResult = sendRpcObserveOkWithResultValue("Observe", idVer_3_0_0);
        String expected = "LwM2mSingleResource [id=0";
        assertTrue(rpcActualResult.contains(expected));
    }

    /**
     *  PreviousObservation  contains "3/0/9"
     *  Observe {"id":["19"]} - Bad Request
     *  Observe {"id":["19/0"]} - Bad Request
     *  Observe {"id":["19/1"]} - Ok
     * @throws Exception
     */
    @Test
    public void testObserves_OverlappedPaths_FirstResource_SecondObjectOrInstance() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
        // "19/0/0"
        sendRpcObserveOkWithResultValue("Observe", idVer_19_0_0);
        // PreviousObservation "19/0/0" change to CurrentObservation "19" - object
        ObjectNode rpcActualResult = sendRpcObserveWithResult("Observe", objectIdVer_19);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Resource [" + fromVersionedIdToObjectId(objectIdVer_19) + "] conflict with is already registered as SingleObservation [" + fromVersionedIdToObjectId(idVer_19_0_0)  + "].";
        assertEquals(expected, rpcActualResult.get("error").asText());
        // Verify ObserveReadAll
        String actualValuesReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        String expectedReadAll = "[\"SingleObservation:/19/0/0\"]";
        assertEquals(expectedReadAll, actualValuesReadAll);
        // PreviousObservation "19/0/0" change to CurrentObservation "19/0" - instance
        String expectedIdVer19_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        rpcActualResult = sendRpcObserveWithResult("Observe", expectedIdVer19_0);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        expected = "Resource [" + fromVersionedIdToObjectId(expectedIdVer19_0) + "] conflict with is already registered as SingleObservation [" + fromVersionedIdToObjectId(idVer_19_0_0)  + "].";
        assertEquals(expected, rpcActualResult.get("error").asText());
        // Verify ObserveReadAll
        actualValuesReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        assertEquals(expectedReadAll, actualValuesReadAll);
        // PreviousObservation "19/0/0" add CurrentObservation "19/1" - instance
        String expectedIdVer19_1 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1;
        rpcActualResult = sendRpcObserveWithResult("Observe", expectedIdVer19_1);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mObjectInstance"));
        // Verify ObserveReadAll
        actualValuesReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        assertTrue(actualValuesReadAll.contains("SingleObservation:/19/1"));
        assertTrue(actualValuesReadAll.contains("SingleObservation:/19/0/0"));
        // PreviousObservation "19/1/"- instance change to CurrentObservation "19/1/0" - resource
        String expectedIdVer19_1_0 = expectedIdVer19_1 + "/" + RESOURCE_ID_0;
        rpcActualResult = sendRpcObserveWithResult("Observe", expectedIdVer19_1_0);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        expected = "Resource [" + fromVersionedIdToObjectId(expectedIdVer19_1_0) + "] conflict with is already registered as SingleObservation [" + fromVersionedIdToObjectId(expectedIdVer19_1)  + "].";
        assertEquals(expected, rpcActualResult.get("error").asText());
        // Verify ObserveReadAll
        actualValuesReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        assertTrue(actualValuesReadAll.contains("SingleObservation:/19/1"));
        assertTrue(actualValuesReadAll.contains("SingleObservation:/19/0/0"));
    }

    /**
     * Observe {"id":"/3/0/9"}
     * ObserveCancel {"id":"/3/0/9"}
     */
    @Test
    public void testObserveResource_ObserveCancelResource_Result_CONTENT_Count_1() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());

        String actualValuesReadAll = sendRpcObserveReadAllWithResult(idVer_3_0_9);
        assertEquals(1, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:" + id_3_0_9 + "\"";
        assertTrue(actualValuesReadAll.contains(expected));

        // cancel observe "/3_1.2/0/9"
        sendRpcObserveOkWithResultValue("ObserveCancel", idVer_3_0_9);
    }

    /**
     * Observe {"id":"/3"}
     * ObserveCancel {"id":"/3/0/9"} -> INTERNAL_SERVER_ERROR
     * ObserveCancel {"id":"/3"} -> CONTENT
     */
    @Test
    public void testObserveObject_ObserveCancelOneResource_Result_INTERNAL_SERVER_ERROR_Than_Cancel_ObserveObject_Result_CONTENT_Count_1() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());

        String actualValuesReadAll = sendRpcObserveReadAllWithResult(objectIdVer_3);
        assertEquals(1, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:" + fromVersionedIdToObjectId(objectIdVer_3) + "\"";
        assertTrue(actualValuesReadAll.contains(expected));

        // cancel observe "/3_1.2/0/9"
        ObjectNode rpcActualResult = sendRpcObserveWithResult("ObserveCancel", idVer_3_0_9);
        String expectedValue = "Could not find active Observe component with path: " +  idVer_3_0_9;
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("error").asText().contains(expectedValue));

        // cancel observe "/3_1.2"
        sendRpcObserveOkWithResultValue("ObserveCancel", objectIdVer_3);
    }

    /**
     * Observe {"id":"/3/0/0"}
     * Observe {"id":"/3/0/9"}
     * ObserveCancel {"id":"/3"} - Bad
     * ObserveCancel {"/3/0/0"} - Ok
     * ObserveCancel {"/3/0/9"} - Ok
     *
     */
    @Test
    public void testObserveResource_ObserveCancelObject_Result_CONTENT_Count_1() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
        sendRpcObserveWithWithTwoResource(idVer_3_0_0, idVer_3_0_9);
        String rpcActualResul = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        assertEquals(2, rpcActualResul.split(",").length);
        String expected_3_0_0 = "\"SingleObservation:" + fromVersionedIdToObjectId(idVer_3_0_0) + "\"";
        String expected_3_0_9 = "\"SingleObservation:" + id_3_0_9 + "\"";
        assertTrue(rpcActualResul.contains(expected_3_0_0));
        assertTrue(rpcActualResul.contains(expected_3_0_9));
        // cancel observe "/3_1.2"
        ObjectNode rpcActualResult = sendRpcObserveWithResult("ObserveCancel", objectIdVer_3);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Could not find active Observe component with path: " + objectIdVer_3;
        assertEquals(expected, rpcActualResult.get("error").asText());
        // Verify ObserveReadAll
        rpcActualResul = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        String expectedReadAll = "[\"SingleObservation:/19/0/0\"]";
        assertTrue(rpcActualResul.contains(expected_3_0_0));
        assertTrue(rpcActualResul.contains(expected_3_0_9));
    }

    /**
     * ObserveCancelAll
     * Observe {"id":"3_1.2/0/9"}
     * updateRegistration
     * idResources_3_1.2/0/9 => updateAttrTelemetry >= 10 times
     */
    @Test
    public void testObserveResource_Update_AfterUpdateRegistration() throws Exception {
        sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());

        awaitUpdateReg(3);

        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);

        int cntUpdate = 10;
        verify(defaultUplinkMsgHandlerTest, timeout(50000).atLeast(cntUpdate))
                .updateAttrTelemetry(Mockito.any(ResourceUpdateResult.class), eq(null));
    }

    private void sendRpcObserveWithWithTwoResource(String expectedId_1, String expectedId_2) throws Exception {
        sendRpcObserveOk("Observe", expectedId_1);
        sendRpcObserveOk("Observe", expectedId_2);
    }
}

