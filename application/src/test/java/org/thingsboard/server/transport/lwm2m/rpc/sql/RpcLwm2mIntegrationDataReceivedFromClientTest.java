package org.thingsboard.server.transport.lwm2m.rpc.sql;

import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

public class RpcLwm2mIntegrationDataReceivedFromClientTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectLwm2mSuccessAndObserveTelemetry() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC;
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        super.testConnectionWithoutObserveWithDataReceivedSingleTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, false);
    }

}
