/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

@Data
public class CassandraTsPartitionsCache {

    private LoadingCache<CassandraPartitionCacheKey, Boolean> partitionsCache;

    public CassandraTsPartitionsCache(long maxCacheSize) {
        this.partitionsCache = CacheBuilder.newBuilder()
                .maximumSize(maxCacheSize)
                .build(new PartitionsCacheLoader());
    }

    public boolean getIfPresent(CassandraPartitionCacheKey key) {
        return BooleanUtils.toBooleanDefaultIfNull(partitionsCache.getIfPresent(key), false);
    }

    public void put(CassandraPartitionCacheKey key) {
        partitionsCache.put(key, true);
    }

    private static class PartitionsCacheLoader extends CacheLoader<CassandraPartitionCacheKey, Boolean> {

        @Override
        public Boolean load(@NotNull CassandraPartitionCacheKey partitionCacheKey) throws Exception {
            return true;
        }
    }


}
