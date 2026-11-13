// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.integration.api.util.ConvertUtil;
import org.thingsboard.integration.api.util.ExceptionUtil;

import javax.script.ScriptException;

/**
 * Created by ashvayka on 04.12.17.
 */
@Slf4j
public abstract class AbstractHttpIntegration<T extends HttpIntegrationMsg<?>> extends AbstractIntegration<T> {

    @Override
    public void process(T msg) {
        String status = "OK";
        Exception exception = null;
        try {
            ResponseEntity httpResponse = doProcess(msg);
            if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                status = ((HttpStatus) httpResponse.getStatusCode()).name();
            }
            try {
                msg.getCallback().setResult(httpResponse);
            } catch (Exception e) {
                log.error("Failed to send response from integration to original HTTP request", e);
            }
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
            handleException(msg, e);
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        persistDebug(context, getTypeUplink(msg), msg.getContentType(),
                () -> ConvertUtil.toDebugMessage(msg.getContentType(), msg.getMsgInBytes()), status, exception);
    }

    private void handleException(T msg, Exception e) {
        HttpStatus status;
        Exception se = ExceptionUtil.lookupExceptionInCause(e, ScriptException.class, JsonParseException.class);
        if (se != null) {
            e = se;
            status = HttpStatus.BAD_REQUEST;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        msg.getCallback().setResult(context.isExceptionStackTraceEnabled() ? new ResponseEntity<>(toString(e), status) : new ResponseEntity<>(status));
    }

    protected abstract ResponseEntity doProcess(T msg) throws Exception;

    protected static ResponseEntity fromStatus(HttpStatus status) {
        return new ResponseEntity<>(status);
    }

    protected abstract String getTypeUplink(T msg);

    @Override
    public void destroy() {

    }
}
