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
package org.thingsboard.server.service.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.ValidationResult;
import org.thingsboard.server.service.telemetry.cmd.AttributesSubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.GetHistoryCmd;
import org.thingsboard.server.service.telemetry.cmd.SubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmd;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.telemetry.exception.UnauthorizedException;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.service.telemetry.sub.SubscriptionState;
import org.thingsboard.server.service.telemetry.sub.SubscriptionUpdate;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetryWebSocketService implements TelemetryWebSocketService {

    public static final int DEFAULT_LIMIT = 100;
    public static final Aggregation DEFAULT_AGGREGATION = Aggregation.NONE;
    private static final int UNKNOWN_SUBSCRIPTION_ID = 0;
    private static final String PROCESSING_MSG = "[{}] Processing: {}";
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final String FAILED_TO_FETCH_DATA = "Failed to fetch data!";
    private static final String FAILED_TO_FETCH_ATTRIBUTES = "Failed to fetch attributes!";
    private static final String SESSION_META_DATA_NOT_FOUND = "Session meta-data not found!";

    private final ConcurrentMap<String, WsSessionMetaData> wsSessionsMap = new ConcurrentHashMap<>();

    @Autowired
    private TelemetrySubscriptionService subscriptionManager;

    @Autowired
    private TelemetryWebSocketMsgEndpoint msgEndpoint;

    @Autowired
    private AccessValidator accessValidator;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService tsService;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void handleWebSocketSessionEvent(TelemetryWebSocketSessionRef sessionRef, SessionEvent event) {
        String sessionId = sessionRef.getSessionId();
        log.debug(PROCESSING_MSG, sessionId, event);
        switch (event.getEventType()) {
            case ESTABLISHED:
                wsSessionsMap.put(sessionId, new WsSessionMetaData(sessionRef));
                break;
            case ERROR:
                log.debug("[{}] Unknown websocket session error: {}. ", sessionId, event.getError().orElse(null));
                break;
            case CLOSED:
                wsSessionsMap.remove(sessionId);
                subscriptionManager.cleanupLocalWsSessionSubscriptions(sessionRef, sessionId);
                break;
        }
    }

    @Override
    public void handleWebSocketMsg(TelemetryWebSocketSessionRef sessionRef, String msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing: {}", sessionRef.getSessionId(), msg);
        }

        try {
            TelemetryPluginCmdsWrapper cmdsWrapper = jsonMapper.readValue(msg, TelemetryPluginCmdsWrapper.class);
            if (cmdsWrapper != null) {
                if (cmdsWrapper.getAttrSubCmds() != null) {
                    cmdsWrapper.getAttrSubCmds().forEach(cmd -> handleWsAttributesSubscriptionCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getTsSubCmds() != null) {
                    cmdsWrapper.getTsSubCmds().forEach(cmd -> handleWsTimeseriesSubscriptionCmd(sessionRef, cmd));
                }
                if (cmdsWrapper.getHistoryCmds() != null) {
                    cmdsWrapper.getHistoryCmds().forEach(cmd -> handleWsHistoryCmd(sessionRef, cmd));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to decode subscription cmd: {}", e.getMessage(), e);
            SubscriptionUpdate update = new SubscriptionUpdate(UNKNOWN_SUBSCRIPTION_ID, SubscriptionErrorCode.INTERNAL_ERROR, SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
        }
    }

    @Override
    public void sendWsMsg(String sessionId, SubscriptionUpdate update) {
        WsSessionMetaData md = wsSessionsMap.get(sessionId);
        if (md != null) {
            sendWsMsg(md.getSessionRef(), update);
        }
    }

    private void handleWsAttributesSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, AttributesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                log.debug("[{}] fetching latest attributes ({}) values for device: {}", sessionId, cmd.getKeys(), entityId);
                Optional<Set<String>> keysOptional = getKeys(cmd);
                if (keysOptional.isPresent()) {
                    List<String> keys = new ArrayList<>(keysOptional.get());
                    handleWsAttributesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId, keys);
                } else {
                    handleWsAttributesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsAttributesSubscriptionByKeys(TelemetryWebSocketSessionRef sessionRef,
                                                      AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId,
                                                      List<String> keys) {
        FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(keys.size());
                keys.forEach(key -> subState.put(key, 0L));
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), entityId, TelemetryFeature.ATTRIBUTES, false, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                SubscriptionUpdate update;
                if (UnauthorizedException.class.isInstance(e)) {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_ATTRIBUTES);
                }
                sendWsMsg(sessionRef, update);
            }
        };

        if (StringUtils.isEmpty(cmd.getScope())) {
            accessValidator.validate(sessionRef.getSecurityCtx(), entityId, getAttributesFetchCallback(entityId, keys, callback));
        } else {
            accessValidator.validate(sessionRef.getSecurityCtx(), entityId, getAttributesFetchCallback(entityId, cmd.getScope(), keys, callback));
        }
    }

    private void handleWsHistoryCmd(TelemetryWebSocketSessionRef sessionRef, GetHistoryCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                    SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty() || cmd.getEntityType() == null || cmd.getEntityType().isEmpty()) {
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getKeys() == null || cmd.getKeys().isEmpty()) {
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Keys are empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
        List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
        List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getEndTs(), cmd.getInterval(), getLimit(cmd.getLimit()), getAggregation(cmd.getAgg())))
                .collect(Collectors.toList());

        FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), data));
            }

            @Override
            public void onFailure(Throwable e) {
                SubscriptionUpdate update;
                if (UnauthorizedException.class.isInstance(e)) {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_DATA);
                }
                sendWsMsg(sessionRef, update);
            }
        };
        accessValidator.validate(sessionRef.getSecurityCtx(), entityId,
                on(r -> Futures.addCallback(tsService.findAll(entityId, queries), callback, executor), callback::onFailure));
    }

    private void handleWsAttributesSubscription(TelemetryWebSocketSessionRef sessionRef,
                                                AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(attributesData.size());
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), entityId, TelemetryFeature.ATTRIBUTES, true, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                        FAILED_TO_FETCH_ATTRIBUTES);
                sendWsMsg(sessionRef, update);
            }
        };


        if (StringUtils.isEmpty(cmd.getScope())) {
            accessValidator.validate(sessionRef.getSecurityCtx(), entityId, getAttributesFetchCallback(entityId, callback));
        } else {
            accessValidator.validate(sessionRef.getSecurityCtx(), entityId, getAttributesFetchCallback(entityId, cmd.getScope(), callback));
        }
    }

    private void handleWsTimeseriesSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, TimeseriesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                Optional<Set<String>> keysOptional = getKeys(cmd);

                if (keysOptional.isPresent()) {
                    handleWsTimeseriesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId);
                } else {
                    handleWsTimeseriesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsTimeseriesSubscriptionByKeys(TelemetryWebSocketSessionRef sessionRef,
                                                      TimeseriesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        long startTs;
        if (cmd.getTimeWindow() > 0) {
            List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            log.debug("[{}] fetching timeseries data for last {} ms for keys: ({}) for device : {}", sessionId, cmd.getTimeWindow(), cmd.getKeys(), entityId);
            startTs = cmd.getStartTs();
            long endTs = cmd.getStartTs() + cmd.getTimeWindow();
            List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, cmd.getInterval(),
                    getLimit(cmd.getLimit()), getAggregation(cmd.getAgg()))).collect(Collectors.toList());

            final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), entityId,
                    on(r -> Futures.addCallback(tsService.findAll(entityId, queries), callback, executor), callback::onFailure));
        } else {
            List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            startTs = System.currentTimeMillis();
            log.debug("[{}] fetching latest timeseries data for keys: ({}) for device : {}", sessionId, cmd.getKeys(), entityId);
            final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), entityId,
                    on(r -> Futures.addCallback(tsService.findLatest(entityId, keys), callback, executor), callback::onFailure));
        }
    }

    private void handleWsTimeseriesSubscription(TelemetryWebSocketSessionRef sessionRef,
                                                TimeseriesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), data));
                Map<String, Long> subState = new HashMap<>(data.size());
                data.forEach(v -> subState.put(v.getKey(), v.getTs()));
                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), entityId, TelemetryFeature.TIMESERIES, true, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                SubscriptionUpdate update;
                if (UnauthorizedException.class.isInstance(e)) {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_DATA);
                }
                sendWsMsg(sessionRef, update);
            }
        };
        accessValidator.validate(sessionRef.getSecurityCtx(), entityId,
                on(r -> Futures.addCallback(tsService.findAllLatest(entityId), callback, executor), callback::onFailure));
    }

    private FutureCallback<List<TsKvEntry>> getSubscriptionCallback(final TelemetryWebSocketSessionRef sessionRef, final TimeseriesSubscriptionCmd cmd, final String sessionId, final EntityId entityId, final long startTs, final List<String> keys) {
        return new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), data));

                Map<String, Long> subState = new HashMap<>(keys.size());
                keys.forEach(key -> subState.put(key, startTs));
                data.forEach(v -> subState.put(v.getKey(), v.getTs()));
                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), entityId, TelemetryFeature.TIMESERIES, false, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_DATA, e);
                SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                        FAILED_TO_FETCH_DATA);
                sendWsMsg(sessionRef, update);
            }
        };
    }

    private void unsubscribe(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd, String sessionId) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            subscriptionManager.cleanupLocalWsSessionSubscriptions(sessionRef, sessionId);
        } else {
            subscriptionManager.removeSubscription(sessionId, cmd.getCmdId());
        }
    }

    private boolean validateSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSessionMetadata(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd, String sessionId) {
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                    SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return false;
        } else {
            return true;
        }
    }

    private void sendWsMsg(TelemetryWebSocketSessionRef sessionRef, SubscriptionUpdate update) {
        try {
            msgEndpoint.send(sessionRef, jsonMapper.writeValueAsString(update));
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to encode reply: {}", sessionRef.getSessionId(), update, e);
        } catch (IOException e) {
            log.warn("[{}] Failed to send reply: {}", sessionRef.getSessionId(), update, e);
        }
    }

    private static Optional<Set<String>> getKeys(TelemetryPluginCmd cmd) {
        if (!StringUtils.isEmpty(cmd.getKeys())) {
            Set<String> keys = new HashSet<>();
            Collections.addAll(keys, cmd.getKeys().split(","));
            return Optional.of(keys);
        } else {
            return Optional.empty();
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final EntityId entityId, final List<String> keys, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.find(entityId, scope, keys));
                }

                ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final EntityId entityId, final String scope, final List<String> keys, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                Futures.addCallback(attributesService.find(entityId, scope, keys), callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final EntityId entityId, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.findAll(entityId, scope));
                }

                ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final EntityId entityId, final String scope, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                Futures.addCallback(attributesService.findAll(entityId, scope), callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private FutureCallback<ValidationResult> on(Consumer<ValidationResult> success, Consumer<Throwable> failure) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                success.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
                failure.accept(t);
            }
        };
    }


    private static Aggregation getAggregation(String agg) {
        return StringUtils.isEmpty(agg) ? DEFAULT_AGGREGATION : Aggregation.valueOf(agg);
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }
}
