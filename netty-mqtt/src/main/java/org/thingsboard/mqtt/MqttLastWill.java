/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;

@SuppressWarnings({"WeakerAccess", "unused", "SimplifiableIfStatement", "StringBufferReplaceableByString"})
public final class MqttLastWill {

    private final String topic;
    private final String message;
    private final boolean retain;
    private final MqttQoS qos;

    public MqttLastWill(String topic, String message, boolean retain, MqttQoS qos) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        if(message == null){
            throw new NullPointerException("message");
        }
        if(qos == null){
            throw new NullPointerException("qos");
        }
        this.topic = topic;
        this.message = message;
        this.retain = retain;
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetain() {
        return retain;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public static MqttLastWill.Builder builder(){
        return new MqttLastWill.Builder();
    }

    public static final class Builder {

        private String topic;
        private String message;
        private boolean retain;
        private MqttQoS qos;

        public String getTopic() {
            return topic;
        }

        public Builder setTopic(String topic) {
            if(topic == null){
                throw new NullPointerException("topic");
            }
            this.topic = topic;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Builder setMessage(String message) {
            if(message == null){
                throw new NullPointerException("message");
            }
            this.message = message;
            return this;
        }

        public boolean isRetain() {
            return retain;
        }

        public Builder setRetain(boolean retain) {
            this.retain = retain;
            return this;
        }

        public MqttQoS getQos() {
            return qos;
        }

        public Builder setQos(MqttQoS qos) {
            if(qos == null){
                throw new NullPointerException("qos");
            }
            this.qos = qos;
            return this;
        }

        public MqttLastWill build(){
            return new MqttLastWill(topic, message, retain, qos);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MqttLastWill that = (MqttLastWill) o;

        if (retain != that.retain) return false;
        if (!topic.equals(that.topic)) return false;
        if (!message.equals(that.message)) return false;
        return qos == that.qos;

    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (retain ? 1 : 0);
        result = 31 * result + qos.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttLastWill{");
        sb.append("topic='").append(topic).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", retain=").append(retain);
        sb.append(", qos=").append(qos.name());
        sb.append('}');
        return sb.toString();
    }
}
