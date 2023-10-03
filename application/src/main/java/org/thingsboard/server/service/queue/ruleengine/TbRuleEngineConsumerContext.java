package org.thingsboard.server.service.queue.ruleengine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
@TbRuleEngineComponent
@Slf4j
@Data
public class TbRuleEngineConsumerContext {

    @Value("${queue.rule-engine.poll-interval}")
    private long pollDuration;
    @Value("${queue.rule-engine.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.rule-engine.stats.enabled:true}")
    private boolean statsEnabled;
    @Value("${queue.rule-engine.prometheus-stats.enabled:false}")
    boolean prometheusStatsEnabled;
    @Value("${queue.rule-engine.topic-deletion-delay:30}")
    private int topicDeletionDelayInSec;

    //TODO: check if they are set correctly.
    protected volatile boolean stopped = false;
    protected volatile boolean isReady = false;

    private final ActorSystemContext actorContext;
    private final StatsFactory statsFactory;
    private final TbRuleEngineSubmitStrategyFactory submitStrategyFactory;
    private final TbRuleEngineProcessingStrategyFactory processingStrategyFactory;
    private final TbRuleEngineQueueFactory queueFactory;
    private final RuleEngineStatisticsService statisticsService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final QueueService queueService;
    private final TbQueueProducerProvider producerProvider;
    private final TbQueueAdmin queueAdmin;

    //TODO: add reasonable limit for mgmt pool.
    private final ExecutorService mgmtExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-rule-engine-mgmt"));
    private final ExecutorService consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer"));
    //TODO: do we actually need this?
    private final ExecutorService submitExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer-submit"));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer-scheduler"));

    public List<Queue> findAllQueues() {
        return queueService.findAllQueues();
    }

    @PreDestroy
    public void stop() {
        mgmtExecutor.shutdownNow();
        consumersExecutor.shutdownNow(); // TODO: shutdown or shutdownNow?
        submitExecutor.shutdownNow();
        scheduler.shutdownNow();
    }
}
