package org.thingsboard.server.cache;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public interface TbCacheTransaction {

    <K,V> void putIfAbsent(K key, V value);

    boolean commit();

    void rollback();

    <T> void rollBackOnFailure(ListenableFuture<T> result, Executor cacheExecutor);
}
