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
