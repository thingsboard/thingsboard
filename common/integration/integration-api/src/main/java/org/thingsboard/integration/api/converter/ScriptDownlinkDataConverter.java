// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 02.12.17.
 */
public class ScriptDownlinkDataConverter extends AbstractDownlinkDataConverter {

    private ScriptDownlinkEvaluator evaluator;

    public ScriptDownlinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        super(jsInvokeService, tbelInvokeService, logSettings);
    }

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        ScriptInvokeService scriptInvokeService = getScriptInvokeService(configuration);
        String encoderField = ScriptLanguage.JS.equals(scriptInvokeService.getLanguage()) ? "encoder" : "tbelEncoder";
        String encoder = configuration.getConfiguration().get(encoderField).asText();
        this.evaluator = new ScriptDownlinkEvaluator(configuration.getTenantId(), scriptInvokeService, configuration.getId(), encoder);
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
    protected JsonNode doConvertDownlink(TbMsg msg, IntegrationMetaData metadata) throws Exception {
        return evaluator.execute(msg, metadata);
    }

}
