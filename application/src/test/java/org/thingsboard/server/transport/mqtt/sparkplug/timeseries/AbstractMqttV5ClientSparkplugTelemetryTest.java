/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.sparkplug.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int64;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public  abstract class AbstractMqttV5ClientSparkplugTelemetryTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processPushTelemetry() throws Exception {
        processClientWithCorrectNodeAccess();

    }

    protected void processClientWithCorrectAccessTokenCreatedPublishBirthNode() throws Exception {
        processClientWithCorrectNodeAccess();
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());

        long ts = calendar.getTimeInMillis()-PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        MetricDataType metricDataType = Int64;
        TsKvEntry tsKvEntryBdSecOriginal = new BasicTsKvEntry(ts, new LongDataEntry(keysBdSeq, valueBdSec));
        payloadBirthNode.addMetrics(createMetric(tsKvEntryBdSecOriginal, metricDataType));

        String keys  = "Node Control/Rebirth";
        boolean valueRebirth = false;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryRebirth = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, valueRebirth));
        payloadBirthNode.addMetrics(createMetric(expectedSsKvEntryRebirth , metricDataType));

        keys  = "Node Metric int32";
        int valueNodeInt32 = 1024;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryNodeInt32 = new BasicTsKvEntry(ts, new LongDataEntry(keys, Integer.toUnsignedLong(valueNodeInt32)));
        payloadBirthNode.addMetrics(createMetric(expectedSsKvEntryNodeInt32 , metricDataType));

        client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                payloadBirthNode.build().toByteArray(), 0, false);




    }

}
