/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;

import com.google.protobuf.Descriptors;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttProtoDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.util.MqttTopicFilter;
import org.thingsboard.server.transport.mqtt.util.MqttTopicFilterFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DeviceSessionCtx extends MqttDeviceAwareSessionContext {

    @Getter
    private ChannelHandlerContext channel;

    @Getter
    private MqttTransportContext context;

    private final AtomicInteger msgIdSeq = new AtomicInteger(0);

    @Getter
    @Setter
    private boolean provisionOnly = false;

    private volatile MqttTopicFilter telemetryTopicFilter = MqttTopicFilterFactory.getDefaultTelemetryFilter();
    private volatile MqttTopicFilter attributesTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
    private volatile TransportPayloadType payloadType = TransportPayloadType.JSON;
    private volatile Descriptors.Descriptor attributesDynamicMessageDescriptor;
    private volatile Descriptors.Descriptor telemetryDynamicMessageDescriptor;

    @Getter
    @Setter
    private TransportPayloadType provisionPayloadType = payloadType;

    public DeviceSessionCtx(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap, MqttTransportContext context) {
        super(sessionId, mqttQoSMap);
        this.context = context;
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    public boolean isDeviceTelemetryTopic(String topicName) {
        return telemetryTopicFilter.filter(topicName);
    }

    public boolean isDeviceAttributesTopic(String topicName) {
        return attributesTopicFilter.filter(topicName);
    }

    public MqttTransportAdaptor getPayloadAdaptor() {
        return payloadType.equals(TransportPayloadType.JSON) ? context.getJsonMqttAdaptor() : context.getProtoMqttAdaptor();
    }

    public boolean isJsonPayloadType() {
        return payloadType.equals(TransportPayloadType.JSON);
    }

    public Descriptors.Descriptor getTelemetryDynamicMsgDescriptor() {
        return telemetryDynamicMessageDescriptor;
    }

    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor() {
        return attributesDynamicMessageDescriptor;
    }

    @Override
    public void setDeviceProfile(DeviceProfile deviceProfile) {
        super.setDeviceProfile(deviceProfile);
        updateTopicFilters(deviceProfile);
    }

    @Override
    public void onProfileUpdate(DeviceProfile deviceProfile) {
        super.onProfileUpdate(deviceProfile);
        updateTopicFilters(deviceProfile);
    }


    private void updateTopicFilters(DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.MQTT) &&
                transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            MqttDeviceProfileTransportConfiguration mqttConfig = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            payloadType = mqttConfig.getTransportPayloadType();
            telemetryTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceTelemetryTopic());
            attributesTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceAttributesTopic());
            if (payloadType.equals(TransportPayloadType.PROTOBUF) && mqttConfig instanceof MqttProtoDeviceProfileTransportConfiguration) {
                updateDynamicMessageDescriptors(mqttConfig);
            }
        } else {
            telemetryTopicFilter = MqttTopicFilterFactory.getDefaultTelemetryFilter();
            attributesTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
        }
    }

    private void updateDynamicMessageDescriptors(MqttDeviceProfileTransportConfiguration mqttConfig) {
        MqttProtoDeviceProfileTransportConfiguration protoMqttConfig = (MqttProtoDeviceProfileTransportConfiguration) mqttConfig;
        telemetryDynamicMessageDescriptor = protoMqttConfig.getDynamicMessageDescriptor(protoMqttConfig.getDeviceTelemetryProtoSchema(), "telemetrySchema");
        attributesDynamicMessageDescriptor = protoMqttConfig.getDynamicMessageDescriptor(protoMqttConfig.getDeviceAttributesProtoSchema(), "attributesSchema");
    }
}
