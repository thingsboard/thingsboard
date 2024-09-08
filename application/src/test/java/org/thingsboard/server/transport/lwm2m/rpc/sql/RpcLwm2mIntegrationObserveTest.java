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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationObserveTest;
import org.thingsboard.server.transport.lwm2m.server.uplink.DefaultLwM2mUplinkMsgHandler;

import java.util.Optional;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

@Slf4j
public class RpcLwm2mIntegrationObserveTest extends AbstractRpcLwM2MIntegrationObserveTest {

    @SpyBean
    DefaultLwM2mUplinkMsgHandler defaultUplinkMsgHandlerTest;

    @Test
    public void testObserveReadAll_Count_2_CancelAll_Count_0_Ok() throws Exception {
        String actualValuesReadAll = sendRpcObserveWithResultValue("ObserveReadAll", null);
        assertEquals(2, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:/19/0/0\"";
        assertTrue(actualValuesReadAll.contains(expected));
        expected = "\"SingleObservation:/3/0/9\"";
        assertTrue(actualValuesReadAll.contains(expected));
    }

    /**
     * Observe "3_1.2/0/9"
     * @throws Exception
     */
    @Test
    public void testObserveOneResource_Result_CONTENT_Value_Count_3_After_Cancel_Count_2() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);

        int cntUpdate = 3;
        verify(defaultUplinkMsgHandlerTest, timeout(10000).times(cntUpdate))
                .onUpdateValueAfterReadResponse(Mockito.any(Registration.class), eq(idVer_3_0_9), Mockito.any(ReadResponse.class));
    }

    /**
     * Observe "3_1.2/0"
     * @throws Exception
     */
    @Test
    public void testObserveOneObjectInstance_Result_CONTENT_Value_Count_3_After_Cancel_Count_2() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        String idVer_3_0 = objectInstanceIdVer_3;
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0);

        int cntUpdate = 3;
        verify(defaultUplinkMsgHandlerTest, timeout(10000).times(cntUpdate))
                .updateAttrTelemetry(Mockito.any(Registration.class), eq(idVer_3_0_9));
    }

    /**
     * Observe "3_1.2"
     * @throws Exception
     */
    @Test
    public void testObserveOneObject_Result_CONTENT_Value_Count_3_After_Cancel_Count_2() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        String idVer_3_0 = objectInstanceIdVer_3;
        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0);

        int cntUpdate = 3;
        verify(defaultUplinkMsgHandlerTest, timeout(10000).times(cntUpdate))
                .updateAttrTelemetry(Mockito.any(Registration.class), eq(idVer_3_0_9));
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/3_1.2/0/0"}
     * @throws Exception
     */
    @Test
    public void testObserveRepeated_Result_CONTENT_AddIfAbsent() throws Exception {
        sendRpcObserveWithResultValue("Observe", idVer_3_0_0);
        String rpcActualResult = sendRpcObserveWithResultValue("Observe", idVer_3_0_0);
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
        sendRpcObserveWithResultValue("Observe", idVer_3_0_0);
        String rpcActualResult = sendRpcObserveWithResultValue("Observe", idVer_3_0_0);
        String expected = "LwM2mSingleResource [id=0";
        assertTrue(rpcActualResult.contains(expected));
    }

    /**
     *  Observe {"id":["3"]} - Ok
     *  PreviousObservation  contains "3/0/9"
     * @throws Exception
     */
    @Test
    public void testObserve_Result_CONTENT_ONE_PATH_PreviousObservation_CONTAINCE_OTHER_CurrentObservation() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        // "3/0/9"
        sendRpcObserveWithResultValue("Observe", idVer_3_0_9);
        // "3"
        sendRpcObserveWithResultValue("Observe", objectIdVer_3);
        // PreviousObservation "3/0/9" change to CurrentObservation "3"
        String actualValuesReadAll = sendRpcObserveWithResultValue("ObserveReadAll", null);
        assertEquals(1, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:/3\"";
        assertTrue(actualValuesReadAll.contains(expected));
    }

    /**
     *  Observe {"id":["3/0/9"]} - Ok
     *  PreviousObservation  contains "3"
     * @throws Exception
     */
    @Test
    public void testObserve_Result_CONTENT_ONE_PATH_CurrentObservation_CONTAINCE_OTHER_PreviousObservation() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        // "3"
        sendRpcObserveWithResultValue("Observe", objectIdVer_3);
        // "3/0/0"; WARN: - Token collision ? existing observation [/3] includes input observation [/3/0/0]
        sendRpcObserveWithResultValue("Observe", idVer_3_0_0);

        String actualValuesReadAll = sendRpcObserveWithResultValue("ObserveReadAll", null);
        assertEquals(1, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:/3\"";
        assertTrue(actualValuesReadAll.contains(expected));
    }

    /**
     * Observe {"id":"/3/0/9"}
     * ObserveCancel {"id":"/3/0/9"}
     */
    @Test
    public void testObserveResource_ObserveCancelResource_Result_CONTENT_Count_1() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

        String actualValuesReadAll = sendRpcObserveReadAllWithResult(idVer_3_0_9);
        assertEquals(1, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:" + id_3_0_9 + "\"";
        assertTrue(actualValuesReadAll.contains(expected));

        // cancel observe "/3_1.2/0/9"
        sendRpcObserveWithResultValue("ObserveCancel", idVer_3_0_9);
    }

    /**
     * Observe {"id":"/3"}
     * ObserveCancel {"id":"/3/0/9"} -> INTERNAL_SERVER_ERROR
     * ObserveCancel {"id":"/3"} -> CONTENT
     */
    @Test
    public void testObserveObject_ObserveCancelOneResource_Result_INTERNAL_SERVER_ERROR_Than_Cancel_ObserveObject_Result_CONTENT_Count_1() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

        String actualValuesReadAll = sendRpcObserveReadAllWithResult(objectIdVer_3);
        assertEquals(1, actualValuesReadAll.split(",").length);
        String expected = "\"SingleObservation:" + fromVersionedIdToObjectId(objectIdVer_3) + "\"";
        assertTrue(actualValuesReadAll.contains(expected));

        // cancel observe "/3_1.2/0/9"
        ObjectNode rpcActualResult = sendRpcObserveWithResult("ObserveCancel", idVer_3_0_9);
        String expectedValue = "for observation path [" + fromVersionedIdToObjectId(objectIdVer_3) + "], that includes this observation path [" + id_3_0_9;
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("error").asText().contains(expectedValue));

        // cancel observe "/3_1.2"
        sendRpcObserveWithResultValue("ObserveCancel", objectIdVer_3);
    }

    /**
     * Observe {"id":"/3/0/0"}
     * Observe {"id":"/3/0/9"}
     * ObserveCancel {"id":"/3"} - Ok, cnt = 2
     */
    @Test
    public void testObserveResource_ObserveCancelObject_Result_CONTENT_Count_1() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);
        sendRpcObserveWithWithTwoResource(idVer_3_0_0, idVer_3_0_9);
        String rpcActualResul = sendRpcObserveWithResultValue("ObserveReadAll", null);
        assertEquals(2, rpcActualResul.split(",").length);
        String expected_3_0_0 = "\"SingleObservation:" + fromVersionedIdToObjectId(idVer_3_0_0) + "\"";
        String expected_3_0_9 = "\"SingleObservation:" + id_3_0_9 + "\"";
        assertTrue(rpcActualResul.contains(expected_3_0_0));
        assertTrue(rpcActualResul.contains(expected_3_0_9));

        // cancel observe "/3_1.2"
        String expectedId_3 = objectIdVer_3;
        String rpcActualResult = sendRpcObserveWithResultValue("ObserveCancel", expectedId_3);
        assertEquals("2", rpcActualResult);
    }

    /**
     * ObserveCancelAll
     * Observe {"id":"3_1.2/0/9"}
     * updateRegistration
     * idResources_3_1.2/0/9 => updateAttrTelemetry >= 10 times
     */
    @Test
    public void testObserveResource_Update_AfterUpdateRegistration() throws Exception {
        sendCancelObserveAllWithAwait(deviceId);

        int cntUpdate = 3;
        verify(defaultUplinkMsgHandlerTest, timeout(50000).atLeast(cntUpdate))
                .updatedReg(Mockito.any(Registration.class));

        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);

        cntUpdate = 10;
        verify(defaultUplinkMsgHandlerTest, timeout(50000).atLeast(cntUpdate))
                .updateAttrTelemetry(Mockito.any(Registration.class), eq(idVer_3_0_9));
    }

    private void sendRpcObserveWithWithTwoResource(String expectedId_1, String expectedId_2) throws Exception {
        sendRpcObserve("Observe", expectedId_1);
        sendRpcObserve("Observe", expectedId_2);
    }

    private String sendRpcObserveReadAllWithResult(String params) throws Exception {
        sendRpcObserve("Observe", params);
        ObjectNode rpcActualResult = sendRpcObserveWithResult("ObserveReadAll", null);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        return rpcActualResult.get("value").asText();
    }

    private ObjectNode sendRpcObserveWithResult(String method, String params) throws Exception {
        String actualResultReadAll = sendRpcObserve(method, params);
        return JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
    }

    private String sendRpcObserveWithResultValue(String method, String params) throws Exception {
        String actualResultReadAll = sendRpcObserve(method, params);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        return rpcActualResult.get("value").asText();
    }

    private void sendRpcObserveWithContainsLwM2mSingleResource(String params) throws Exception {
        String rpcActualResult = sendRpcObserveWithResultValue("Observe", params);
        assertTrue(rpcActualResult.contains("LwM2mSingleResource"));
        assertEquals(Optional.of(1).get(), Optional.ofNullable(getCntObserveAll(deviceId)).get());
    }

    private String sendRpcObserve(String method, String params) throws Exception {
        return sendObserve(method, params, deviceId);
    }
}

