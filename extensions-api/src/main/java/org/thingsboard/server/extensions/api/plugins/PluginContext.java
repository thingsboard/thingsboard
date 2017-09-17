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
package org.thingsboard.server.extensions.api.plugins;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.rpc.RpcMsg;
import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;
import org.thingsboard.server.extensions.api.plugins.ws.msg.PluginWebsocketMsg;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PluginContext {

    PluginId getPluginId();

    void reply(PluginToRuleMsg<?> msg);

    void checkAccess(DeviceId deviceId, PluginCallback<Void> callback);

    Optional<PluginApiCallSecurityContext> getSecurityCtx();

    void persistError(String method, Exception e);

    /*
        Device RPC API
     */

    Optional<ServerAddress> resolve(EntityId entityId);

    void getDevice(DeviceId deviceId, PluginCallback<Device> pluginCallback);

    void sendRpcRequest(ToDeviceRpcRequest msg);

    void scheduleTimeoutMsg(TimeoutMsg<?> timeoutMsg);


    /*
        Websocket API
     */

    void send(PluginWebsocketMsg<?> wsMsg) throws IOException;

    void close(PluginWebsocketSessionRef sessionRef) throws IOException;

    /*
        Plugin RPC API
     */

    void sendPluginRpcMsg(RpcMsg msg);

    /*
        Timeseries API
     */


    void saveTsData(EntityId entityId, TsKvEntry entry, PluginCallback<Void> callback);

    void saveTsData(EntityId entityId, List<TsKvEntry> entries, PluginCallback<Void> callback);

    void saveTsData(EntityId deviceId, List<TsKvEntry> entries, long ttl, PluginCallback<Void> pluginCallback);

    void loadTimeseries(EntityId entityId, List<TsKvQuery> queries, PluginCallback<List<TsKvEntry>> callback);

    void loadLatestTimeseries(EntityId entityId, Collection<String> keys, PluginCallback<List<TsKvEntry>> callback);

    void loadLatestTimeseries(EntityId entityId, PluginCallback<List<TsKvEntry>> callback);

    /*
        Attributes API
     */

    void saveAttributes(TenantId tenantId, EntityId entityId, String attributeType, List<AttributeKvEntry> attributes, PluginCallback<Void> callback);

    void removeAttributes(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys, PluginCallback<Void> callback);

    void loadAttribute(EntityId entityId, String attributeType, String attributeKey, PluginCallback<Optional<AttributeKvEntry>> callback);

    void loadAttributes(EntityId entityId, String attributeType, Collection<String> attributeKeys, PluginCallback<List<AttributeKvEntry>> callback);

    void loadAttributes(EntityId entityId, String attributeType, PluginCallback<List<AttributeKvEntry>> callback);

    void loadAttributes(EntityId entityId, Collection<String> attributeTypes, PluginCallback<List<AttributeKvEntry>> callback);

    void loadAttributes(EntityId entityId, Collection<String> attributeTypes, Collection<String> attributeKeys, PluginCallback<List<AttributeKvEntry>> callback);

    void getCustomerDevices(TenantId tenantId, CustomerId customerId, int limit, PluginCallback<List<Device>> callback);


    /*
    *   Relations API
    * */

    ListenableFuture<List<EntityRelation>> findByFromAndType(EntityId from, String relationType);

    ListenableFuture<List<EntityRelation>> findByToAndType(EntityId from, String relationType);
}
