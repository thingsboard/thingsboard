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
package org.thingsboard.server.transport.mqtt.sparkplug;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTest extends AbstractMqttIntegrationTest {

    protected MqttV5TestClient client;
    protected Calendar calendar = Calendar.getInstance();

    protected static final String NAMESPACE = "spBv1.0";
    protected static final String groupId = "SparkplugBGroupId";
    protected static final String edgeNode = "SparkpluBNode";
    protected static final String keysBdSeq = "bdSeq";
    protected static final String alias = "Failed Post Telemetry node proto payload. SparkplugMessageType ";
    protected String deviceId = "Test Sparkplug B Device";
    protected int bdSeq = 0;
    protected int seq = 0;
    protected static final long PUBLISH_TS_DELTA_MS = 86400000;// Publish start TS <-> 24h


    public void beforeSparkplugTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test Connect Sparkplug client node")
                .isSparkPlug(true)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    public void processClientWithCorrectNodeAccess() throws Exception {
        this.client = new MqttV5TestClient();
        MqttWireMessage response = clientWithCorrectNodeAccessToken(client);
        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());
        MqttConnAck connAckMsg = (MqttConnAck) response;
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
    }

    protected SparkplugBProto.Payload.Metric createMetric(Object value, TsKvEntry tsKvEntry, MetricDataType metricDataType) throws ThingsboardException {
        SparkplugBProto.Payload.Metric metric = SparkplugBProto.Payload.Metric.newBuilder()
                .setTimestamp(tsKvEntry.getTs())
                .setName(tsKvEntry.getKey())
                .setDatatype(metricDataType.toIntValue())
                .build();
        switch (metricDataType) {
            case Int8:
            case Int16:
            case UInt8:
            case UInt16:
                int valueMetric = Integer.valueOf(String.valueOf(value));
                return metric.toBuilder().setIntValue(valueMetric).build();
            case Int32:
            case UInt32:
                if (value instanceof Long) {
                    return metric.toBuilder().setLongValue((long) value).build();
                } else {
                    return metric.toBuilder().setIntValue((int)value).build();
                }
            case Int64:
            case UInt64:
            case DateTime:
                return metric.toBuilder().setLongValue((long) value).build();
            case Float:
                return metric.toBuilder().setFloatValue((float) value).build();
            case Double:
                return metric.toBuilder().setDoubleValue((double) value).build();
            case Boolean:
                return metric.toBuilder().setBooleanValue((boolean) value).build();
            case String:
            case Text:
            case UUID:
                return metric.toBuilder().setStringValue((String) value).build();
            case DataSet:
                return metric.toBuilder().setDatasetValue((SparkplugBProto.Payload.DataSet) value).build();
            case Bytes:
            case Int8Array:
                ByteString byteString = ByteString.copyFrom((byte[]) value);
                return metric.toBuilder().setBytesValue(byteString).build();
            case Int16Array:
            case UInt8Array:
                byte[] int16Array = shortToByte_ByteBuffer_Method((short[]) value);
                ByteString byteInt16Array = ByteString.copyFrom((int16Array));
                return metric.toBuilder().setBytesValue(byteInt16Array).build();
            case Int32Array:
            case UInt16Array:
                byte[] int32Array = integerToByte_ByteBuffer_Method((int[]) value);
                ByteString byteInt32Array = ByteString.copyFrom((int32Array));
                return metric.toBuilder().setBytesValue(byteInt32Array).build();
            case Int64Array:
            case UInt32Array:
                byte[] int64Array = longToByte_ByteBuffer_Method((long[]) value);
                ByteString byteInt64Array = ByteString.copyFrom((int64Array));
                return metric.toBuilder().setBytesValue(byteInt64Array).build();
            case UInt64Array:
            case DoubleArray:
                byte[] doubleArray = doubleToByte_ByteBuffer_Method((double[]) value);
                ByteString byteDoubleArray = ByteString.copyFrom(doubleArray);
                return metric.toBuilder().setBytesValue(byteDoubleArray).build();
            case FloatArray:
                byte[] floatArray = floatToByte_ByteBuffer_Method((float[]) value);
                ByteString byteFloatArray = ByteString.copyFrom(floatArray);
                return metric.toBuilder().setBytesValue(byteFloatArray).build();
            case BooleanArray:
                byte[] booleanArray = booleanToByte_ByteBuffer_Method((boolean[]) value);
                ByteString byteBooleanArray = ByteString.copyFrom(booleanArray);
                return metric.toBuilder().setBytesValue(byteBooleanArray).build();
            case StringArray:
                byte[] stringArray = stringToByte_ByteBuffer_Method((String[]) value);
                ByteString byteStringArray = ByteString.copyFrom(stringArray);
                return metric.toBuilder().setBytesValue(byteStringArray).build();
            case DateTimeArray:
                byte[] dateArray = dateToByte_ByteBuffer_Method((Date[]) value);
                ByteString byteDateArray = ByteString.copyFrom(dateArray);
                return metric.toBuilder().setBytesValue(byteDateArray).build();
            case File:
                SparkplugMetricUtil.File file = (SparkplugMetricUtil.File) value;
                ByteString byteFileString = ByteString.copyFrom(file.getBytes());
                return metric.toBuilder().setBytesValue(byteFileString).build();
            case Template:
                return metric.toBuilder().setTemplateValue((SparkplugBProto.Payload.Template) value).build();
            case Unknown:
                throw new ThingsboardException("Invalid value for MetricDataType " + metricDataType.name(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return metric;
    }

    private byte[] shortToByte_ByteBuffer_Method(short[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 2);
        for (short d : inputs) {
            bb.putShort(d);
        }
        return bb.array();
    }

    private byte[] integerToByte_ByteBuffer_Method(int[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 4);
        for (int d : inputs) {
            bb.putLong(d);
        }
        return bb.array();
    }

    private byte[] longToByte_ByteBuffer_Method(long[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 8);
        for (long d : inputs) {
            bb.putLong(d);
        }
        return bb.array();
    }

    private byte[] doubleToByte_ByteBuffer_Method(double[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 8);
        for (double d : inputs) {
            bb.putDouble(d);
        }
        return bb.array();
    }

    private byte[] floatToByte_ByteBuffer_Method(float[] inputs) throws ThingsboardException {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        for (float f : inputs) {
            try {
                ds.writeFloat(f);
            } catch (IOException e) {
                throw new ThingsboardException("Invalid value float ", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        }
        return bas.toByteArray();
    }

    private byte[] booleanToByte_ByteBuffer_Method(boolean[] inputs) {
        byte[] toReturn = new byte[inputs.length / 8];
        for (int entry = 0; entry < toReturn.length; entry++) {
            for (int bit = 0; bit < 8; bit++) {
                if (inputs[entry * 8 + bit]) {
                    toReturn[entry] |= (128 >> bit);
                }
            }
        }
        return toReturn;
    }

    private byte[] stringToByte_ByteBuffer_Method(String[] inputs) throws ThingsboardException {
        final ByteArrayOutputStream bas = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream os = new ObjectOutputStream(bas);
            os.writeObject(inputs);
            os.flush();
            os.close();
        } catch (Exception e) {
            throw new ThingsboardException("Invalid value float ", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return bas.toByteArray();
    }

    private byte[] dateToByte_ByteBuffer_Method(Date[] inputs) {
        long[] ll = new long[inputs.length];
        int i = 0;
        for (Date date : inputs) {
            ll[i] = date.getTime();
            i++;
        }
        return longToByte_ByteBuffer_Method(ll);
    }


    private MqttWireMessage clientWithCorrectNodeAccessToken(MqttV5TestClient client) throws Exception {
        IMqttToken connectionResult = client.connectAndWait(gatewayAccessToken);
        return connectionResult.getResponse();
    }

    protected long getBdSeqNum() throws Exception {
        if (bdSeq == 256) {
            bdSeq = 0;
        }
        return bdSeq++;
    }

    protected long getSeqNum() throws Exception {
        if (seq == 256) {
            seq = 0;
        }
        return seq++;
    }

}
