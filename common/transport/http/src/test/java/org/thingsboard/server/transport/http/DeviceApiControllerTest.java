// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.http;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceApiControllerTest {

    @Test
    void deviceAuthCallbackTest() {
        TransportContext transportContext = Mockito.mock(TransportContext.class);
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        Consumer<TransportProtos.SessionInfoProto> onSuccess = x -> {
        };
        var callback = new DeviceApiController.DeviceAuthCallback(transportContext, responseWriter, onSuccess);

        callback.onError(new HttpMessageNotReadableException("JSON incorrect syntax", (HttpInputMessage) null));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }

    @Test
    void deviceProvisionCallbackTest() {
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        var callback = new DeviceApiController.DeviceProvisionCallback(responseWriter);

        callback.onError(new HttpMessageNotReadableException("JSON incorrect syntax", (HttpInputMessage) null));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }

    @Test
    void getOtaPackageCallback() {
        TransportContext transportContext = Mockito.mock(TransportContext.class);
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        String title = "Title";
        String version = "version";
        int chunkSize = 11;
        int chunk = 3;

        var callback = new DeviceApiController.GetOtaPackageCallback(transportContext, responseWriter, title, version, chunkSize, chunk);

        callback.onError(new HttpMessageNotReadableException("JSON incorrect syntax", (HttpInputMessage) null));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }

    @Test
    void getOtaPackageCallbackWithCommaInFileName() {
        UUID otaPackageId = UUID.randomUUID();
        OtaPackageDataCache otaPackageDataCache = Mockito.mock(OtaPackageDataCache.class);
        Mockito.when(otaPackageDataCache.get(otaPackageId.toString(), 0, 0)).thenReturn(new byte[]{1, 2, 3});
        TransportContext transportContext = Mockito.mock(TransportContext.class);
        Mockito.when(transportContext.getOtaPackageDataCache()).thenReturn(otaPackageDataCache);
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);

        var callback = new DeviceApiController.GetOtaPackageCallback(transportContext, responseWriter, "Title", "1.0", 0, 0);
        callback.onSuccess(TransportProtos.GetOtaPackageResponseMsg.newBuilder()
                .setResponseStatus(TransportProtos.ResponseStatus.SUCCESS)
                .setOtaPackageIdMSB(otaPackageId.getMostSignificantBits())
                .setOtaPackageIdLSB(otaPackageId.getLeastSignificantBits())
                .setTitle("Title")
                .setVersion("1.0")
                .setContentType("application/octet-stream")
                .setFileName("Baker Street, 221B.bin")
                .build());

        ArgumentCaptor<ResponseEntity> response = ArgumentCaptor.forClass(ResponseEntity.class);
        Mockito.verify(responseWriter).setResult(response.capture());
        assertThat(response.getValue().getHeaders().get(HttpHeaders.CONTENT_DISPOSITION))
                .containsExactly("attachment; filename=\"Baker Street, 221B.bin\"");
    }
}
