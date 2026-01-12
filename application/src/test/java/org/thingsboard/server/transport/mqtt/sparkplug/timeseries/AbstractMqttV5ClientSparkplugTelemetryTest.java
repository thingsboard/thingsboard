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
package org.thingsboard.server.transport.mqtt.sparkplug.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.TOPIC_ROOT_SPB_V_1_0;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTelemetryTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processClientWithCorrectAccessTokenPublishNBIRTH() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), listKeys));
                    return !finalFuture.get().get().isEmpty();
                });
        Assert.assertEquals(listKeys.size(), finalFuture.get().get().size());
    }

    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildPrimitiveSimple() throws Exception {
        List<String> listKeys = new ArrayList<>();
        clientWithCorrectNodeAccessTokenWithNDEATH();

        String messageTypeName = SparkplugMessageType.NDATA.name();

        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricValuePrimitiveTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDATA.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == (listTsKvEntry.size() + 1);
                });
        Assert.assertTrue("Actual tsKvEntries is not containsAll Expected tsKvEntries", containsIgnoreVersion(finalFuture.get().get(), listTsKvEntry));
    }

    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildArraysPrimitiveSimple() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();

        String messageTypeName = SparkplugMessageType.NDATA.name();
        List<String> listKeys = new ArrayList<>();
        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricValueArraysPrimitiveTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDATA.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == (listTsKvEntry.size() + 1);
                });
        Assert.assertTrue("Actual tsKvEntries is not containsAll Expected tsKvEntries", containsIgnoreVersion(finalFuture.get().get(), listTsKvEntry));
    }

    private static boolean containsIgnoreVersion(List<TsKvEntry> expected, List<TsKvEntry> actual) {
        for (TsKvEntry actualEntry : actual) {
            var found = expected.stream()
                    .filter(tsKv -> tsKv.getKey().equals(actualEntry.getKey()))
                    .filter(tsKv -> tsKv.getValue().equals(actualEntry.getValue()))
                    .filter(tsKv -> tsKv.getTs() == actualEntry.getTs())
                    .findFirst();
            if (found.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
