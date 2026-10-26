package com.iot.thingmodel.tb;

public class TbSyncException extends RuntimeException {

    private final int httpCode;
    private final String responseBody;

    public TbSyncException(String message, int httpCode, String responseBody) {
        super(message);
        this.httpCode = httpCode;
        this.responseBody = responseBody;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
