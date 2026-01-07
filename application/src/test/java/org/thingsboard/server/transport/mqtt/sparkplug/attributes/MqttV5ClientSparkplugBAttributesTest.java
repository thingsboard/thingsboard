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
package org.thingsboard.server.transport.mqtt.sparkplug.attributes;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

/**
 * Created by nickAS21 on 12.01.23
 */
@DaoSqlTest
public class MqttV5ClientSparkplugBAttributesTest extends AbstractMqttV5ClientSparkplugAttributesTest {

    @Before
    public void beforeTest() throws Exception {
        beforeSparkplugTest();
    }

    @After
    public void afterTest () throws MqttException {
        if (client.isConnected()) {
            client.disconnect();        }
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNCMDReBirth() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMDReBirth();
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_BooleanType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_BooleanType_IfMetricFailedTypeCheck_SendValueOk();
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_LongType_IfMetricFailedTypeCheck_SendValueOk();
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_FloatType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_FloatType_IfMetricFailedTypeCheck_SendValueOk();
    }
    @Test
    public void testClientWithCorrectAccessTokenPublishMetricDataTypeFromJson_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishMetricDataTypeFromJson_SendValueOk();
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_DoubleType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_DoubleType_IfMetricFailedTypeCheck_SendValueOk();
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_StringType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_StringType_IfMetricFailedTypeCheck_SendValueOk();
    }

    @Test
    public void testClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttribute() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttribute();
    }

    @Test
    public void testClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttributes_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttributes_LongType_IfMetricFailedTypeCheck_SendValueOk();
    }

}
