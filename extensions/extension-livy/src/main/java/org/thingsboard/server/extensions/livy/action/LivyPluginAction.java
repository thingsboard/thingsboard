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
package org.thingsboard.server.extensions.livy.action;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.SparkApplication;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;

import java.util.Optional;

@Action(name = "Livy Plugin Action",
        descriptor = "LivyActionDescriptor.json", configuration = LivyPluginActionConfiguration.class)
@Slf4j
public class LivyPluginAction extends SimpleRuleLifecycleComponent implements PluginAction<LivyPluginActionConfiguration> {

    private LivyPluginActionConfiguration configuration;

    @Override
    public void init(LivyPluginActionConfiguration configuration) {
        this.configuration = configuration;
    }

    //TODO: Externalize Spark job to Jar and Version mapping
    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg msg, RuleProcessingMetaData deviceMsgMd) {
        //TODO: Transform message depending upon spark job to create LivyActionMessage with Payload
        SparkApplication application = SparkApplication.valueOf(configuration.getSparkApplication());
        LivyActionPayload.LivyActionPayloadBuilder builder = LivyActionPayload.builder();
        builder.sparkApplication(application);
        builder.actionPath(configuration.getActionPath());
        switch (application){
            case ROLLING_AVERAGE:
                String livyRequest = buildLivyRequest("rolling-average-0.0.1-SNAPSHOT",
                        "com.hashmap.tempus.app.rollingaverage.StreamData", application);
                builder.msgBody(livyRequest);
                return Optional.of(new LivyActionMessage(msg.getTenantId(),
                        msg.getCustomerId(), msg.getDeviceId(), builder.build()));
            case BACKFILL:
            case ARIMA:
            default: return Optional.empty();
        }
    }

    private String buildLivyRequest(String sparkJob, String className, SparkApplication application) {
        LivyRequest.LivyRequestBuilder builder = LivyRequest.builder();
        builder.jars(jars());
        builder.file("/user/pdarshini/" + sparkJob + ".jar");
        builder.className(className);
        builder.executoCores(4);
        builder.executorMemory("4g");
        builder.driverCores(4);
        builder.driverMemory("4g");
        builder.queue("default");
        builder.args(args(application));
        LivyRequest livyRequest = builder.build();
        Gson gson = new Gson();
        return gson.toJson(livyRequest);
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> response) {
        if (response instanceof ResponsePluginToRuleMsg) {
            return Optional.of(((ResponsePluginToRuleMsg) response).getPayload());
        }
        return Optional.empty();
    }

    @Override
    public boolean isOneWayAction() {
        return true;
    }

    private String[] jars() {
        return new String[]{"/user/pdarshini/datanucleus-api-jdo-3.2.6.jar",
                "/user/pdarshini/datanucleus-rdbms-3.2.9.jar",
                "/user/pdarshini/datanucleus-core-3.2.10.jar",
                "/user/pdarshini/etp-lib-0.0.1-SNAPSHOT.jar",
                "/user/pdarshini/phoenix-core-4.7.0.2.6.0.3-8.jar",
                "/user/pdarshini/hbase-common-1.1.2.2.6.0.3-8.jar",
                "/user/pdarshini/hbase-client-1.1.2.2.6.0.3-8.jar",
                "/user/pdarshini/hbase-protocol-1.1.2.2.6.0.3-8.jar",
                "/user/pdarshini/hbase-server-1.1.2.2.6.0.3-8.jar",
                "/user/pdarshini/phoenix-spark-4.7.0-HBase-1.1.jar",
                "/user/pdarshini/metrics-core-2.2.0.jar",
                "/user/pdarshini/spark-streaming-kafka_2.10-1.6.1.2.4.2.0-258.jar",
                "/user/pdarshini/kafka_2.10-0.9.0.2.4.2.0-258.jar",
                "/user/pdarshini/kafka-clients-0.9.0.2.4.2.0-258.jar"};
    }

    private String[] args(SparkApplication application) {
        switch (application){
            case ROLLING_AVERAGE:
                return new String[]{configuration.getKafkaBrokers(),
                        configuration.getTopic(), configuration.getZkUrl(),
                        configuration.getGatewayApiToken(), configuration.getEndpoint(),
                        Long.toString(configuration.getWindow()), Double.toString(configuration.getVolume())};
            case BACKFILL:
            case ARIMA:
            default: return null;
        }
    }
}
