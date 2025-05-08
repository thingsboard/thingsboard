/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.model;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2MModelConfigServiceImpl implements LwM2MModelConfigService {

    @Autowired
    TbLwM2MModelConfigStore modelStore;

    @Autowired
    @Lazy
    private LwM2mDownlinkMsgHandler downlinkMsgHandler;
    @Autowired
    @Lazy
    private LwM2mUplinkMsgHandler uplinkMsgHandler;
    @Autowired
    @Lazy
    private LwM2mClientContext clientContext;

    @Autowired
    private LwM2MTelemetryLogService logService;

    ConcurrentMap<String, LwM2MModelConfig> currentModelConfigs;

    @AfterStartUp(order = AfterStartUp.BEFORE_TRANSPORT_SERVICE)
    public void init() {
        List<LwM2MModelConfig> models = modelStore.getAll();
        log.debug("Fetched model configs: {}", models);
        currentModelConfigs = models.stream()
                .collect(Collectors.toConcurrentMap(LwM2MModelConfig::getEndpoint, m -> m, (existing, replacement) -> existing));
    }

    @Override
    public void sendUpdates(LwM2mClient lwM2mClient) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(lwM2mClient.getEndpoint());
        if (modelConfig == null || modelConfig.isEmpty()) {
            return;
        }

        doSend(lwM2mClient, modelConfig);
    }

    public void sendUpdates(LwM2mClient lwM2mClient, LwM2MModelConfig newModelConfig) {
        String endpoint = lwM2mClient.getEndpoint();
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        if (modelConfig == null || modelConfig.isEmpty()) {
            modelConfig = newModelConfig;
            currentModelConfigs.put(endpoint, modelConfig);
        } else {
            modelConfig.merge(newModelConfig);
        }

        if (lwM2mClient.isAsleep()) {
            modelStore.put(modelConfig);
        } else {
            doSend(lwM2mClient, modelConfig);
        }
    }

    private void doSend(LwM2mClient lwM2mClient, LwM2MModelConfig modelConfig) {
        log.trace("Send LwM2M Model updates: [{}]", modelConfig);

        String endpoint = lwM2mClient.getEndpoint();

        Map<String, ObjectAttributes> attrToAdd = modelConfig.getAttributesToAdd();
        attrToAdd.forEach((id, attributes) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(attributes)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToAdd.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> attrToRemove = modelConfig.getAttributesToRemove();
        attrToRemove.forEach((id) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(new ObjectAttributes())
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToRemove.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> toRead = modelConfig.getToRead();
        toRead.forEach(id -> {
            TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendReadRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toRead.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MReadCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toObserve = modelConfig.getToObserve();
        toObserve.forEach(id -> {
            TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toObserve.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MObserveCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toCancelObserve = modelConfig.getToCancelObserve();
        toCancelObserve.forEach(id -> {
            TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendCancelObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toCancelObserve.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MCancelObserveCallback(logService, lwM2mClient, id))
            );
        });
    }

    private <R, T> DownlinkRequestCallback<R, T> createDownlinkProxyCallback(Runnable processRemove, DownlinkRequestCallback<R, T> callback) {
        return new DownlinkRequestCallback<>() {
            @Override
            public void onSuccess(R request, T response) {
                processRemove.run();
                callback.onSuccess(request, response);
            }

            @Override
            public void onValidationError(String params, String msg) {
                processRemove.run();
                callback.onValidationError(params, msg);
            }

            @Override
            public void onError(String params, Exception e) {
                try {
                    if (e instanceof TimeoutException) {
                        return;
                    }
                    processRemove.run();
                } finally {
                    callback.onError(params, e);
                }
            }

        };
    }

    @Override
    public void persistUpdates(String endpoint) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        if (modelConfig != null && !modelConfig.isEmpty()) {
            modelStore.put(modelConfig);
        }
    }

    @Override
    public void removeUpdates(String endpoint) {
        currentModelConfigs.remove(endpoint);
    }

    @PreDestroy
    private void destroy() {
        currentModelConfigs.values().forEach(model -> {
            if (model != null && !model.isEmpty()) {
                modelStore.put(model);
            }
        });
    }
}
