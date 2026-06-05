/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sqlts.dictionary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@RequiredArgsConstructor
public class JpaKeyDictionaryDao extends JpaAbstractDaoListeningExecutorService implements KeyDictionaryDao {

    private final KeyDictionaryRepository keyDictionaryRepository;

    private final ConcurrentMap<String, Integer> keyDictionaryMap = new ConcurrentHashMap<>();
    private static final ReentrantLock creationLock = new ReentrantLock();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public Integer getOrSaveKeyId(String strKey) {
        Integer cached = keyDictionaryMap.get(strKey);
        if (cached != null) {
            return cached;
        }
        var compositeKey = new KeyDictionaryCompositeKey(strKey);
        Optional<Integer> existingId = keyDictionaryRepository.findById(compositeKey).map(KeyDictionaryEntry::getKeyId);
        if (existingId.isPresent()) {
            return cacheAndReturn(strKey, existingId.get());
        }
        creationLock.lock();
        try {
            Integer fromCache = keyDictionaryMap.get(strKey);
            if (fromCache != null) {
                return fromCache;
            }
            Integer keyId = keyDictionaryRepository.upsertAndGetKeyId(strKey);
            if (keyId != null) {
                return cacheAndReturn(strKey, keyId);
            }
            log.warn("upsertAndGetKeyId returned: [{}] for key: [{}], falling back to findById", keyId, strKey);
            keyId = keyDictionaryRepository.findById(compositeKey)
                    .map(KeyDictionaryEntry::getKeyId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to resolve keyId for string key: " + strKey + " after fallback."));
            return cacheAndReturn(strKey, keyId);
        } finally {
            creationLock.unlock();
        }
    }

    @Override
    public String getKey(Integer keyId) {
        Optional<KeyDictionaryEntry> byKeyId = keyDictionaryRepository.findByKeyId(keyId);
        return byKeyId.map(KeyDictionaryEntry::getKey).orElse(null);
    }

    @Override
    public PageData<KeyDictionaryEntry> findAll(PageLink pageLink) {
        return DaoUtil.pageToPageData(keyDictionaryRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    private Integer cacheAndReturn(String key, Integer keyId) {
        keyDictionaryMap.put(key, keyId);
        return keyId;
    }

}
