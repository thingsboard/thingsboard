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
package org.thingsboard.server.common.msg.tools;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.Getter;
import org.thingsboard.server.common.data.limit.RateLimitEntry;
import org.thingsboard.server.common.data.limit.RateLimitUtil;

import java.time.Duration;
import java.util.List;

/**
 * Created by ashvayka on 22.10.18.
 */
public class TbRateLimits {
    private final LocalBucket bucket;

    @Getter
    private final String configuration;

    public TbRateLimits(String limitsConfiguration) {
        this(limitsConfiguration, false);
    }

    public TbRateLimits(String limitsConfiguration, boolean refillIntervally) {
        List<RateLimitEntry> limitedApiEntries = RateLimitUtil.parseConfig(limitsConfiguration);
        if (limitedApiEntries.isEmpty()) {
            throw new IllegalArgumentException("Failed to parse rate limits configuration: " + limitsConfiguration);
        }
        LocalBucketBuilder localBucket = Bucket.builder();
        for (RateLimitEntry entry : limitedApiEntries) {
            BandwidthBuilder.BandwidthBuilderRefillStage bandwidthBuilder = Bandwidth.builder().capacity(entry.capacity());
            Bandwidth bandwidth = refillIntervally ?
                    bandwidthBuilder.refillIntervally(entry.capacity(), Duration.ofSeconds(entry.durationSeconds())).build() :
                    bandwidthBuilder.refillGreedy(entry.capacity(), Duration.ofSeconds(entry.durationSeconds())).build();
            localBucket.addLimit(bandwidth);
        }
        this.bucket = localBucket.build();
        this.configuration = limitsConfiguration;
    }

    public boolean tryConsume() {
        return bucket.tryConsume(1);
    }

    public boolean tryConsume(long number) {
        return bucket.tryConsume(number);
    }

}
