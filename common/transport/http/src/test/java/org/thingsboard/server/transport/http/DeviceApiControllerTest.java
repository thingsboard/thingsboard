package org.thingsboard.server.transport.http;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class DeviceApiControllerTest {

    @Test
    void callbackOnErrorTest() {
        TransportContext transportContext = Mockito.mock(TransportContext.class);
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        Consumer<TransportProtos.SessionInfoProto> onSuccess = x -> {};
        var callback = new DeviceApiController.DeviceAuthCallback(transportContext, responseWriter, onSuccess);

        callback.onError(new HttpMessageNotReadableException("JSON incorect syntax"));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }
}