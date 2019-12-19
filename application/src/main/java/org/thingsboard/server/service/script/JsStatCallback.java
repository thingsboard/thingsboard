package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.FutureCallback;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public class JsStatCallback<T> implements FutureCallback<T> {

    private final AtomicInteger jsSuccessMsgs;
    private final AtomicInteger jsTimeoutMsgs;
    private final AtomicInteger jsFailedMsgs;


    @Override
    public void onSuccess(@Nullable T result) {
        jsSuccessMsgs.incrementAndGet();
    }

    @Override
    public void onFailure(Throwable t) {
        if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
            jsTimeoutMsgs.incrementAndGet();
        } else {
            jsFailedMsgs.incrementAndGet();
        }
    }
}
