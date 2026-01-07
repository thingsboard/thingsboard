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
package org.thingsboard.server.transport.lwm2m.security.diffPort;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.SocketIdentity;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;

@DaoSqlTest
@Slf4j
public abstract class AbstractLwM2MIntegrationDiffPortTest extends AbstractSecurityLwM2MIntegrationTest {

    @SpyBean
    private RegistrationStore registrationStoreTest;

    protected void basicTestConnectionDifferentPort(Lwm2mDeviceProfileTransportConfiguration transportConfiguration,
                                                    String awaitAlias) throws Exception {

        doAnswer((invocation) -> {
            Object[] arguments = invocation.getArguments();
            log.trace("doAnswer for registrationStoreTest.updateRegistration with args {}", arguments);
            int portOld = ((RegistrationUpdate) arguments[0]).getPort();
            int portValueChange = 5;
            arguments[0] = registrationUpdateNewPort((RegistrationUpdate) arguments[0], portValueChange);
            int portNew =  ((RegistrationUpdate) arguments[0]).getPort();
            Assert.assertEquals((portNew - portOld), portValueChange);
            return invocation.callRealMethod();
        }).when(registrationStoreTest).updateRegistration(any(RegistrationUpdate.class));

        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);
        final Device device = createLwm2mDevice(deviceCredentials, clientEndpoint, deviceProfile.getId());
        createNewClient(security, null, true, clientEndpoint, device.getId().getId().toString());
        lwM2MTestClient.start(true);

        verify(defaultUplinkMsgHandlerTest, timeout(50000).atLeast(1))
                .onRegistered(Mockito.any(Registration.class), Mockito.any());
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED));
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccess));

        awaitUpdateReg(1);
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS));

        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccessUpdate));

        long cntBefore = countUpdateReg();
        awaitUpdateReg((int) (cntBefore + 1));
    }

    private RegistrationUpdate registrationUpdateNewPort (RegistrationUpdate update, int portValueChange) {
        Integer portOld = update.getPort();
        Integer portNew = portOld + portValueChange;
        log.warn("portOld: [{}], portNew: [{}]", portOld, portNew);
        InetAddress addressOld = update.getAddress();
        InetSocketAddress socketAddressUpdate = new InetSocketAddress(addressOld, portNew);
        SocketIdentity socketIdentity = new SocketIdentity(socketAddressUpdate);
        LwM2mPeer sender = new IpPeer(new InetSocketAddress(addressOld, portNew), socketIdentity);
        return new RegistrationUpdate(update.getRegistrationId(), sender,
                update.getLifeTimeInSec(), update.getSmsNumber(), update.getBindingMode(),
                update.getObjectLinks(), update.getAlternatePath(),
                update.getSupportedContentFormats(), update.getSupportedObjects(),
                update.getAvailableInstances(), update.getAdditionalAttributes(),
                update.getApplicationData());
    }
}
