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
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ActorSystemTest {

    public static final String ROOT_DISPATCHER = "root-dispatcher";
    private static final int _100K = 100 * 1024;
    public static final int TIMEOUT_AWAIT_MAX_SEC = 30;

    private volatile TbActorSystem actorSystem;
    private volatile ExecutorService submitPool;
    private ExecutorService executor;
    private int parallelism;

    @BeforeEach
    public void initActorSystem() {
        int cores = Runtime.getRuntime().availableProcessors();
        parallelism = Math.max(2, cores / 2);
        log.debug("parallelism {}", parallelism);
        TbActorSystemSettings settings = new TbActorSystemSettings(5, parallelism, 42);
        actorSystem = new DefaultTbActorSystem(settings);
        submitPool = Executors.newFixedThreadPool(parallelism, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-submit-test-scope")); //order guaranteed
    }

    @AfterEach
    public void shutdownActorSystem() {
        actorSystem.stop();
        submitPool.shutdownNow();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void test1actorsAnd100KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(1, _100K, 1);
    }

    @Test
    public void test10actorsAnd100KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(10, _100K, 1);
    }

    @Test
    public void test100KActorsAnd1Messages5timesSingleThread() throws InterruptedException {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 1, 5);
    }

    @Test
    public void test100KActorsAnd1Messages5times() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 1, 5);
    }

    @Test
    public void test100KActorsAnd10Messages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 10, 1);
    }

    @Test
    public void test1KActorsAnd1KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(1000, 1000, 10);
    }

    @Test
    public void testNoMessagesAfterDestroy() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);

        TbActorRef actorId1 = actorSystem.createRootActor(ROOT_DISPATCHER, new SlowInitActor.SlowInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx1));
        TbActorRef actorId2 = actorSystem.createRootActor(ROOT_DISPATCHER, new SlowInitActor.SlowInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx2));

        actorId1.tell(new IntTbActorMsg(42));
        actorId2.tell(new IntTbActorMsg(42));
        actorSystem.stop(actorId1);

        Assertions.assertTrue(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
        Assertions.assertFalse(testCtx1.getLatch().await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testOneActorCreated() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);
        assertThat(testCtx1.getLatch().getCount()).as("testCtx1 latch initial state").isEqualTo(1);
        assertThat(testCtx2.getLatch().getCount()).as("testCtx2 latch initial state").isEqualTo(1);
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch actorsReadyLatch = new CountDownLatch(2);
        submitPool.submit(() -> {
            log.info("submit 1");
            actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx1, initLatch));
            actorsReadyLatch.countDown();
            log.info("done 1");
        });
        submitPool.submit(() -> {
            log.info("submit 2");
            actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx2, initLatch));
            actorsReadyLatch.countDown();
            log.info("done 2");
        });

        initLatch.countDown(); //replacement for Thread.wait(500) in the SlowCreateActorCreator
        Assertions.assertTrue(actorsReadyLatch.await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));
        log.info("actorsReadyLatch ok");
        actorSystem.tell(actorId, new IntTbActorMsg(42));

        //only one of two contexts are initialized. no matter Ctx1 or Ctx2
        Awaitility.await("one of two actors latch zeroed").atMost(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS)
                        .until(() -> testCtx1.getLatch().getCount() + testCtx2.getLatch().getCount() == 1);
        Thread.yield();
        if (testCtx1.getLatch().getCount() == 0) {
            assertThat(testCtx2.getLatch().await(100, TimeUnit.MILLISECONDS)).as("testCtx2 never latched").isFalse();
        } else {
            assertThat(testCtx1.getLatch().await(100, TimeUnit.MILLISECONDS)).as("testCtx1 never latched").isFalse();
        }

    }

    @Test
    public void testActorCreatorCalledOnce() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx = getActorTestCtx(1);
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        final int actorsCount = 1000;
        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch actorsReadyLatch = new CountDownLatch(actorsCount);
        for (int i = 0; i < actorsCount; i++) {
            submitPool.submit(() -> {
                actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx, initLatch));
                actorsReadyLatch.countDown();
            });
        }
        initLatch.countDown();
        Assertions.assertTrue(actorsReadyLatch.await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));

        actorSystem.tell(actorId, new IntTbActorMsg(42));

        Assertions.assertTrue(testCtx.getLatch().await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));
        //One for creation and one for message
        Assertions.assertEquals(2, testCtx.getInvocationCount().get());
    }

    @Test
    public void testFailedInit() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);

        TbActorRef actorId1 = actorSystem.createRootActor(ROOT_DISPATCHER, new FailedToInitActor.FailedToInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx1, 1, 3000));
        TbActorRef actorId2 = actorSystem.createRootActor(ROOT_DISPATCHER, new FailedToInitActor.FailedToInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx2, 2, 1));

        actorId1.tell(new IntTbActorMsg(42));
        actorId2.tell(new IntTbActorMsg(42));

        Assertions.assertFalse(testCtx1.getLatch().await(2, TimeUnit.SECONDS));
        Assertions.assertTrue(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
        Assertions.assertTrue(testCtx1.getLatch().await(3, TimeUnit.SECONDS));
    }


    public void testActorsAndMessages(int actorsCount, int msgNumber, int times) throws InterruptedException {
        Random random = new Random();
        int[] randomIntegers = new int[msgNumber];
        long sumTmp = 0;
        for (int i = 0; i < msgNumber; i++) {
            int tmp = random.nextInt();
            randomIntegers[i] = tmp;
            sumTmp += tmp;
        }
        long expected = sumTmp;

        List<ActorTestCtx> testCtxes = new ArrayList<>();

        List<TbActorRef> actorRefs = new ArrayList<>();
        for (int actorIdx = 0; actorIdx < actorsCount; actorIdx++) {
            ActorTestCtx testCtx = getActorTestCtx(msgNumber);
            actorRefs.add(actorSystem.createRootActor(ROOT_DISPATCHER, new TestRootActor.TestRootActorCreator(
                    new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx)));
            testCtxes.add(testCtx);
        }

        for (int t = 0; t < times; t++) {
            long start = System.nanoTime();
            for (int i = 0; i < msgNumber; i++) {
                int tmp = randomIntegers[i];
                submitPool.execute(() -> actorRefs.forEach(actorId -> actorId.tell(new IntTbActorMsg(tmp))));
            }
            log.info("Submitted all messages");
            testCtxes.forEach(ctx -> {
                try {
                    boolean success = ctx.getLatch().await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS);
                    if (!success) {
                        log.warn("Failed: {}, {}", ctx.getActual().get(), ctx.getInvocationCount().get());
                    }
                    Assertions.assertTrue(success);
                    Assertions.assertEquals(expected, ctx.getActual().get());
                    Assertions.assertEquals(msgNumber, ctx.getInvocationCount().get());
                    ctx.clear();
                } catch (InterruptedException e) {
                    log.error("interrupted", e);
                }
            });
            long duration = System.nanoTime() - start;
            log.info("Time spend: {}ns ({} ms)", duration, TimeUnit.NANOSECONDS.toMillis(duration));
        }
    }

    private ActorTestCtx getActorTestCtx(int i) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicLong actual = new AtomicLong();
        AtomicInteger invocations = new AtomicInteger();
        return new ActorTestCtx(countDownLatch, invocations, i, actual);
    }
}
