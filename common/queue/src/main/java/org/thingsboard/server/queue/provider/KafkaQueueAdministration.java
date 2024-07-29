/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.queue.provider;

import java.util.concurrent.atomic.AtomicLong;

import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

public class KafkaQueueAdministration {

    private TopicService topicService;

    private TbKafkaSettings kafkaSettings;

    private TbServiceInfoProvider serviceInfoProvider;

    private TbQueueCoreSettings coreSettings;

    private TbQueueRuleEngineSettings ruleEngineSettings;

    private TbQueueTransportApiSettings transportApiSettings;

    private TbQueueTransportNotificationSettings transportNotificationSettings;

    private TbQueueRemoteJsInvokeSettings jsInvokeSettings;

    private TbQueueVersionControlSettings vcSettings;

    private TbKafkaConsumerStatsService consumerStatsService;

    private TbQueueAdmin coreAdmin;

    private TbQueueAdmin jsExecutorRequestAdmin;

    private TbQueueAdmin jsExecutorResponseAdmin;

    private TbQueueAdmin transportApiRequestAdmin;

    private TbQueueAdmin transportApiResponseAdmin;

    private TbQueueAdmin notificationAdmin;

    private TbQueueAdmin fwUpdatesAdmin;

    private TbQueueAdmin vcAdmin;

    private TbQueueAdmin housekeeperAdmin;

    private TbQueueAdmin housekeeperReprocessingAdmin;

    private AtomicLong consumerCount;

    public TopicService getTopicService() {
        return topicService;
    }

    public TbKafkaSettings getKafkaSettings() {
        return kafkaSettings;
    }

    public TbServiceInfoProvider getServiceInfoProvider() {
        return serviceInfoProvider;
    }

    public TbQueueCoreSettings getCoreSettings() {
        return coreSettings;
    }

    public TbQueueRuleEngineSettings getRuleEngineSettings() {
        return ruleEngineSettings;
    }

    public TbQueueTransportApiSettings getTransportApiSettings() {
        return transportApiSettings;
    }

    public TbQueueTransportNotificationSettings getTransportNotificationSettings() {
        return transportNotificationSettings;
    }

    public TbQueueRemoteJsInvokeSettings getJsInvokeSettings() {
        return jsInvokeSettings;
    }

    public TbQueueVersionControlSettings getVcSettings() {
        return vcSettings;
    }

    public TbKafkaConsumerStatsService getConsumerStatsService() {
        return consumerStatsService;
    }

    public TbQueueAdmin getCoreAdmin() {
        return coreAdmin;
    }

    public TbQueueAdmin getJsExecutorRequestAdmin() {
        return jsExecutorRequestAdmin;
    }

    public TbQueueAdmin getJsExecutorResponseAdmin() {
        return jsExecutorResponseAdmin;
    }

    public TbQueueAdmin getTransportApiRequestAdmin() {
        return transportApiRequestAdmin;
    }

    public TbQueueAdmin getTransportApiResponseAdmin() {
        return transportApiResponseAdmin;
    }

    public TbQueueAdmin getNotificationAdmin() {
        return notificationAdmin;
    }

    public TbQueueAdmin getFwUpdatesAdmin() {
        return fwUpdatesAdmin;
    }

    public TbQueueAdmin getVcAdmin() {
        return vcAdmin;
    }

    public TbQueueAdmin getHousekeeperAdmin() {
        return housekeeperAdmin;
    }

    public TbQueueAdmin getHousekeeperReprocessingAdmin() {
        return housekeeperReprocessingAdmin;
    }

    public AtomicLong getConsumerCount() {
        return consumerCount;
    }

    public void setTopicService(TopicService topicService) {
        this.topicService=topicService;
    }

    public void setKafkaSettings(TbKafkaSettings kafkaSettings) {
        this.kafkaSettings=kafkaSettings;
    }

    public void setServiceInfoProvider(TbServiceInfoProvider serviceInfoProvider) {
        this.serviceInfoProvider=serviceInfoProvider;
    }

    public void setCoreSettings(TbQueueCoreSettings coreSettings) {
        this.coreSettings=coreSettings;
    }

    public void setRuleEngineSettings(TbQueueRuleEngineSettings ruleEngineSettings) {
        this.ruleEngineSettings=ruleEngineSettings;
    }

    public void setTransportApiSettings(TbQueueTransportApiSettings transportApiSettings) {
        this.transportApiSettings=transportApiSettings;
    }

    public void setTransportNotificationSettings(TbQueueTransportNotificationSettings transportNotificationSettings) {
        this.transportNotificationSettings=transportNotificationSettings;
    }

