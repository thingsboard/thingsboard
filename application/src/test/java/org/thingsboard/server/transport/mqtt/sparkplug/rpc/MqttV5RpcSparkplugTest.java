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
package org.thingsboard.server.transport.mqtt.sparkplug.rpc;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

@DaoSqlTest
@Slf4j
public class MqttV5RpcSparkplugTest  extends AbstractMqttV5RpcSparkplugTest {

    @Before
    public void beforeTest() throws Exception {
        beforeSparkplugTest();
    }

    @After
    public void afterTest() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success();
    }

    @Test
    public void testClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success();
    }
    @Test
    public void testClientDeviceWithCorrectAccessTokenPublishWithAlias_TwoWayRpc_Success() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublishWithAlias_TwoWayRpc_Success();
    }

    @Test
    public void testClientNodeWithCorrectAccessTokenPublishWithAliasWithoutMetricName_TwoWayRpc_BAD_REQUEST_PARAMS() throws Exception {
        processClientNodeWithCorrectAccessTokenPublishWithAliasWithoutMetricName_TwoWayRpc_BAD_REQUEST_PARAMS();
    }

    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS();
    }

    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InBirthNotHaveMetric_BAD_REQUEST_PARAMS() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS();
    }

}
