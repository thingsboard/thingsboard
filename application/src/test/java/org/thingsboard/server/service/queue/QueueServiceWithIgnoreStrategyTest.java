package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.thingsboard.rule.engine.api.TbMsgQueueService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = QueueServiceWithIgnoreStrategyTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"org.thingsboard.server"})
@WebAppConfiguration
@SpringBootTest(
        properties = {
                "backpressure.type:memory",
                "backpressure.strategy:ignore",
                "backpressure.timeout:10",
                "pack_size:1000"
        })
@Slf4j
public class QueueServiceWithIgnoreStrategyTest {

    @Autowired
    private TbMsgQueueService queueService;

    @Mock
    private ActorService actorService;

    private static final int POOL_SIZE = 10;
    private static final int MSG_SIZE = 200;
    private static final AtomicInteger SENT_MSG_COUNT = new AtomicInteger(0);

    private ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);

    private CountDownLatch latch;

    private void sendMsgs() {
        final TenantId tenantId = TenantId.SYS_TENANT_ID;
        final TbMsgMetaData metaData = new TbMsgMetaData();

        for (int i = 0; i < POOL_SIZE; i++) {
            executorService.submit(() -> {
                DeviceId deviceId = new DeviceId(UUID.randomUUID());
                for (int j = 0; j < MSG_SIZE; j++) {
                    queueService.add(
                            new TbMsg(UUID.randomUUID(), null, deviceId, metaData, null, null, null, null, 0),
                            tenantId);
                    SENT_MSG_COUNT.incrementAndGet();
                }
            });
        }
    }

    @Test
    public void testQueueAck() {
        final AtomicInteger msgCount = new AtomicInteger(0);
        latch = new CountDownLatch(POOL_SIZE * MSG_SIZE);

        Whitebox.setInternalState(queueService, "actorService", actorService);

        doAnswer((arg) -> {
            TbMsgQueuePack pack = arg.getArgument(0);
            pack.getMsgs().values()
                    .parallelStream()
                    .forEach(msg -> {
                        msgCount.incrementAndGet();
                        latch.countDown();
                    });
            return null;
        })
                .when(actorService)
                .onMsgFromTbMsgQueue(any(TbMsgQueuePack.class));

        sendMsgs();

        try {
            latch.await(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }
        assertEquals(SENT_MSG_COUNT.get(), msgCount.get());
    }

    @PreDestroy
    private void destroy() {
        executorService.shutdown();
    }
}