    public void setJsInvokeSettings(TbQueueRemoteJsInvokeSettings jsInvokeSettings) {
        this.jsInvokeSettings=jsInvokeSettings;
    }

    public void setVcSettings(TbQueueVersionControlSettings vcSettings) {
        this.vcSettings=vcSettings;
    }

    public void setConsumerStatsService(TbKafkaConsumerStatsService consumerStatsService) {
        this.consumerStatsService=consumerStatsService;
    }

    public void setCoreAdmin(TbQueueAdmin coreAdmin) {
        this.coreAdmin=coreAdmin;
    }

    public void setJsExecutorRequestAdmin(TbQueueAdmin jsExecutorRequestAdmin) {
        this.jsExecutorRequestAdmin=jsExecutorRequestAdmin;
    }

    public void setJsExecutorResponseAdmin(TbQueueAdmin jsExecutorResponseAdmin) {
        this.jsExecutorResponseAdmin=jsExecutorResponseAdmin;
    }

    public void setTransportApiRequestAdmin(TbQueueAdmin transportApiRequestAdmin) {
        this.transportApiRequestAdmin=transportApiRequestAdmin;
    }

    public void setTransportApiResponseAdmin(TbQueueAdmin transportApiResponseAdmin) {
        this.transportApiResponseAdmin=transportApiResponseAdmin;
    }

    public void setNotificationAdmin(TbQueueAdmin notificationAdmin) {
        this.notificationAdmin=notificationAdmin;
    }

    public void setFwUpdatesAdmin(TbQueueAdmin fwUpdatesAdmin) {
        this.fwUpdatesAdmin=fwUpdatesAdmin;
    }

    public void setVcAdmin(TbQueueAdmin vcAdmin) {
        this.vcAdmin=vcAdmin;
    }

    public void setHousekeeperAdmin(TbQueueAdmin housekeeperAdmin) {
        this.housekeeperAdmin=housekeeperAdmin;
    }

    public void setHousekeeperReprocessingAdmin(TbQueueAdmin housekeeperReprocessingAdmin) {
        this.housekeeperReprocessingAdmin=housekeeperReprocessingAdmin;
    }

    public void setConsumerCount(AtomicLong consumerCount) {
        this.consumerCount=consumerCount;
    }

    public KafkaQueueAdministration    (TopicService topicService,TbKafkaSettings kafkaSettings,TbServiceInfoProvider serviceInfoProvider,TbQueueCoreSettings coreSettings,TbQueueRuleEngineSettings ruleEngineSettings,TbQueueTransportApiSettings transportApiSettings,TbQueueTransportNotificationSettings transportNotificationSettings,TbQueueRemoteJsInvokeSettings jsInvokeSettings,TbQueueVersionControlSettings vcSettings,TbKafkaConsumerStatsService consumerStatsService,TbQueueAdmin coreAdmin,TbQueueAdmin jsExecutorRequestAdmin,TbQueueAdmin jsExecutorResponseAdmin,TbQueueAdmin transportApiRequestAdmin,TbQueueAdmin transportApiResponseAdmin,TbQueueAdmin notificationAdmin,TbQueueAdmin fwUpdatesAdmin,TbQueueAdmin vcAdmin,TbQueueAdmin housekeeperAdmin,TbQueueAdmin housekeeperReprocessingAdmin,AtomicLong consumerCount){
        this.topicService    =    topicService;
        this.kafkaSettings    =    kafkaSettings;
        this.serviceInfoProvider    =    serviceInfoProvider;
        this.coreSettings    =    coreSettings;
        this.ruleEngineSettings    =    ruleEngineSettings;
        this.transportApiSettings    =    transportApiSettings;
        this.transportNotificationSettings    =    transportNotificationSettings;
        this.jsInvokeSettings    =    jsInvokeSettings;
        this.vcSettings    =    vcSettings;
        this.consumerStatsService    =    consumerStatsService;
        this.coreAdmin    =    coreAdmin;
        this.jsExecutorRequestAdmin    =    jsExecutorRequestAdmin;
        this.jsExecutorResponseAdmin    =    jsExecutorResponseAdmin;
        this.transportApiRequestAdmin    =    transportApiRequestAdmin;
        this.transportApiResponseAdmin    =    transportApiResponseAdmin;
        this.notificationAdmin    =    notificationAdmin;
        this.fwUpdatesAdmin    =    fwUpdatesAdmin;
        this.vcAdmin    =    vcAdmin;
        this.housekeeperAdmin    =    housekeeperAdmin;
        this.housekeeperReprocessingAdmin    =    housekeeperReprocessingAdmin;
        this.consumerCount    =    consumerCount;
}
}
