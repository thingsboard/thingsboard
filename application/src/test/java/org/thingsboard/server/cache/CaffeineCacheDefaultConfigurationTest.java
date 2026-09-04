// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CaffeineCacheDefaultConfigurationTest.class, loader = SpringBootContextLoader.class)
@ComponentScan({"org.thingsboard.server.cache"})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "cache.specs.edgeSessions.timeToLiveInMinutes=1",
        "cache.specs.relatedEdges.maxSize=1"
})
@Slf4j
public class CaffeineCacheDefaultConfigurationTest {

    @Autowired
    CacheSpecsMap cacheSpecsMap;

    @Test
    public void verifyTransactionAwareCacheManagerProxy() {
        assertThat(cacheSpecsMap.getSpecs()).as("specs").isNotNull();
        cacheSpecsMap.getSpecs().forEach((name, cacheSpecs) -> assertThat(cacheSpecs).as("cache %s specs", name).isNotNull());

        SoftAssertions softly = new SoftAssertions();
        cacheSpecsMap.getSpecs().forEach((name, cacheSpecs) -> {
            softly.assertThat(name).as("cache name").isNotEmpty();
            softly.assertThat(cacheSpecs.getTimeToLiveInMinutes()).as("cache %s time to live", name).isGreaterThan(0);
            softly.assertThat(cacheSpecs.getMaxSize()).as("cache %s max size", name).isGreaterThan(0);
        });
        softly.assertAll();
    }

}
