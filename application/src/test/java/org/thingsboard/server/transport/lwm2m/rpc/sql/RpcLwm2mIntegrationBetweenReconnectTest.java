// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import org.eclipse.leshan.core.ResponseCode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;

@TestPropertySource(properties = {
    "transport.sessions.inactivity_timeout=6000",
            "transport.sessions.report_timeout=6000"
})
public class RpcLwm2mIntegrationBetweenReconnectTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Connect Lwm2m client with endpoint to device
     * sleep Lwm2m without Observe
     * Remove active session of client:
            *         "transport.sessions.inactivity_timeout=6000",
            *         "transport.sessions.report_timeout=6000",
            * Delete device  with endpoint and Create new Device with endpoint
     * Connect Lwm2m client with endpoint to device after sleep with old Registration - result = 404
            * new ReConnect Lwm2m client with endpoint to device - new Registration
     * Observe 3/0/9
            * @throws Exception
     */
    @Test
    public void testWithNoSecConnectLwm2mSuccessAndCreateNewDeviceAfterDeleteDevice() throws Exception {
        // Connect client with endpoint to device and sleep
        String oldDeviceIdStr = lwM2MTestClient.getDeviceIdStr();
        sendObserveCancelAllWithAwait(oldDeviceIdStr);

        // Client sleep after registration
        lwM2MTestClient.sleepClient();

        DeviceCredentials deviceCredentials = doGet("/api/device/" + oldDeviceIdStr + "/credentials", DeviceCredentials.class);
        LwM2MDeviceCredentials lwM2MDeviceCredentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
        String clientEndpoint = lwM2MDeviceCredentials.getClient().getEndpoint();
        Device oldDevice = doGet("/api/tenant/devices?deviceName=" + clientEndpoint, Device.class);
        LwM2mClient lwM2mClient = clientContextTest.getClientByEndpoint(clientEndpoint);
        DeviceId oldDeviceId = oldDevice.getId();

        // Wait until the old transport session is closed due to inactivity
        TransportProtos.SessionInfoProto oldSession = lwM2mClient.getSession();
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(20, TimeUnit.MILLISECONDS)
                .until(() -> !transportServiceTest.hasSession(oldSession));

        // Delete device with endpoint
        doDelete("/api/device/" + oldDeviceIdStr)
                .andExpect(status().isOk());

        doGet("/api/device/" + oldDeviceIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", oldDeviceIdStr))));

        // Create new Device with the same endpoint
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        createLwm2mDevice(clientCredentials, clientEndpoint, oldDevice.getDeviceProfileId());
        Device newDevice = doGet("/api/tenant/devices?deviceName=" + clientEndpoint, Device.class);
        DeviceId newDeviceId = newDevice.getId();
        Assert.assertNotEquals(oldDeviceId, newDeviceId);

        // Wake up client -> stale registration is cleared -> 4.04 Not Found -> client re-registers to newDevice
        lwM2MTestClient.setLastUpdateFailureCode(null);
        lwM2MTestClient.getClientStates().clear();
        lwM2MTestClient.setDeviceIdStr(newDeviceId.getId().toString());
        lwM2MTestClient.unSleepClient();
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    LwM2mClient currentClient = clientContextTest.getClientByEndpoint(clientEndpoint);
                    return currentClient != null
                            && newDeviceId.getId().equals(currentClient.getDeviceId())
                            && transportServiceTest.hasSession(currentClient.getSession())
                            && ResponseCode.NOT_FOUND.equals(lwM2MTestClient.getLastUpdateFailureCode())
                            && lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS);
                });

        sendRpcObserveWithContainsLwM2mSingleResource(idVer_3_0_9);
        updateRegAtLeastOnceAfterAction();
        awaitObserveReadAll(1, newDeviceId.getId().toString());

        LwM2mClient newLwM2mClient = clientContextTest.getClientByEndpoint(clientEndpoint);
        Assert.assertNotEquals(oldSession, newLwM2mClient.getSession());
    }
}
