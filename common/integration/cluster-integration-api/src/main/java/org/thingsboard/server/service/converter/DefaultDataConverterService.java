// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.converter.DedicatedScriptUplinkDataConverter;
import org.thingsboard.integration.api.converter.ScriptDownlinkDataConverter;
import org.thingsboard.integration.api.converter.ScriptUplinkDataConverter;
import org.thingsboard.integration.api.converter.TBDataConverter;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;
import org.thingsboard.server.service.integration.EventStorageService;
import org.thingsboard.server.service.integration.RemoteIntegrationRpcService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 02.12.17.
 */
@TbCoreOrIntegrationExecutorComponent
@Service
public class DefaultDataConverterService implements DataConverterService {

    @Autowired
    private ConverterLookupService converterService;

    @Autowired
    private JsInvokeService jsInvokeService;

    @Autowired(required = false)
    private TbelInvokeService tbelInvokeService;

    @Autowired
    private LogSettingsComponent logSettingsComponent;

    @Autowired(required = false)
    private RemoteIntegrationRpcService rpcService;

    @Autowired
    private EventStorageService eventStorageService;

    private final ConcurrentMap<ConverterId, TBDataConverter> convertersByIdMap = new ConcurrentHashMap<>();

    @PreDestroy
    public void destroy() {
        convertersByIdMap.values().forEach(TBDataConverter::destroy);
    }

    @Override
    public TBDataConverter createConverter(Converter converter) {
        // TODO: This still may cause converter to initialize multiple times, even if one converter will be in the map. Need to improve this later.
        return convertersByIdMap.computeIfAbsent(converter.getId(), c -> initConverter(converter));
    }

    @Override
    public TBDataConverter updateConverter(Converter configuration) {
        if (rpcService != null) {
            rpcService.updateConverter(configuration);
        }
        TBDataConverter converter = convertersByIdMap.get(configuration.getId());
        if (converter != null) {
            converter.update(configuration);
            eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.UPDATED, null);
            return converter;
        } else {
            return createConverter(configuration);
        }
    }

    @Override
    public void deleteConverter(ConverterId converterId) {
        TBDataConverter converter = convertersByIdMap.remove(converterId);
        if (converter != null) {
            converter.destroy();
        }
    }

    @Override
    public Optional<TBUplinkDataConverter> getUplinkConverterById(TenantId tenantId, ConverterId converterId) {
        return Optional.of((TBUplinkDataConverter) getConverterById(tenantId, converterId));
    }

    @Override
    public Optional<TBDownlinkDataConverter> getDownlinkConverterById(TenantId tenantId, ConverterId converterId) {
        return Optional.ofNullable((TBDownlinkDataConverter) getConverterById(tenantId, converterId));
    }

    private TBDataConverter getConverterById(TenantId tenantId, ConverterId converterId) {
        if (converterId == null) return null;
        TBDataConverter converter = convertersByIdMap.get(converterId);
        if (converter == null) {
            Converter configuration = converterService.findConverterById(tenantId, converterId);
            if (configuration != null) {
                converter = createConverter(configuration);
            }
        }
        return converter;
    }

    private TBDataConverter initConverter(Converter converter) {
        var dataConverter = switch (converter.getType()) {
            case UPLINK -> {
                if (converter.isDedicated()) {
                    yield new DedicatedScriptUplinkDataConverter(jsInvokeService, tbelInvokeService, logSettingsComponent);
                } else {
                    yield new ScriptUplinkDataConverter(jsInvokeService, tbelInvokeService, logSettingsComponent);
                }
            }
            case DOWNLINK -> new ScriptDownlinkDataConverter(jsInvokeService, tbelInvokeService, logSettingsComponent);
        };
        dataConverter.init(converter);
        return dataConverter;
    }
}
