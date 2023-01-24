/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.cache.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
public class DefaultRuleNodeCache implements RuleNodeCache {

    private final Map<String, Set<byte[]>> cache;

    public DefaultRuleNodeCache() {
        cache = new HashMap<>();
    }

    @Override
    public void add(String key, byte[]... values) {
        Set<byte[]> data = cache.computeIfAbsent(key, s -> new TreeSet<>(Arrays::compare));
        data.addAll(Arrays.asList(values));
    }

    @Override
    public void remove(String key, byte[]... values) {
        cache.computeIfPresent(key, (id, data) -> {
            Arrays.asList(values).forEach(data::remove);
            return data;
        });
    }

    @Override
    public Set<byte[]> get(String key) {
        return cache.getOrDefault(key, Collections.emptySet());
    }

    @Override
    public void evict(String key) {
        cache.remove(key);
    }

}
