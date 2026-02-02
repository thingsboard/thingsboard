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
package org.thingsboard.server.transport.lwm2m.security.cid;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpoint;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_READ_CONNECTION_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_WRITE_CONNECTION_ID;

@DaoSqlTest
@Slf4j
public abstract class AbstractSecurityLwM2MIntegrationDtlsCidLengthTest extends AbstractSecurityLwM2MIntegrationTest {

    protected String awaitAlias;

    protected void testNoSecDtlsCidLength(Integer dtlsCidLength, Integer serverDtlsCidLength) throws Exception {
        initDeviceCredentialsNoSek();
        basicTestConnectionDtlsCidLength(dtlsCidLength, serverDtlsCidLength);
    }
    protected void testPskDtlsCidLength(Integer dtlsCidLength, Integer serverDtlsCidLength) throws Exception {
        initDeviceCredentialsPsk();
        basicTestConnectionDtlsCidLength(dtlsCidLength, serverDtlsCidLength);
    }

    protected void basicTestConnectionDtlsCidLength(Integer clientDtlsCidLength,
                                                    Integer serverDtlsCidLength) throws Exception {
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);
        final Device device = createLwm2mDevice(deviceCredentials, clientEndpoint, deviceProfile.getId());
        createNewClient(security, null, true, clientEndpoint, clientDtlsCidLength, device.getId().getId().toString());
        lwM2MTestClient.start(true);

        awaitUpdateReg(1);
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS));
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccess));

        Configuration clientCoapConfig = ((CaliforniumClientEndpoint)((CaliforniumClientEndpointsProvider)lwM2MTestClient
                .getLeshanClient().getEndpointsProvider().toArray()[0]).getEndpoints().toArray()[0]).getCoapEndpoint().getConfig();
        Assert.assertEquals(clientDtlsCidLength, clientCoapConfig.get(DTLS_CONNECTION_ID_LENGTH));

        if (security.equals(SECURITY_NO_SEC)) {
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().isEmpty());
        } else {
            Assert.assertEquals(2L, lwM2MTestClient.getClientDtlsCid().size());
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().keySet().contains(ON_READ_CONNECTION_ID));
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().keySet().contains(ON_WRITE_CONNECTION_ID));
            if (serverDtlsCidLength == null) {
                Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_WRITE_CONNECTION_ID));
                Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
            } else {
                Assert.assertEquals(clientDtlsCidLength, lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
                if (clientDtlsCidLength == null) {
                    Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
                } else {
                    Assert.assertEquals(Integer.valueOf(serverDtlsCidLength), lwM2MTestClient.getClientDtlsCid().get(ON_WRITE_CONNECTION_ID));
                }
            }
        }
    }
}
