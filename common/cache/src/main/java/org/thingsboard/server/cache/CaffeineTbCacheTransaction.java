package org.thingsboard.server.cache;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class CaffeineTbCacheTransaction implements TbCacheTransaction {
    @Getter
    private final UUID id = UUID.randomUUID();
    private final CaffeineCacheTransactionStorage cache;
    @Getter
    private final List<?> keys;
    @Getter @Setter
    private boolean failed;

    private final Map<Object, Object> pendingPuts = new LinkedHashMap<>();

    @Override
    public <K, V> void putIfAbsent(K key, V value) {
        pendingPuts.put(key, value);
    }

    @Override
    public boolean commit() {
        return cache.commit(id, pendingPuts);
    }

    @Override
    public void rollback() {
        cache.rollback(id);
    }

    @Override
    public <T> void rollBackOnFailure(ListenableFuture<T> future, Executor executor) {
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
            }

            @Override
            public void onFailure(Throwable t) {
                log.trace("[{}] Rollback transaction due to error", id, t);
                rollback();
            }
        }, executor);
    }

}
