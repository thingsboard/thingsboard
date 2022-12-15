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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by nickAS21 on 13.12.22
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SparkplugBPayload {

    private Date timestamp;
    private List<Metric> metrics;
    private long seq = -1;
    private String uuid;
    private byte[] body;

    public SparkplugBPayload() {};

    public SparkplugBPayload(Date timestamp, List<Metric> metrics, long seq, String uuid, byte[] body) {
        this.timestamp = timestamp;
        this.metrics = metrics;
        this.seq = seq;
        this.uuid = uuid;
        this.body = body;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void addMetric(Metric metric) {
        metrics.add(metric);
    }

    public void addMetric(int index, Metric metric) {
        metrics.add(index, metric);
    }

    public void addMetrics(List<Metric> metrics) {
        this.metrics.addAll(metrics);
    }

    public Metric removeMetric(int index) {
        return metrics.remove(index);
    }

    public boolean removeMetric(Metric metric) {
        return metrics.remove(metric);
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    @JsonIgnore
    public Integer getMetricCount() {
        return metrics.size();
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "SparkplugBPayload [timestamp=" + timestamp + ", metrics=" + metrics + ", seq=" + seq + ", uuid=" + uuid
                + ", body=" + Arrays.toString(body) + "]";
    }

    /**
     * A builder for creating a {@link SparkplugBPayload} instance.
     */
    public static class SparkplugBPayloadBuilder {

        private Date timestamp;
        private List<Metric> metrics;
        private long seq = -1;
        private String uuid;
        private byte[] body;

        public SparkplugBPayloadBuilder(long sequenceNumber) {
            this.seq = sequenceNumber;
            metrics = new ArrayList<Metric>();
        }

        public SparkplugBPayloadBuilder() {
            metrics = new ArrayList<Metric>();
        }

        public SparkplugBPayloadBuilder addMetric(Metric metric) {
            this.metrics.add(metric);
            return this;
        }

        public SparkplugBPayloadBuilder addMetrics(Collection<Metric> metrics) {
            this.metrics.addAll(metrics);
            return this;
        }

        public SparkplugBPayloadBuilder setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SparkplugBPayloadBuilder setSeq(long seq) {
            this.seq = seq;
            return this;
        }

        public SparkplugBPayloadBuilder setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public SparkplugBPayloadBuilder setBody(byte[] body) {
            this.body = body;
            return this;
        }

        public SparkplugBPayload createPayload() {
            return new SparkplugBPayload(timestamp, metrics, seq, uuid, body);
        }
    }
}
