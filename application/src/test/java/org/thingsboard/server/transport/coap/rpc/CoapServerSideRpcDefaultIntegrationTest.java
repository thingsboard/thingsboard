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
package org.thingsboard.server.transport.coap.rpc;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class CoapServerSideRpcDefaultIntegrationTest extends AbstractCoapServerSideRpcIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("RPC test device")
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testServerCoapOneWayRpcDeviceOffline() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"24\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().is(504),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerCoapOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"25\",\"value\": 1}}";
        String nonExistentDeviceId = Uuids.timeBased().toString();

        String result = doPostAsync("/api/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerCoapTwoWayRpcDeviceOffline() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"27\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().is(504),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerCoapTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"28\",\"value\": 1}}";
        String nonExistentDeviceId = Uuids.timeBased().toString();

        String result = doPostAsync("/api/rpc/twoway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerCoapOneWayRpc() throws Exception {
        processOneWayRpcTest(false);
    }

    @Test
    public void testServerCoapTwoWayRpc() throws Exception {
        processTwoWayRpcTest("{\"value1\":\"A\",\"value2\":\"B\"}", false);
    }

}
