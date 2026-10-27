package com.iot.thingmodel.tb;

import com.iot.thingmodel.common.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TbExceptionTranslatorTest {

    @Test
    void translatesStatusAndBody() {
        TbSyncException ex = TbExceptionTranslator.translate(
                "同步模板", new org.thingsboard.client.ApiException(
                        "message", 500, null, "boom"));

        assertEquals(500, ex.getHttpCode());
        assertEquals("boom", ex.getResponseBody());
        assertTrue(ex.getMessage().contains("同步模板"));
        assertTrue(ex.getMessage().contains("HTTP 500"));
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    void truncatesLongBody() {
        String body = "x".repeat(Constants.ThingsBoard.ERROR_BODY_MAX_LENGTH + 100);

        TbSyncException ex = TbExceptionTranslator.translate(
                "同步", new org.thingsboard.client.ApiException(
                        "message", 500, null, body));

        assertEquals(Constants.ThingsBoard.ERROR_BODY_MAX_LENGTH,
                ex.getResponseBody().length());
    }

    @Test
    void omitsBlankBody() {
        TbSyncException ex = TbExceptionTranslator.translate(
                "同步", new org.thingsboard.client.ApiException(
                        "message", 401, null, ""));

        assertEquals(401, ex.getHttpCode());
        assertTrue(ex.getResponseBody() == null || ex.getResponseBody().isBlank());
        assertEquals("ThingsBoard 同步 失败: HTTP 401", ex.getMessage());
    }
}
