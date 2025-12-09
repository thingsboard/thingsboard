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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class KeyDictionaryDaoTest extends AbstractServiceTest {

    @Autowired
    private KeyDictionaryDao keyDictionaryDao;

    @Autowired
    private KeyDictionaryRepository keyDictionaryRepository;

    private static final String KEY = "testKeyDictionaryDaoTestKey";

    @Test
    public void testGetOrSaveKeyId_concurrent() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CountDownLatch allReady = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(threads);

        Integer[] keyIds = new Integer[threads];

        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                executor.submit(() -> {
                    allReady.countDown();
                    try {
                        // wait until all threads are ready
                        start.await();
                        // concurrent call
                        Integer id = keyDictionaryDao.getOrSaveKeyId(KEY);
                        keyIds[idx] = id;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        allDone.countDown();
                    }
                });
            }

            // ensure all threads are queued
            allReady.await(5, TimeUnit.SECONDS);
            // fire the start gun
            start.countDown();
            // wait for all to finish
            allDone.await(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // basic sanity
        for (int i = 0; i < threads; i++) {
            assertThat(keyIds[i])
                    .as("keyId[%s]", i)
                    .isNotNull()
                    .isGreaterThan(0);
        }

        // all threads must see the same keyId
        int first = keyIds[0];
        assertThat(first).isGreaterThan(0);
        assertThat(Arrays.stream(keyIds).distinct().count())
                .as("all threads should get the same keyId")
                .isEqualTo(1);

        // DB must have exactly one row for this key and the same id
        KeyDictionaryCompositeKey id = new KeyDictionaryCompositeKey(KEY);
        Optional<KeyDictionaryEntry> entry = keyDictionaryRepository.findById(id);

        assertThat(entry.isPresent()).isTrue();
        assertThat(entry.get().getKeyId()).isEqualTo(first);

        keyDictionaryRepository.deleteById(id);
    }

}
