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

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationObserve_Ver_1_1_Test;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;

@Slf4j
public class RpcLwm2mIntegrationObserveVer11Test extends AbstractRpcLwM2MIntegrationObserve_Ver_1_1_Test {

    public RpcLwm2mIntegrationObserveVer11Test() throws Exception {
    }

    @Before
    public void setupObserveTest() throws Exception {
        awaitObserveReadAll(4,lwM2MTestClient.getDeviceIdStr());
    }

    /**
     * Observe "3_1.1/0/9"
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
     * "3_1.1/0/9"
     * Observe count 4
     * CancelAll Observe
     * Reboot
     * Observe count 4 contains
     * "/3" - Discover Object - find ver (lwm2mVersion == 1.1)
     * @throws Exception
     */
    @Test
    public void testObserveOneResourceValue_Count_4_CancelAll_Reboot_After_Observe_Count_4_ObjectVer_1_1() throws Exception {
        String expectedIdVer = "</3>";
        testObserveOneResourceValue_Count_4_CancelAll_Reboot_After_Observe_Count_4(expectedIdVer);
    }
}
