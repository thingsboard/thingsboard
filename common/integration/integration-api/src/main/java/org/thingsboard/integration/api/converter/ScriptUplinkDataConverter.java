// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.script.ScriptLanguage;

public class ScriptUplinkDataConverter extends AbstractUplinkDataConverter {

    private ScriptUplinkEvaluator evaluator;

    public ScriptUplinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        super(jsInvokeService, tbelInvokeService, logSettings);
    }

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
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

}
