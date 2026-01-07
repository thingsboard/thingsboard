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
package org.thingsboard.server.common.data.limit;

import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RateLimitUtil {

    public static List<RateLimitEntry> parseConfig(String config) {
        if (config == null || config.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(config.split(","))
                .map(RateLimitEntry::parse)
                .toList();
    }

    public static Function<DefaultTenantProfileConfiguration, String> merge(
            Function<DefaultTenantProfileConfiguration, String> configExtractor1,
            Function<DefaultTenantProfileConfiguration, String> configExtractor2) {
        return config -> {
            String config1 = configExtractor1.apply(config);
            String config2 = configExtractor2.apply(config);
            return RateLimitUtil.mergeStrConfigs(config1, config2); // merges the configs
        };
    }

    private static String mergeStrConfigs(String firstConfig, String secondConfig) {
        List<RateLimitEntry> all = new ArrayList<>();
        all.addAll(parseConfig(firstConfig));
        all.addAll(parseConfig(secondConfig));

        Map<Long, Long> merged = new HashMap<>();

        for (RateLimitEntry entry : all) {
            merged.merge(entry.durationSeconds(), entry.capacity(), Long::sum);
        }

        return merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // optional: sort by duration
                .map(e -> e.getValue() + ":" + e.getKey())
                .collect(Collectors.joining(","));
    }

    public static boolean isValid(String configStr) {
        List<RateLimitEntry> limitedApiEntries = parseConfig(configStr);
        Set<Long> distinctDurations = new HashSet<>();
        for (RateLimitEntry entry : limitedApiEntries) {
            if (!distinctDurations.add(entry.durationSeconds())) {
                return false;
            }
        }
        return true;
    }

}
