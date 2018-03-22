/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.extensions.sqs.plugin;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.sqs.action.fifo.SqsFifoQueuePluginAction;
import org.thingsboard.server.extensions.sqs.action.standard.SqsStandardQueuePluginAction;

/**
 * Created by Valerii Sosliuk on 11/6/2017.
 */
@Plugin(name = "SQS Plugin", actions = {SqsStandardQueuePluginAction.class, SqsFifoQueuePluginAction.class},
        descriptor = "SqsPluginDescriptor.json", configuration = SqsPluginConfiguration.class)
public class SqsPlugin extends AbstractPlugin<SqsPluginConfiguration> {

    private SqsMessageHandler sqsMessageHandler;
    private SqsPluginConfiguration configuration;

    @Override
    public void init(SqsPluginConfiguration configuration) {
        this.configuration = configuration;
        init();
    }

    private void init() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(configuration.getAccessKeyId(), configuration.getSecretAccessKey());
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(configuration.getRegion())).build();
        this.sqsMessageHandler = new SqsMessageHandler(sqs);

    }

    private void destroy() {
        this.sqsMessageHandler = null;
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return sqsMessageHandler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }
}
