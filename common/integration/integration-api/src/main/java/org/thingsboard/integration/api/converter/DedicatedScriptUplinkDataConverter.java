// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DedicatedScriptUplinkDataConverter extends AbstractUplinkDataConverter {

    private ScriptUplinkEvaluator evaluator;
    private DedicatedConverterConfig config;

    public DedicatedScriptUplinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        super(jsInvokeService, tbelInvokeService, logSettings);
    }

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        this.config = JacksonUtil.treeToValue(configuration.getConfiguration(), DedicatedConverterConfig.class);
        ScriptInvokeService scriptInvokeService = getScriptInvokeService(configuration);
        String decoderField = ScriptLanguage.JS.equals(scriptInvokeService.getLanguage()) ? "decoder" : "tbelDecoder";
        String decoder = configuration.getConfiguration().get(decoderField).asText();
        this.evaluator = new ScriptUplinkEvaluator(configuration.getTenantId(), scriptInvokeService, configuration.getId(), decoder);
    }

    @Override
    public void update(Converter configuration) {
        destroy();
        init(configuration);
    }

    @Override
    public void destroy() {
        if (this.evaluator != null) {
            this.evaluator.destroy();
        }
    }

    @Override
    public ListenableFuture<String> doConvertUplink(byte[] data, UplinkMetaData metadata) throws Exception {
        return evaluator.execute(data, metadata);
    }

    @Override
    protected UplinkData parseUplinkData(JsonObject src, UplinkMetaData metadata) {
        DedicatedUplinkData uplinkData = DedicatedConverterUtil.parseUplinkData(config, src, metadata);

        boolean isAsset = uplinkData.getEntityType() == EntityType.ASSET;

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.isAsset(isAsset);

        String name = uplinkData.getName();
        String label = uplinkData.getLabel();
        String profile = uplinkData.getProfile();
        String customer = uplinkData.getCustomer();
        String group = uplinkData.getGroup();

        if (isAsset) {
            builder.assetName(name);
            builder.assetType(profile);
            if (label != null) {
                builder.assetLabel(label);
            }
        } else {
            builder.deviceName(name);
            builder.deviceType(profile);
            if (label != null) {
                builder.deviceLabel(label);
            }
        }

        if (customer != null) {
            builder.customerName(customer);
        }
        if (group != null) {
            builder.groupName(group);
        }

        Map<String, String> currentOnValueTelemetryUpdate = this.currentUpdateOnlyTelemetryPerEntity.getOrDefault(name, new ConcurrentHashMap<>());
        Map<String, String> currentOnValueAttributesUpdate = this.currentUpdateOnlyAttributesPerEntity.getOrDefault(name, new ConcurrentHashMap<>());

        TransportProtos.PostTelemetryMsg parsedTelemetry = parseTelemetry(uplinkData.getTelemetryJson());
        if (!this.updateOnlyKeys.isEmpty()) {
            parsedTelemetry = filterTelemetryOnKeyValueUpdateAndUpdateMap(parsedTelemetry, currentOnValueTelemetryUpdate);
        }
        builder.telemetry(parsedTelemetry);

        TransportProtos.PostAttributeMsg parsedAttributes = parseAttributesUpdate(uplinkData.getAttributesJson());
        if (!this.updateOnlyKeys.isEmpty()) {
            parsedAttributes = filterAttributeOnKeyValueUpdateAndUpdateMap(parsedAttributes, currentOnValueAttributesUpdate);
        }
        builder.attributesUpdate(parsedAttributes);

        if (!currentOnValueTelemetryUpdate.isEmpty()) {
            this.currentUpdateOnlyTelemetryPerEntity.put(name, currentOnValueTelemetryUpdate);
        }

        if (!currentOnValueAttributesUpdate.isEmpty()) {
            this.currentUpdateOnlyAttributesPerEntity.put(name, currentOnValueAttributesUpdate);
        }

        return builder.build();
    }

}
