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
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.InMemoryReadWriteLockConnectionStore;
import org.eclipse.californium.scandium.dtls.ResumptionSupportingConnectionStore;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpoint;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.peer.IpPeer;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
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

    protected void testNoSecDtlsCidLength(Integer clientDtlsCidLength, Integer serverDtlsCidLength) throws Exception {
        initDeviceCredentialsNoSek();
        basicTestConnectionDtlsCidLength(clientDtlsCidLength, serverDtlsCidLength);
    }
    protected void testPskDtlsCidLength(Integer clientDtlsCidLength, Integer serverDtlsCidLength) throws Exception {
        initDeviceCredentialsPsk();
        basicTestConnectionDtlsCidLength(clientDtlsCidLength, serverDtlsCidLength);
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
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().containsKey(ON_READ_CONNECTION_ID));
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().containsKey(ON_WRITE_CONNECTION_ID));

            LwM2mServer lwM2mServer = lwM2MTestClient.getLeshanClient().getRegisteredServers().entrySet().stream().findFirst().get().getValue();
            CaliforniumClientEndpoint  lwM2mClientEndpoint  = (CaliforniumClientEndpoint) lwM2MTestClient.getLeshanClient().getEndpoint(lwM2mServer);
            Connection connection = getConnection(lwM2mClientEndpoint, lwM2mServer);
            ConnectionId clientCid = connection.getConnectionId();
            ConnectionId readCid = connection.getEstablishedDtlsContext().getReadConnectionId();
            ConnectionId serverCid = connection.getEstablishedDtlsContext().getWriteConnectionId();
            if (serverDtlsCidLength == null || clientDtlsCidLength == null) {
                // cid is not used
                Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_WRITE_CONNECTION_ID));
                Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
                Assert.assertNull(readCid);
                Assert.assertNull(serverCid);
            } else {
                Assert.assertEquals(serverDtlsCidLength, lwM2MTestClient.getClientDtlsCid().get(ON_WRITE_CONNECTION_ID));
                Assert.assertEquals(clientDtlsCidLength, lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
                // cid used
                Assert.assertNotNull(clientCid);
                Assert.assertNotNull(readCid);
                if (clientDtlsCidLength > 0) {
                    Assert.assertEquals(clientCid, readCid);
                }
                Assert.assertNotNull(serverCid);
                int actualServerCidLength = serverCid.getBytes().length;
                int expectedServerCidLength = serverDtlsCidLength;
                Assert.assertEquals(expectedServerCidLength, actualServerCidLength);
            }

            if (clientCid != null) {
                int actualClientCidLength = clientCid.getBytes().length;
                int expectedClientCidLength;
                if (clientDtlsCidLength == null || clientDtlsCidLength == 0) {
                    expectedClientCidLength = 3;
                } else {
                    expectedClientCidLength = clientDtlsCidLength;
                }
                Assert.assertEquals(expectedClientCidLength, actualClientCidLength);
            }
        }
    }

    private static Connection getConnection(CaliforniumClientEndpoint lwM2mClientEndpoint, LwM2mServer lwM2mServer) throws NoSuchFieldException, IllegalAccessException {
        DTLSConnector connector = (DTLSConnector) lwM2mClientEndpoint.getCoapEndpoint().getConnector();
        Field field = DTLSConnector.class.getDeclaredField("connectionStore");
        field.setAccessible(true);
        ResumptionSupportingConnectionStore connectionStore = (InMemoryReadWriteLockConnectionStore) field.get(connector);
        InetSocketAddress serverAddr = ((IpPeer) lwM2mServer.getTransportData()).getSocketAddress();
        return connectionStore.get(serverAddr);
    }
}
