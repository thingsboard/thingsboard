package org.thingsboard.server.cache;

import org.springframework.cache.Cache;

import java.io.Serializable;
import java.util.List;

public interface TbTransactionalCache {

    <K extends Serializable> Cache.ValueWrapper get(String cacheName, K key);

    <K extends Serializable, V extends Serializable> void putIfAbsent(String cacheName, K key, V value);

    <K extends Serializable> void evict(String cacheName, K key);

    <K extends Serializable> TbCacheTransaction newTransactionForKey(String cacheName, K key);

    <K extends Serializable> TbCacheTransaction newTransactionForKeys(String cacheName, List<K> keys);

}
