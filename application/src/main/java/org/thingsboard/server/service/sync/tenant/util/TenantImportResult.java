/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.tenant.util;

import lombok.Data;
import org.thingsboard.server.common.data.ObjectType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class TenantImportResult {
    private boolean done;
    private boolean success;
    private String error;

    private final Map<ObjectType, AtomicInteger> stats = new LinkedHashMap<>();

    public void report(ObjectType type) {
        stats.computeIfAbsent(type, k -> new AtomicInteger()).incrementAndGet();
    }

    public int getCount(ObjectType type) {
        return Optional.ofNullable(stats.get(type)).map(AtomicInteger::get).orElse(0);
    }

}
