package org.thingsboard.server.service.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.extensions.api.exception.UnauthorizedException;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;
import org.thingsboard.server.extensions.api.plugins.ws.SessionEvent;
import org.thingsboard.server.extensions.core.plugin.telemetry.cmd.AttributesSubscriptionCmd;
import org.thingsboard.server.extensions.core.plugin.telemetry.cmd.SubscriptionCmd;
import org.thingsboard.server.extensions.core.plugin.telemetry.cmd.TelemetryPluginCmd;
import org.thingsboard.server.extensions.core.plugin.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionState;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionType;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionUpdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetryWebSocketService implements TelemetryWebSocketService {

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
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService tsService;

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

                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), entityId, SubscriptionType.ATTRIBUTES, false, subState, cmd.getScope());
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
            //ValidationCallback?
            ctx.loadAttributes(entityId, Arrays.asList(DataConstants.allScopes()), keys, callback);
        } else {
            ctx.loadAttributes(entityId, cmd.getScope(), keys, callback);
        }
    }

    private void handleWsAttributesSubscription(PluginContext ctx, PluginWebsocketSessionRef sessionRef,
                                                AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        PluginCallback<List<AttributeKvEntry>> callback = new PluginCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(PluginContext ctx, List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(ctx, sessionRef, new SubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(attributesData.size());
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), entityId, SubscriptionType.ATTRIBUTES, true, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(ctx, sessionId, entityId, sub);
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                        FAILED_TO_FETCH_ATTRIBUTES);
                sendWsMsg(ctx, sessionRef, update);
            }
        };

        if (StringUtils.isEmpty(cmd.getScope())) {
            ctx.loadAttributes(entityId, Arrays.asList(DataConstants.allScopes()), callback);
        } else {
            ctx.loadAttributes(entityId, cmd.getScope(), callback);
        }
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

}
