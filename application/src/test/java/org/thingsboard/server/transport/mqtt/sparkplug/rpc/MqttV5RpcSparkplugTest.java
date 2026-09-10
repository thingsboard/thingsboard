// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
