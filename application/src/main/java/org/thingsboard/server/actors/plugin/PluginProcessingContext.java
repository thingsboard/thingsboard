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
package org.thingsboard.server.actors.plugin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.device.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.rpc.PluginRpcMsg;
import org.thingsboard.server.extensions.api.plugins.rpc.RpcMsg;
import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;
import org.thingsboard.server.extensions.api.plugins.ws.msg.PluginWebsocketMsg;

import akka.actor.ActorRef;

import javax.annotation.Nullable;

@Slf4j
public final class PluginProcessingContext implements PluginContext {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    private final SharedPluginProcessingContext pluginCtx;
    private final Optional<PluginApiCallSecurityContext> securityCtx;

    public PluginProcessingContext(SharedPluginProcessingContext pluginCtx, PluginApiCallSecurityContext securityCtx) {
        super();
        this.pluginCtx = pluginCtx;
        this.securityCtx = Optional.ofNullable(securityCtx);
    }

    @Override
    public void sendPluginRpcMsg(RpcMsg msg) {
        this.pluginCtx.rpcService.tell(new PluginRpcMsg(pluginCtx.tenantId, pluginCtx.pluginId, msg));
    }

    @Override
    public void send(PluginWebsocketMsg<?> wsMsg) throws IOException {
        pluginCtx.msgEndpoint.send(wsMsg);
    }

    @Override
    public void close(PluginWebsocketSessionRef sessionRef) throws IOException {
        pluginCtx.msgEndpoint.close(sessionRef);
    }

    @Override
    public void saveAttributes(DeviceId deviceId, String scope, List<AttributeKvEntry> attributes, PluginCallback<Void> callback) {
        validate(deviceId);
        Set<AttributeKey> keys = new HashSet<>();
        for (AttributeKvEntry attribute : attributes) {
            keys.add(new AttributeKey(scope, attribute.getKey()));
        }

        ListenableFuture<List<ResultSet>> rsListFuture = pluginCtx.attributesService.save(deviceId, scope, attributes);
        Futures.addCallback(rsListFuture, getListCallback(callback, v -> {
            onDeviceAttributesChanged(deviceId, keys);
            return null;
        }), executor);
    }

    @Override
    public Optional<AttributeKvEntry> loadAttribute(DeviceId deviceId, String attributeType, String attributeKey) {
        validate(deviceId);
        AttributeKvEntry attribute = pluginCtx.attributesService.find(deviceId, attributeType, attributeKey);
        return Optional.ofNullable(attribute);
    }

    @Override
    public List<AttributeKvEntry> loadAttributes(DeviceId deviceId, String attributeType, List<String> attributeKeys) {
        validate(deviceId);
        List<AttributeKvEntry> result = new ArrayList<>(attributeKeys.size());
        for (String attributeKey : attributeKeys) {
            AttributeKvEntry attribute = pluginCtx.attributesService.find(deviceId, attributeType, attributeKey);
            if (attribute != null) {
                result.add(attribute);
            }
        }
        return result;
    }

    @Override
    public List<AttributeKvEntry> loadAttributes(DeviceId deviceId, String attributeType) {
        validate(deviceId);
        return pluginCtx.attributesService.findAll(deviceId, attributeType);
    }

    @Override
    public void removeAttributes(DeviceId deviceId, String scope, List<String> keys) {
        validate(deviceId);
        pluginCtx.attributesService.removeAll(deviceId, scope, keys);
        onDeviceAttributesDeleted(deviceId, keys.stream().map(key -> new AttributeKey(scope, key)).collect(Collectors.toSet()));
    }

    @Override
    public void saveTsData(DeviceId deviceId, TsKvEntry entry, PluginCallback<Void> callback) {
        validate(deviceId);
        ListenableFuture<List<ResultSet>> rsListFuture = pluginCtx.tsService.save(DataConstants.DEVICE, deviceId, entry);
        Futures.addCallback(rsListFuture, getListCallback(callback, v -> null), executor);
    }

    @Override
    public void saveTsData(DeviceId deviceId, List<TsKvEntry> entries, PluginCallback<Void> callback) {
        validate(deviceId);
        ListenableFuture<List<ResultSet>> rsListFuture = pluginCtx.tsService.save(DataConstants.DEVICE, deviceId, entries);
        Futures.addCallback(rsListFuture, getListCallback(callback, v -> null), executor);
    }

