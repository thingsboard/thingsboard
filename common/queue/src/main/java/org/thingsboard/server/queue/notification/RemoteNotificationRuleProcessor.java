/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.queue.notification;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.settings.TriggerTypeConfig;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.notification.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.SOFT;

@Service
@ConditionalOnMissingBean(value = NotificationRuleProcessor.class, ignored = RemoteNotificationRuleProcessor.class)
@ConfigurationProperties(prefix = "notification-system.rules")
@RequiredArgsConstructor
@Slf4j
public class RemoteNotificationRuleProcessor implements NotificationRuleProcessor {

    private final TbQueueProducerProvider producerProvider;
    private final NotificationsTopicService notificationsTopicService;
    private final PartitionService partitionService;
    private final DataDecodingEncodingService encodingService;

    private Map<NotificationRuleTriggerType, TriggerTypeConfig> triggerTypesConfigs;
    private final ConcurrentMap<String, Long> submittedTriggers = new ConcurrentReferenceHashMap<>(16, SOFT);

    @Override
    public void process(NotificationRuleTrigger trigger) {
        if (trigger.deduplicate() && alreadySubmitted(trigger)) {
            return;
        }
        try {
            log.debug("Submitting notification rule trigger: {}", trigger);
            TransportProtos.NotificationRuleProcessorMsg.Builder msg = TransportProtos.NotificationRuleProcessorMsg.newBuilder()
                    .setTrigger(ByteString.copyFrom(encodingService.encode(trigger)));

            partitionService.getAllServiceIds(ServiceType.TB_CORE).stream().findAny().ifPresent(serviceId -> {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(),
                        TransportProtos.ToCoreNotificationMsg.newBuilder()
                                .setNotificationRuleProcessorMsg(msg)
                                .build()), null);
            });
        } catch (Throwable e) {
            log.error("Failed to submit notification rule trigger: {}", trigger, e);
        }
    }

    private boolean alreadySubmitted(NotificationRuleTrigger trigger) {
        String deduplicationKey = trigger.getDeduplicationKey();

        AtomicBoolean alreadySubmitted = new AtomicBoolean(false);
        submittedTriggers.compute(deduplicationKey, (key, lastSubmittedTs) -> {
            long currentTs = System.currentTimeMillis();
            if (lastSubmittedTs == null) {
                return currentTs;
            } else {
                long deduplicationDuration = getDeduplicationDuration(trigger);
                long passed = currentTs - lastSubmittedTs;
                if (deduplicationDuration == 0 || passed <= deduplicationDuration) {
                    log.trace("Notification rule trigger {} was already submitted {} ms ago, deduplication duration is {} ms. Key: '{}'",
                            trigger.getType(), passed, deduplicationDuration, deduplicationKey);
                    alreadySubmitted.set(true);
                    return lastSubmittedTs;
                } else {
                    return currentTs;
                }
            }
        });
        return alreadySubmitted.get();
    }

    private long getDeduplicationDuration(NotificationRuleTrigger trigger) {
        if (triggerTypesConfigs == null) {
            triggerTypesConfigs = new EnumMap<>(NotificationRuleTriggerType.class);
        }
        TriggerTypeConfig triggerTypeConfig = triggerTypesConfigs.computeIfAbsent(trigger.getType(), triggerType -> {
            TriggerTypeConfig config = new TriggerTypeConfig();
            config.setDeduplicationDuration(trigger.getDefaultDeduplicationDuration());
            return config;
        });
        return triggerTypeConfig.getDeduplicationDuration();
    }

    // set from ConfigurationProperties
    public void setTriggerTypesConfigs(Map<NotificationRuleTriggerType, TriggerTypeConfig> triggerTypesConfigs) {
        if (triggerTypesConfigs != null) {
            this.triggerTypesConfigs = new EnumMap<>(triggerTypesConfigs);
        }
    }

}
