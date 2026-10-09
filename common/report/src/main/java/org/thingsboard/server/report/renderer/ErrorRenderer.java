// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.report.configuration.components.ErrorComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class ErrorRenderer implements PdfReportComponentRenderer<ErrorComponent> {

    @Override
    public String render(ErrorComponent errorComponent, ComponentData reportDataSource) {
        Map<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("errorMessage", errorComponent.getErrorMessage());

        Exception exception = extractRootException(errorComponent.getException());
        if (exception != null) {
            componentVariables.put("exception", formatExceptionMessage(exception));
        }

        return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", componentVariables);
    }

    private Exception extractRootException(Exception exception) {
        if (exception instanceof RuntimeException runtimeException && runtimeException.getCause() instanceof Exception cause) {
            return cause;
        }
        return exception;
    }

    private String formatExceptionMessage(Exception exception) {
        if (exception instanceof ThingsboardException tbException) {
            return "[" + tbException.getErrorCode().name() + "] " + tbException.getMessage();
        }
        return exception.getMessage();
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ERROR;
    }

}
