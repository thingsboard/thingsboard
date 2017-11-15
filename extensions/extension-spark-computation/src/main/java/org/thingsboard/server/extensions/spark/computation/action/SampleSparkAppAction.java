/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.spark.computation.action;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.component.Action;

@Action(name = "Sample Spark Computation Action",
        descriptor = "SampleSparkApplicationDescriptor.json", configuration = SampleSparkAppConfiguration.class)
@Slf4j
public class SampleSparkAppAction extends AbstractSparkAppAction<SampleSparkAppConfiguration> {

    @Override
    protected String buildSparkComputationRequest() {
        SparkComputationRequest.SparkComputationRequestBuilder builder = SparkComputationRequest.builder();
        builder.file("/usr/livy-server-0.3.0/upload/spark-kafka-streaming-integration-1.0.0.jar");
        builder.className("org.thingsboard.samples.spark.SparkKafkaStreamingDemoMain");
        builder.args(args());
        SparkComputationRequest sparkComputationRequest = builder.build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        String msgBody;
        try {
            msgBody = mapper.writeValueAsString(sparkComputationRequest);
        } catch (JsonProcessingException e) {
            return "{}";
        }
        return msgBody;
    }

    @Override
    protected String[] args() {
        return new String[]{"--topic", configuration.getTopic(), "--window", Long.toString(configuration.getWindow()),
                "--mqttbroker", configuration.getEndpoint(), "--kafka", configuration.getKafkaBrokers(),
                "--token", configuration.getGatewayApiToken()};
    }
}
