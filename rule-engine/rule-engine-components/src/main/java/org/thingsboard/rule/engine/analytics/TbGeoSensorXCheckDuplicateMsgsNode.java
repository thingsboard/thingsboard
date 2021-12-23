package org.thingsboard.rule.engine.analytics;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceQueue;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.msg.queue.ServiceQueue.MAIN;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "duplicate msg",
        configClazz = TbGeoSensorXCheckDuplicateMsgsNodeConfiguration.class,
        nodeDescription = "analyzing rule engine exceptions for TbServiceQueues",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "functions"
)
public class TbGeoSensorXCheckDuplicateMsgsNode implements TbNode {

    private TbGeoSensorXCheckDuplicateMsgsNodeConfiguration config;
    private final static JsonParser parser = new JsonParser();
    private static final Gson gson = new Gson();

    private static final CronTrigger CRON_TRIGGER = new CronTrigger("0 * * ? * *", TimeZone.getTimeZone(ZoneId.of("America/Los_Angeles")));

    private ConcurrentHashMap<EntityId, Long> duplicateMsgs = new ConcurrentHashMap<>();
    private static final String TICK_MSG_TYPE = "CHECK_DUPLICATE_MSG_TYPE";
    private static final String RESULT_MSG_TYPE = "DUPLICATE_MSG_ANALYTICS_RESULT";

    private ScheduledExecutorService executorService;
    private ThreadPoolTaskScheduler checkDuplicateMsgsScheduler;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGeoSensorXCheckDuplicateMsgsNodeConfiguration.class);
        try {
            executorService = Executors.newScheduledThreadPool(10);
            executorService.scheduleAtFixedRate(() -> {
                List<Customer> customers = getCustomers(ctx);
                List<ListenableFuture<Void>> attributesTransformFuturesList = new ArrayList<>();
                for (Customer customer : customers) {
                    ListenableFuture<Optional<AttributeKvEntry>> duplicateMsgsConfig = ctx.getAttributesService().find(ctx.getTenantId(), customer.getId(), DataConstants.SERVER_SCOPE, "DG800_duplicateMsgsConfig");
                        Futures.transform(duplicateMsgsConfig,input -> {},)
                    attributesTransformFuturesList.add(duplicateMsgsConfig);
                    log.info("Customer: " + customer);
                }
                ListenableFuture<List<Void>> duplicateMsgsConfigFutureList = Futures.allAsList(attributesTransformFuturesList);
                DonAsynchron.withCallback(duplicateMsgsConfigFutureList, value -> {

                }, throwable -> log.error(throwable.getMessage()), ctx.getDbCallbackExecutor());
            }, 2, 60000, TimeUnit.MILLISECONDS);
            checkDuplicateMsgsScheduler = initScheduler();
            checkDuplicateMsgsScheduler.schedule(() -> scheduleTickMsg(ctx), CRON_TRIGGER);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init: " + this.getClass().getSimpleName() + " due to: " + e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.getType().equals(TICK_MSG_TYPE)) {
            JsonObject data = new JsonObject();
            duplicateMsgs.forEach((key, value) -> {
                if (value > 5L) {
                    data.addProperty(key.toString(), value);
                }
            });
            TbMsg newMsg = ctx.newMsg(ServiceQueue.MAIN, RESULT_MSG_TYPE, ctx.getTenantId(), msg.getMetaData(), gson.toJson(data));
            ctx.tellSuccess(newMsg);
            duplicateMsgs.clear();
        } else if (msg.getMetaData().getValue("timestamp") != null
                && parser.parse(msg.getData()).getAsJsonObject().has("timestamp")
                && msg.getMetaData().getValue("timestamp").equals(parser.parse(msg.getData()).getAsJsonObject().get("timestamp").getAsString())){
            duplicateMsgs.compute(msg.getOriginator(), (key, value) -> ((value != null) ? (value + 1) : 1));
        }
        ctx.ack(msg);
    }

    @Override
    public void destroy() {

    }

    private List<Customer> getCustomers(TbContext ctx) {
        List<Customer> customers = new ArrayList<>();
        PageLink pageLink = new PageLink(10, 0);
        boolean hasNext = true;
        while (hasNext) {
            PageData<Customer> customersPageData = ctx.getCustomerService().findCustomersByTenantId(ctx.getTenantId(), pageLink);
            if (customersPageData != null && !CollectionUtils.isEmpty(customersPageData.getData())) {
                customers.addAll(customersPageData.getData());
                hasNext = customersPageData.hasNext();
                if (hasNext) {
                    pageLink = pageLink.nextPageLink();
                }
            } else {
                hasNext = false;
            }
        }
        return customers;
    }

    private ThreadPoolTaskScheduler initScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("LocationAnalyticsScheduler");
        taskScheduler.initialize();
        return taskScheduler;
    }

    private void scheduleTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(MAIN, TICK_MSG_TYPE, ctx.getSelfId(), new TbMsgMetaData(), "{}");
        ctx.tellSelf(tickMsg, 0);
    }

    @Data
    @AllArgsConstructor
    private static class DuplicateMsgConfig {

        private int threshold;
        private int timeframe;
        private TimeUnit units;

    }
}
