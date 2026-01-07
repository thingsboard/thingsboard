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
package org.thingsboard.server.common.data.kv;

import lombok.Data;

@Data
public class BaseDeleteTsKvQuery extends BaseTsKvQuery implements DeleteTsKvQuery {

    private final Boolean rewriteLatestIfDeleted;
    private final Boolean deleteLatest;

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs, boolean rewriteLatestIfDeleted, boolean deleteLatest) {
        super(key, startTs, endTs);
        this.rewriteLatestIfDeleted = rewriteLatestIfDeleted;
        this.deleteLatest = deleteLatest;
    }

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs, boolean rewriteLatestIfDeleted) {
        this(key, startTs, endTs, rewriteLatestIfDeleted, true);
    }

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, false, true);
    }

}
