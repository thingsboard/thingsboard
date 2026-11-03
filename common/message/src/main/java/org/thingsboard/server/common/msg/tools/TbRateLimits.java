// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
