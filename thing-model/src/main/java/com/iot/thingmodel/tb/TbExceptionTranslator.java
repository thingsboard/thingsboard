package com.iot.thingmodel.tb;

import com.iot.thingmodel.common.Constants;

public final class TbExceptionTranslator {

    private TbExceptionTranslator() {
    }

    public static TbSyncException translate(String action, org.thingsboard.client.ApiException ex) {
        String body = ex.getResponseBody();
        if (body != null && body.length() > Constants.ThingsBoard.ERROR_BODY_MAX_LENGTH) {
            body = body.substring(0, Constants.ThingsBoard.ERROR_BODY_MAX_LENGTH);
        }
        String message = "ThingsBoard " + action + " 失败: HTTP " + ex.getCode();
        if (body != null && !body.isBlank()) {
            message = message + " " + body;
        }
        return new TbSyncException(message, ex.getCode(), body);
    }
}
