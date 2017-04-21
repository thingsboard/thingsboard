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
package org.thingsboard.server.extensions.core.plugin.telemetry.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.DefaultRestMsgHandler;
import org.thingsboard.server.extensions.api.plugins.rest.PluginRestMsg;
import org.thingsboard.server.extensions.api.plugins.rest.RestRequest;
import org.thingsboard.server.extensions.core.plugin.telemetry.AttributeData;
import org.thingsboard.server.extensions.core.plugin.telemetry.SubscriptionManager;
import org.thingsboard.server.extensions.core.plugin.telemetry.TsData;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TelemetryRestMsgHandler extends DefaultRestMsgHandler {

    private final SubscriptionManager subscriptionManager;

    public TelemetryRestMsgHandler(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void handleHttpGetRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        RestRequest request = msg.getRequest();
        String[] pathParams = request.getPathParams();
        if (pathParams.length >= 3) {
            String deviceIdStr = pathParams[0];
            String method = pathParams[1];
            String entity = pathParams[2];
            String scope = pathParams.length >= 4 ? pathParams[3] : null;
            if (StringUtils.isEmpty(method) || StringUtils.isEmpty(entity) || StringUtils.isEmpty(deviceIdStr)) {
                msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
                return;
            }

            DeviceId deviceId = DeviceId.fromString(deviceIdStr);

            if (method.equals("keys")) {
                if (entity.equals("timeseries")) {
                    ctx.loadLatestTimeseries(deviceId, new PluginCallback<List<TsKvEntry>>() {
                        @Override
                        public void onSuccess(PluginContext ctx, List<TsKvEntry> value) {
                            List<String> keys = value.stream().map(tsKv -> tsKv.getKey()).collect(Collectors.toList());
                            msg.getResponseHolder().setResult(new ResponseEntity<>(keys, HttpStatus.OK));
                        }

                        @Override
                        public void onFailure(PluginContext ctx, Exception e) {
                            msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                    });
                } else if (entity.equals("attributes")) {
                    PluginCallback<List<AttributeKvEntry>> callback = getAttributeKeysPluginCallback(msg);
                    if (!StringUtils.isEmpty(scope)) {
                        ctx.loadAttributes(deviceId, scope, callback);
                    } else {
                        ctx.loadAttributes(deviceId, Arrays.asList(DataConstants.ALL_SCOPES), callback);
                    }
                }
            } else if (method.equals("values")) {
                if ("timeseries".equals(entity)) {
                    String keysStr = request.getParameter("keys");
                    List<String> keys = Arrays.asList(keysStr.split(","));

                    Optional<Long> startTs = request.getLongParamValue("startTs");
                    Optional<Long> endTs = request.getLongParamValue("endTs");
                    Optional<Long> interval = request.getLongParamValue("interval");
                    Optional<Integer> limit = request.getIntParamValue("limit");

                    if (startTs.isPresent() || endTs.isPresent() || interval.isPresent() || limit.isPresent()) {
                        if (!startTs.isPresent() || !endTs.isPresent() || !interval.isPresent()) {
                            msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
                            return;
                        }
                        Aggregation agg = Aggregation.valueOf(request.getParameter("agg", Aggregation.NONE.name()));

                        List<TsKvQuery> queries = keys.stream().map(key -> new BaseTsKvQuery(key, startTs.get(), endTs.get(), interval.get(), limit.orElse(TelemetryWebsocketMsgHandler.DEFAULT_LIMIT), agg))
                                .collect(Collectors.toList());
                        ctx.loadTimeseries(deviceId, queries, getTsKvListCallback(msg));
                    } else {
                        ctx.loadLatestTimeseries(deviceId, keys, getTsKvListCallback(msg));
                    }
                } else if ("attributes".equals(entity)) {
                    String keys = request.getParameter("keys", "");

                    PluginCallback<List<AttributeKvEntry>> callback = getAttributeValuesPluginCallback(msg);
                    if (!StringUtils.isEmpty(scope)) {
                        if (!StringUtils.isEmpty(keys)) {
                            List<String> keyList = Arrays.asList(keys.split(","));
                            ctx.loadAttributes(deviceId, scope, keyList, callback);
                        } else {
                            ctx.loadAttributes(deviceId, scope, callback);
                        }
                    } else {
                        if (!StringUtils.isEmpty(keys)) {
                            List<String> keyList = Arrays.asList(keys.split(","));
                            ctx.loadAttributes(deviceId, Arrays.asList(DataConstants.ALL_SCOPES), keyList, callback);
                        } else {
                            ctx.loadAttributes(deviceId, Arrays.asList(DataConstants.ALL_SCOPES), callback);
                        }
                    }
                }
            }
        } else {
            msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        }
    }

    private PluginCallback<List<TsKvEntry>> getTsKvListCallback(final PluginRestMsg msg) {
        return new PluginCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(PluginContext ctx, List<TsKvEntry> data) {
                Map<String, List<TsData>> result = new LinkedHashMap<>();
                for (TsKvEntry entry : data) {
                    List<TsData> vList = result.get(entry.getKey());
                    if (vList == null) {
                        vList = new ArrayList<>();
                        result.put(entry.getKey(), vList);
                    }
                    vList.add(new TsData(entry.getTs(), entry.getValueAsString()));
                }
                msg.getResponseHolder().setResult(new ResponseEntity<>(result, HttpStatus.OK));
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error("Failed to fetch historical data", e);
                msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            }
        };
    }

    @Override
    public void handleHttpPostRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        RestRequest request = msg.getRequest();
        try {
            String[] pathParams = request.getPathParams();
            if (pathParams.length == 2) {
                DeviceId deviceId = DeviceId.fromString(pathParams[0]);
                String scope = pathParams[1];
                if (DataConstants.SERVER_SCOPE.equals(scope) ||
                        DataConstants.SHARED_SCOPE.equals(scope)) {
                    JsonNode jsonNode = jsonMapper.readTree(request.getRequestBody());
                    if (jsonNode.isObject()) {
                        long ts = System.currentTimeMillis();
                        List<AttributeKvEntry> attributes = new ArrayList<>();
                        jsonNode.fields().forEachRemaining(entry -> {
                            String key = entry.getKey();
                            JsonNode value = entry.getValue();
                            if (entry.getValue().isTextual()) {
                                attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value.textValue()), ts));
                            } else if (entry.getValue().isBoolean()) {
                                attributes.add(new BaseAttributeKvEntry(new BooleanDataEntry(key, value.booleanValue()), ts));
                            } else if (entry.getValue().isDouble()) {
                                attributes.add(new BaseAttributeKvEntry(new DoubleDataEntry(key, value.doubleValue()), ts));
                            } else if (entry.getValue().isNumber()) {
                                attributes.add(new BaseAttributeKvEntry(new LongDataEntry(key, value.longValue()), ts));
                            }
                        });
                        if (attributes.size() > 0) {
                            ctx.saveAttributes(ctx.getSecurityCtx().orElseThrow(() -> new IllegalArgumentException()).getTenantId(), deviceId, scope, attributes, new PluginCallback<Void>() {
                                @Override
                                public void onSuccess(PluginContext ctx, Void value) {
                                    msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.OK));
                                    subscriptionManager.onAttributesUpdateFromServer(ctx, deviceId, scope, attributes);
                                }

                                @Override
                                public void onFailure(PluginContext ctx, Exception e) {
                                    log.error("Failed to save attributes", e);
                                    msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                                }
                            });
                            return;
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            log.debug("Failed to process POST request due to exception", e);
        }
        msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @Override
    public void handleHttpDeleteRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        RestRequest request = msg.getRequest();
        try {
            String[] pathParams = request.getPathParams();
            if (pathParams.length == 2) {
                DeviceId deviceId = DeviceId.fromString(pathParams[0]);
                String scope = pathParams[1];
                if (DataConstants.SERVER_SCOPE.equals(scope) ||
                        DataConstants.SHARED_SCOPE.equals(scope) ||
                        DataConstants.CLIENT_SCOPE.equals(scope)) {
                    String keysParam = request.getParameter("keys");
                    if (!StringUtils.isEmpty(keysParam)) {
                        String[] keys = keysParam.split(",");
                        ctx.removeAttributes(ctx.getSecurityCtx().orElseThrow(() -> new IllegalArgumentException()).getTenantId(), deviceId, scope, Arrays.asList(keys), new PluginCallback<Void>() {
                            @Override
                            public void onSuccess(PluginContext ctx, Void value) {
                                msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.OK));
                            }

                            @Override
                            public void onFailure(PluginContext ctx, Exception e) {
                                log.error("Failed to remove attributes", e);
                                msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                            }
                        });
                        return;
                    }
                }
            }
        } catch (RuntimeException e) {
            log.debug("Failed to process DELETE request due to Runtime exception", e);
        }
        msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }


    private PluginCallback<List<AttributeKvEntry>> getAttributeKeysPluginCallback(final PluginRestMsg msg) {
        return new PluginCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(PluginContext ctx, List<AttributeKvEntry> attributes) {
                List<String> keys = attributes.stream().map(attrKv -> attrKv.getKey()).collect(Collectors.toList());
                msg.getResponseHolder().setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error("Failed to fetch attributes", e);
                msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            }
        };
    }

    private PluginCallback<List<AttributeKvEntry>> getAttributeValuesPluginCallback(final PluginRestMsg msg) {
        return new PluginCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(PluginContext ctx, List<AttributeKvEntry> attributes) {
                List<AttributeData> values = attributes.stream().map(attribute -> new AttributeData(attribute.getLastUpdateTs(),
                        attribute.getKey(), attribute.getValue())).collect(Collectors.toList());
                msg.getResponseHolder().setResult(new ResponseEntity<>(values, HttpStatus.OK));
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error("Failed to fetch attributes", e);
                msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            }
        };
    }
}
