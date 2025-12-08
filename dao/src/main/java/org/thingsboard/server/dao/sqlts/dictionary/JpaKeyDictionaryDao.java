/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
        Optional<KeyDictionaryEntry> entryOpt = keyDictionaryRepository.findById(compositeKey);
        if (entryOpt.isPresent()) {
            Integer keyId = entryOpt.get().getKeyId();
            if (keyId != null) {
                keyDictionaryMap.put(strKey, keyId);
                return keyId;
            }
        }
        creationLock.lock();
        try {
            Integer keyId = keyDictionaryMap.get(strKey);
            if (keyId != null) {
                return keyId;
            }
            keyId = keyDictionaryRepository.upsertAndGetKeyId(strKey);
            if (keyId == null || keyId == 0) {
                log.warn("upsertAndGetKeyId returned: [{}] for key: [{}], falling back to findById", keyId, strKey);
                entryOpt = keyDictionaryRepository.findById(compositeKey);
                if (entryOpt.isEmpty() ||
                    entryOpt.get().getKeyId() == null ||
                    entryOpt.get().getKeyId() == 0) {
                    throw new IllegalStateException("Failed to resolve keyId for string key: " + strKey + " after fallback. keyId: " + keyId);
                }
            }
            keyDictionaryMap.put(strKey, keyId);
            return keyId;
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

}
