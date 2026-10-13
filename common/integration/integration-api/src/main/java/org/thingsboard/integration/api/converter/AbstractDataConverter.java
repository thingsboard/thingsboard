// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.util.ExceptionUtil;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.ConverterDebugEvent;
import org.thingsboard.server.common.data.script.ScriptLanguage;

import static org.thingsboard.integration.api.util.ConvertUtil.toDebugMessage;

@Slf4j
public abstract class AbstractDataConverter implements TBDataConverter {

    private final LogSettingsComponent logSettings;
    private final JsInvokeService jsInvokeService;
    private final TbelInvokeService tbelInvokeService;
    protected Converter configuration;

    public AbstractDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        this.jsInvokeService = jsInvokeService;
        this.tbelInvokeService = tbelInvokeService;
        this.logSettings = logSettings;
    }

    protected ScriptInvokeService getScriptInvokeService(Converter configuration) {
        var cfgJson = configuration.getConfiguration();
        ScriptLanguage scriptLang = cfgJson.has("scriptLang") ? ScriptLanguage.valueOf(cfgJson.get("scriptLang").asText()) : ScriptLanguage.JS;
        ScriptInvokeService scriptInvokeService;
        if (ScriptLanguage.JS.equals(scriptLang)) {
            scriptInvokeService = jsInvokeService;
        } else {
            if (tbelInvokeService == null) {
                throw new RuntimeException("TBEL script engine is disabled!");
            } else {
                scriptInvokeService = tbelInvokeService;
            }
        }
        return scriptInvokeService;
    }


    @Override
    public void init(Converter configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : null;
    }

    protected String toString(Exception e) {
        return ExceptionUtil.toString(e, configuration.getId(),  logSettings.isExceptionStackTraceEnabled());
    }

    protected void persistDebug(ConverterContext context, String type, String inMessageType, byte[] inMessage,
                                String outMessageType, byte[] outMessage, String metadata, Exception exception) {
        var event = ConverterDebugEvent.builder()
                .tenantId(configuration.getTenantId())
                .entityId(configuration.getId().getId())
                .serviceId(context.getServiceId())
                .eventType(type)
                .inMsgType(inMessageType)
                .inMsg(toDebugMessage(inMessageType, inMessage))
                .outMsgType(outMessageType)
                .outMsg(toDebugMessage(outMessageType, outMessage))
                .metadata(metadata);
        if (exception != null) {
            event.error(toString(exception));
        }
        context.saveEvent(event.build(), new DebugEventCallback());
    }

    private static class DebugEventCallback implements IntegrationCallback<Void> {

        @Override
        public void onSuccess(Void msg) {
            if (log.isDebugEnabled()) {
                log.debug("Event has been saved successfully!");
            }
        }

        @Override
        public void onError(Throwable e) {
            log.error("Failed to save the debug event!", e);
        }
    }

}