    @Override
    public List<TsKvEntry> loadTimeseries(DeviceId deviceId, TsKvQuery query) {
        validate(deviceId);
        try {
            return pluginCtx.tsService.findAll(DataConstants.DEVICE, deviceId, query).get();
        } catch (Exception e) {
            log.error("TODO", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void loadTimeseries(DeviceId deviceId, TsKvQuery query, PluginCallback<List<TsKvEntry>> callback) {
        validate(deviceId);
        ListenableFuture<List<TsKvEntry>> future = pluginCtx.tsService.findAll(DataConstants.DEVICE, deviceId, query);
        Futures.addCallback(future, getCallback(callback, v -> v), executor);
    }

    @Override
    public void loadLatestTimeseries(DeviceId deviceId, PluginCallback<List<TsKvEntry>> callback) {
        validate(deviceId);
        ResultSetFuture future = pluginCtx.tsService.findAllLatest(DataConstants.DEVICE, deviceId);
        Futures.addCallback(future, getCallback(callback, pluginCtx.tsService::convertResultSetToTsKvEntryList), executor);
    }

    @Override
    public void loadLatestTimeseries(DeviceId deviceId, Collection<String> keys, PluginCallback<List<TsKvEntry>> callback) {
        validate(deviceId);
        ListenableFuture<List<ResultSet>> rsListFuture = pluginCtx.tsService.findLatest(DataConstants.DEVICE, deviceId, keys);
        Futures.addCallback(rsListFuture, getListCallback(callback, rsList ->
        {
            List<TsKvEntry> result = new ArrayList<>();
            for (ResultSet rs : rsList) {
                Row row = rs.one();
                if (row != null) {
                    result.add(pluginCtx.tsService.convertResultToTsKvEntry(row));
                }
            }
            return result;
        }), executor);
    }

    @Override
    public void reply(PluginToRuleMsg<?> msg) {
        pluginCtx.parentActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public boolean checkAccess(DeviceId deviceId) {
        try {
            return validate(deviceId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public PluginId getPluginId() {
        return pluginCtx.pluginId;
    }

    @Override
    public Optional<PluginApiCallSecurityContext> getSecurityCtx() {
        return securityCtx;
    }

    private void onDeviceAttributesChanged(DeviceId deviceId, AttributeKey key) {
        onDeviceAttributesChanged(deviceId, Collections.singleton(key));
    }

    private void onDeviceAttributesDeleted(DeviceId deviceId, Set<AttributeKey> keys) {
        Device device = pluginCtx.deviceService.findDeviceById(deviceId);
        pluginCtx.toDeviceActor(DeviceAttributesEventNotificationMsg.onDelete(device.getTenantId(), deviceId, keys));
    }

    private void onDeviceAttributesChanged(DeviceId deviceId, Set<AttributeKey> keys) {
        Device device = pluginCtx.deviceService.findDeviceById(deviceId);
        pluginCtx.toDeviceActor(DeviceAttributesEventNotificationMsg.onUpdate(device.getTenantId(), deviceId, keys));
    }

    private <T> FutureCallback<List<ResultSet>> getListCallback(final PluginCallback<T> callback, Function<List<ResultSet>, T> transformer) {
        return new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(@Nullable List<ResultSet> result) {
                pluginCtx.self().tell(PluginCallbackMessage.onSuccess(callback, transformer.apply(result)), ActorRef.noSender());
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof Exception) {
                    pluginCtx.self().tell(PluginCallbackMessage.onError(callback, (Exception) t), ActorRef.noSender());
                } else {
                    log.error("Critical error: {}", t.getMessage(), t);
                }
            }
        };
    }

    private <T, R> FutureCallback<R> getCallback(final PluginCallback<T> callback, Function<R, T> transformer) {
        return new FutureCallback<R>() {
            @Override
            public void onSuccess(@Nullable R result) {
                pluginCtx.self().tell(PluginCallbackMessage.onSuccess(callback, transformer.apply(result)), ActorRef.noSender());
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof Exception) {
                    pluginCtx.self().tell(PluginCallbackMessage.onError(callback, (Exception) t), ActorRef.noSender());
                } else {
                    log.error("Critical error: {}", t.getMessage(), t);
                }
            }
        };
    }

    // TODO: replace with our own exceptions
    private boolean validate(DeviceId deviceId) {
        if (securityCtx.isPresent()) {
            PluginApiCallSecurityContext ctx = securityCtx.get();
            if (ctx.isTenantAdmin() || ctx.isCustomerUser()) {
                Device device = pluginCtx.deviceService.findDeviceById(deviceId);
                if (device == null) {
                    throw new IllegalStateException("Device not found!");
                } else {
                    if (!device.getTenantId().equals(ctx.getTenantId())) {
                        throw new IllegalArgumentException("Device belongs to different tenant!");
                    } else if (ctx.isCustomerUser() && !device.getCustomerId().equals(ctx.getCustomerId())) {
                        throw new IllegalArgumentException("Device belongs to different customer!");
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<ServerAddress> resolve(DeviceId deviceId) {
        return pluginCtx.routingService.resolve(deviceId);
    }

    @Override
    public void getDevice(DeviceId deviceId, PluginCallback<Device> callback) {
        //TODO: add caching here with async api.
        Device device = pluginCtx.deviceService.findDeviceById(deviceId);
        pluginCtx.self().tell(PluginCallbackMessage.onSuccess(callback, device), ActorRef.noSender());
    }

    @Override
    public void getCustomerDevices(TenantId tenantId, CustomerId customerId, int limit, PluginCallback<List<Device>> callback) {
        //TODO: add caching here with async api.
        List<Device> devices = pluginCtx.deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, new TextPageLink(limit)).getData();
        pluginCtx.self().tell(PluginCallbackMessage.onSuccess(callback, devices), ActorRef.noSender());
    }

    @Override
    public void sendRpcRequest(ToDeviceRpcRequest msg) {
        pluginCtx.sendRpcRequest(msg);
    }

    @Override
    public void scheduleTimeoutMsg(TimeoutMsg msg) {
        pluginCtx.scheduleTimeoutMsg(msg);
    }
}
