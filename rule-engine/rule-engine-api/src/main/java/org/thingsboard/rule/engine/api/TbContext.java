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
package org.thingsboard.rule.engine.api;

import io.netty.channel.EventLoopGroup;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbContext {

    /*
     *
     *  METHODS TO CONTROL THE MESSAGE FLOW
     *
     */

    /**
     * Indicates that message was successfully processed by the rule node.
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using "Success" relationType.
     *
     * @param msg
     */
    void tellSuccess(TbMsg msg);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using specified relationType.
     *
     * @param msg
     * @param relationType
     */
    void tellNext(TbMsg msg, String relationType);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using one of specified relationTypes.
     *
     * @param msg
     * @param relationTypes
     */
    void tellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * Sends message to the current Rule Node with specified delay in milliseconds.
     * Note: this message is not queued and may be lost in case of a server restart.
     *
     * @param msg
     */
    void tellSelf(TbMsg msg, long delayMs);

    /**
     * Notifies Rule Engine about failure to process current message.
     *
     * @param msg - message
     * @param th  - exception
     */
    void tellFailure(TbMsg msg, Throwable th);

    /**
     * Puts new message to queue for processing by the Root Rule Chain
     *
     * @param msg - message
     */
    void enqueue(TbMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * Sends message to the nested rule chain.
     * Fails processing of the message if the nested rule chain is not found.
     *
     * @param msg - the message
     * @param ruleChainId - the id of a nested rule chain
     */
    void input(TbMsg msg, RuleChainId ruleChainId);

    /**
     * Sends message to the caller rule chain.
     * Acknowledge the message if no caller rule chain is present in processing stack
     *
     * @param msg - the message
     * @param relationType - the relation type that will be used to route messages in the caller rule chain
     */
    void output(TbMsg msg, String relationType);

    /**
     * Puts new message to custom queue for processing
     *
     * @param msg - message
     */
    void enqueue(TbMsg msg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellFailure(TbMsg msg, String failureMessage);

    void enqueueForTellNext(TbMsg msg, String relationType);

    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes);

    void enqueueForTellNext(TbMsg msg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    void ack(TbMsg tbMsg);

    TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    // TODO: Does this changes the message?
    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, String action);

    TbMsg attributesUpdatedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<AttributeKvEntry> attributes);

    TbMsg attributesDeletedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<String> keys);

    void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId);

    /*
     *
     *  METHODS TO PROCESS THE MESSAGES
     *
     */

    boolean isLocalEntity(EntityId entityId);

    void isTenantEntity(EntityId entityId);

    RuleNodeId getSelfId();

    RuleNode getSelf();

    String getRuleChainName();

    TenantId getTenantId();

    AttributesService getAttributesService();

    CustomerService getCustomerService();

    TenantService getTenantService();

    UserService getUserService();

    AssetService getAssetService();

    DeviceService getDeviceService();

    DeviceCredentialsService getDeviceCredentialsService();

    TbClusterService getClusterService();

    DashboardService getDashboardService();

    RuleEngineAlarmService getAlarmService();

    RuleChainService getRuleChainService();

    RuleEngineRpcService getRpcService();

    RuleEngineTelemetryService getTelemetryService();

    TimeseriesService getTimeseriesService();

    RelationService getRelationService();

    EntityViewService getEntityViewService();

    ResourceService getResourceService();

    OtaPackageService getOtaPackageService();

    RuleEngineDeviceProfileCache getDeviceProfileCache();

    RuleEngineAssetProfileCache getAssetProfileCache();

    EdgeService getEdgeService();

    EdgeEventService getEdgeEventService();

    QueueService getQueueService();

    ListeningExecutor getMailExecutor();

    ListeningExecutor getSmsExecutor();

    ListeningExecutor getDbCallbackExecutor();

    ListeningExecutor getExternalCallExecutor();

    MailService getMailService(boolean isSystem);

    SmsService getSmsService();

    SmsSenderFactory getSmsSenderFactory();

    ScriptEngine createJsScriptEngine(String script, String... argNames);

    void logJsEvalRequest();

    void logJsEvalResponse();

    void logJsEvalFailure();

    String getServiceId();

    EventLoopGroup getSharedEventLoop();

    CassandraCluster getCassandraCluster();

    TbResultSetFuture submitCassandraReadTask(CassandraStatementTask task);

    TbResultSetFuture submitCassandraWriteTask(CassandraStatementTask task);

    PageData<RuleNodeState> findRuleNodeStates(PageLink pageLink);

    RuleNodeState findRuleNodeStateForEntity(EntityId entityId);

    void removeRuleNodeStateForEntity(EntityId entityId);

    RuleNodeState saveRuleNodeState(RuleNodeState state);

    void clearRuleNodeStates();

    void addTenantProfileListener(Consumer<TenantProfile> listener);

    void addDeviceProfileListeners(Consumer<DeviceProfile> listener, BiConsumer<DeviceId, DeviceProfile> deviceListener);

    void addAssetProfileListeners(Consumer<AssetProfile> listener, BiConsumer<AssetId, AssetProfile> assetListener);

    void removeListeners();

    TenantProfile getTenantProfile();
}
