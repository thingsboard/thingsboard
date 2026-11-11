// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.gcloud.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.ConvertUtil;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PubSubIntegration extends AbstractIntegration<PubSubIntegrationMsg> {

    private IntegrationContext context;
    private Subscriber subscriber;
    private volatile boolean stopped;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        stopped = false;
        this.context = params.getContext();
        PubSubIntegrationConfiguration pubSubConfiguration = getClientConfiguration(configuration, PubSubIntegrationConfiguration.class);
        ServiceAccountCredentials credentials =
                ServiceAccountCredentials.fromStream(new ByteArrayInputStream(pubSubConfiguration.getServiceAccountKey().getBytes()));
        CredentialsProvider credProvider = FixedCredentialsProvider.create(credentials);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(pubSubConfiguration.getProjectId(), pubSubConfiguration.getSubscriptionId());
        subscriber = Subscriber.newBuilder(subscriptionName, (MessageReceiver) (pubsubMessage, ackReplyConsumer) -> {
            Map<String, String> metadata = new HashMap<>(metadataTemplate.getKvMap());
            metadata.putAll(pubsubMessage.getAttributesMap());
            metadata.put("pubSubMsgId", pubsubMessage.getMessageId());
            process(new PubSubIntegrationMsg(pubsubMessage.getData().toByteArray(), metadata));
            ackReplyConsumer.ack();
        }).setCredentialsProvider(credProvider).build();
        subscriber.startAsync().awaitRunning();
    }

    @Override
    public void process(PubSubIntegrationMsg msg) {
        if (stopped) {
            return;
        }
        String status = "OK";
        Exception exception = null;
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData<>(getDefaultUplinkContentType(), msg.getDeviceMetadata()));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkDataBlocking(context, data);
                    log.trace("[{}] Processing uplink data", data);
                }
            }
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            exception = e;
            status = "ERROR";
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(),
                () -> ConvertUtil.toDebugMessage(getDefaultUplinkContentType(), msg.getPayload()), status, exception);
    }

    @Override
    public void destroy() {
        stopped = true;
        if (subscriber != null) {
            subscriber.stopAsync();
        }
    }

}
