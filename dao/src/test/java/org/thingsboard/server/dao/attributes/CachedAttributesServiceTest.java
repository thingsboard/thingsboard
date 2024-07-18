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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.FstStatsService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.dao.AbstractJpaDaoTest;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "cache.type=redis",
        "redis.connection.type=standalone",
        "cache.maximumPoolSize=8",
        "redis.standalone.usePoolConfig=true",
        "redis.pool_config.testOnBorrow=false",
        "redis.pool_config.testOnReturn=false",
        "redis.pool_config.testWhileIdle=false",
})
@Slf4j
public class CachedAttributesServiceTest extends AbstractJpaDaoTest {

    @MockBean(answer = Answers.RETURNS_MOCKS)
    FstStatsService fstStatsService;
    @Autowired
    CachedAttributesService cachedAttributesService;

    @Before
    public void setUp() {
    }

    @Test
    public void saveOne() throws ExecutionException, InterruptedException, TimeoutException {
        AttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(new LongDataEntry("temp", 100L), System.currentTimeMillis());
        EntityId deviceId = new DeviceId(UUID.randomUUID());
        ListenableFuture<String> future = cachedAttributesService.save(TenantId.SYS_TENANT_ID, deviceId, AttributeScope.SERVER_SCOPE, attributeKvEntry);
        future.get(30, TimeUnit.SECONDS);
    }

    @Test
    public void save100k() throws ExecutionException, InterruptedException, TimeoutException {
        log.warn("generating devices");
        Random random = new Random(0);
        AtomicLong progress = new AtomicLong();
        List<DeviceId> devices = IntStream.range(0, 500_000).mapToObj(x -> new DeviceId(new UUID(random.nextLong(), random.nextLong()))).toList();
        log.warn("Saving attrs devices");
        var futures = devices.stream().parallel()
                .map(dev -> {
                    var f = cachedAttributesService.save(TenantId.SYS_TENANT_ID, dev, AttributeScope.SERVER_SCOPE, new BaseAttributeKvEntry(new LongDataEntry("ts", System.nanoTime()), System.currentTimeMillis()));
                    DonAsynchron.withCallback(f, x -> progress.incrementAndGet(), t -> {});
                    return f;
                })
                .collect(Collectors.toList());
        log.warn("Awaiting results...");
        ListenableFuture<List<String>> resultFutures = Futures.allAsList(futures);
        while (!resultFutures.isDone()) {
            Thread.sleep(1000);
            log.warn("Progress is {}", progress.get());
        }
        log.warn("Done.");
    }

}
