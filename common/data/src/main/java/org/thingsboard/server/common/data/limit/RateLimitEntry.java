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

public record RateLimitEntry(long capacity, long durationSeconds) {

    public static RateLimitEntry parse(String s) {
        String[] parts = s.split(":");
        return new RateLimitEntry(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }

    @Override
    public String toString() {
        return capacity + ":" + durationSeconds;
    }

}
