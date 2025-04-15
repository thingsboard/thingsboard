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
package org.thingsboard.server.edqs.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class VersionsStore {

    private final Cache<String, Long> versions = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    public boolean isNew(String key, Long version) {
        AtomicBoolean isNew = new AtomicBoolean(false);
        versions.asMap().compute(key, (k, prevVersion) -> {
            if (prevVersion == null || prevVersion <= version) {
                isNew.set(true);
                return version;
            } else {
                log.info("[{}] Version {} is outdated, the latest is {}", key, version, prevVersion);
                return prevVersion;
            }
        });
        return isNew.get();
    }

}
