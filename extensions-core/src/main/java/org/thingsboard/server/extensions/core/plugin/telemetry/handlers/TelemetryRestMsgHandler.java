/**
 * Copyright © 2016 The Thingsboard Authors
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
import org.thingsboard.server.extensions.core.plugin.telemetry.TsData;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TelemetryRestMsgHandler extends DefaultRestMsgHandler {

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
                    List<AttributeKvEntry> attributes;
                    if (!StringUtils.isEmpty(scope)) {
                        attributes = ctx.loadAttributes(deviceId, scope);
                    } else {
                        attributes = ctx.loadAttributes(deviceId, DataConstants.CLIENT_SCOPE);
                        attributes.addAll(ctx.loadAttributes(deviceId, DataConstants.SERVER_SCOPE));
                        attributes.addAll(ctx.loadAttributes(deviceId, DataConstants.SHARED_SCOPE));
                    }
                    List<String> keys = attributes.stream().map(attrKv -> attrKv.getKey()).collect(Collectors.toList());
                    msg.getResponseHolder().setResult(new ResponseEntity<>(keys, HttpStatus.OK));
                }
            } else if (method.equals("values")) {
                if ("timeseries".equals(entity)) {
                    String keys = request.getParameter("keys");
                    Optional<Long> startTs = request.getLongParamValue("startTs");
                    Optional<Long> endTs = request.getLongParamValue("endTs");
                    Optional<Integer> limit = request.getIntParamValue("limit");
                    Map<String, List<TsData>> data = new LinkedHashMap<>();
                    for (String key : keys.split(",")) {
                        List<TsKvEntry> entries = ctx.loadTimeseries(deviceId, new BaseTsKvQuery(key, startTs, endTs, limit));
                        data.put(key, entries.stream().map(v -> new TsData(v.getTs(), v.getValueAsString())).collect(Collectors.toList()));
                    }
                    msg.getResponseHolder().setResult(new ResponseEntity<>(data, HttpStatus.OK));
                } else if ("attributes".equals(entity)) {
                    String keys = request.getParameter("keys", "");
                    List<AttributeKvEntry> attributes;
                    if (!StringUtils.isEmpty(scope)) {
                        attributes = getAttributeKvEntries(ctx, scope, deviceId, keys);
                    } else {
                        attributes = getAttributeKvEntries(ctx, DataConstants.CLIENT_SCOPE, deviceId, keys);
                        attributes.addAll(getAttributeKvEntries(ctx, DataConstants.SHARED_SCOPE, deviceId, keys));
                        attributes.addAll(getAttributeKvEntries(ctx, DataConstants.SERVER_SCOPE, deviceId, keys));
                    }
                    List<AttributeData> values = attributes.stream().map(attribute -> new AttributeData(attribute.getLastUpdateTs(),
                            attribute.getKey(), attribute.getValue())).collect(Collectors.toList());
                    msg.getResponseHolder().setResult(new ResponseEntity<>(values, HttpStatus.OK));
                }
            }
        } else {
            msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        }
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
                            ctx.saveAttributes(deviceId, scope, attributes, new PluginCallback<Void>() {
                                @Override
                                public void onSuccess(PluginContext ctx, Void value) {
                                    msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.OK));
                                }

                                @Override
                                public void onFailure(PluginContext ctx, Exception e) {
                                    msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                                }
                            });
                            return;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Failed to process POST request due to IO exception", e);
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
                        DataConstants.SHARED_SCOPE.equals(scope)) {
                    String keysParam = request.getParameter("keys");
                    if (!StringUtils.isEmpty(keysParam)) {
                        String[] keys = keysParam.split(",");
                        ctx.removeAttributes(deviceId, scope, Arrays.asList(keys));
                        msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.OK));
                        return;
                    }
                }
            }
        } catch (RuntimeException e) {
            log.debug("Failed to process DELETE request due to Runtime exception", e);
        }
        msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    private List<AttributeKvEntry> getAttributeKvEntries(PluginContext ctx, String scope, DeviceId deviceId, String keysParam) {
        List<AttributeKvEntry> attributes;
        if (!StringUtils.isEmpty(keysParam)) {
            String[] keys = keysParam.split(",");
            attributes = ctx.loadAttributes(deviceId, scope, Arrays.asList(keys));
        } else {
            attributes = ctx.loadAttributes(deviceId, scope);
        }
        return attributes;
    }
}
