// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class TbAlarmsCountNodeV2Test extends AbstractRuleNodeUpgradeTest {

    private final Gson gson = new Gson();

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineAlarmService alarmService;

    private TbAlarmsCountNodeV2 node;
    private TbNodeConfiguration nodeConfiguration;

    private Map<EntityId, Integer> expectedAllAlarmsCountMap;
    private Map<EntityId, Integer> expectedActiveAlarmsCountMap;
    private Map<EntityId, Integer> expectedLastDayAlarmsCountMap;
    private Set<Long> alarmCreatedTimes;

    @BeforeEach
    public void before() {
        node = spy(new TbAlarmsCountNodeV2());
        lenient().doAnswer((Answer<List<Long>>) invocationOnMock -> {
            AlarmQuery query = (AlarmQuery) (invocationOnMock.getArguments())[1];
            List<AlarmFilter> filters = (List<AlarmFilter>) (invocationOnMock.getArguments())[2];
            return findAlarmCounts(alarmService, query, filters);
        }).when(alarmService).findAlarmCounts(ArgumentMatchers.any(), ArgumentMatchers.any(AlarmQuery.class), ArgumentMatchers.any(List.class));

        lenient().when(ctx.getAlarmService()).thenReturn(alarmService);
    }

    public void init(TbAlarmsCountNodeV2Configuration configuration) throws TbNodeException {
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(configuration));
        expectedAllAlarmsCountMap = new HashMap<>();
        expectedActiveAlarmsCountMap = new HashMap<>();
        expectedLastDayAlarmsCountMap = new HashMap<>();
        alarmCreatedTimes = new HashSet<>();
    }

    @Test
    public void alarmsCountV2Test() throws Exception {
        processTest(TbMsgType.ALARM);
    }

    @Test
    public void alarmsCountV2Test2() throws Exception {
        processTest(TbMsgType.ENTITY_CREATED);
    }

    private void processTest(TbMsgType type) throws Exception {
        init(createConfig());
        performAlarmsCountTest(type);
    }

    @Test
    public void alarmsCountV2TestWithCountAlarmsForPropagationEntitiesEnabled() throws Exception {
        init(createConfigWithCountAlarmsForPropagationEntitiesEnabled());
        testWithCountAlarmsForPropagationEntitiesEnabled();
    }

    @Test
    public void alarmsCountV2TestWithCountAlarmsForPropagationEntitiesEnabledAndPropagationTypesSelected() throws Exception {
        List<EntityType> propagationEntityTypes = Collections.singletonList(EntityType.ASSET);
        init(createConfigWithCountAlarmsForPropagationEntitiesEnabledAndPropagationTypesSelected(propagationEntityTypes));
        testWithCountAlarmsForPropagationEntitiesEnabledAndPropagationTypesSelected(propagationEntityTypes);
    }

    // Rule nodes upgrade
    public static final String EXPECTED_CONFIG = "{\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]," +
            "\"countAlarmsForPropagationEntities\":true,\"propagationEntityTypes\":[],\"outMsgType\":\"POST_TELEMETRY_REQUEST\"}";

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]," +
                                "\"countAlarmsForPropagationEntities\":true,\"propagationEntityTypes\":[],\"queueName\":null}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]," +
                                "\"countAlarmsForPropagationEntities\":true,\"propagationEntityTypes\":[],\"queueName\":\"Main\"}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 1 with upgrade from version 1
                Arguments.of(1,
                        "{\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]," +
                                "\"countAlarmsForPropagationEntities\":true,\"propagationEntityTypes\":[],\"queueName\":\"Main\",\"outMsgType\":\"POST_TELEMETRY_REQUEST\"}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 2 with upgrade from version 0
                Arguments.of(0, EXPECTED_CONFIG, false, EXPECTED_CONFIG)
        );
    }

    private TbAlarmsCountNodeV2Configuration createConfig() {
        TbAlarmsCountNodeV2Configuration configuration = new TbAlarmsCountNodeV2Configuration();
        configuration.setCountAlarmsForPropagationEntities(false);
        configuration.setAlarmsCountMappings(getAlarmsCountMappings());
        configuration.setPropagationEntityTypes(Collections.emptyList());
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }

    private TbAlarmsCountNodeV2Configuration createConfigWithCountAlarmsForPropagationEntitiesEnabled() {
        TbAlarmsCountNodeV2Configuration configuration = createConfig();
        configuration.setCountAlarmsForPropagationEntities(true);
        return configuration;
    }

    private TbAlarmsCountNodeV2Configuration createConfigWithCountAlarmsForPropagationEntitiesEnabledAndPropagationTypesSelected(List<EntityType> propagationEntityTypes) {
        TbAlarmsCountNodeV2Configuration configuration = createConfig();
        configuration.setCountAlarmsForPropagationEntities(true);
        configuration.setPropagationEntityTypes(propagationEntityTypes);
        return configuration;
    }

    private List<AlarmsCountMapping> getAlarmsCountMappings() {
        List<AlarmsCountMapping> alarmsCountMappings = new ArrayList<>();

        AlarmsCountMapping allAlarmsCountMapping = new AlarmsCountMapping();
        allAlarmsCountMapping.setTarget("allAlarmsCount");
        alarmsCountMappings.add(allAlarmsCountMapping);

        AlarmsCountMapping lastDayAlarmsCountMapping = new AlarmsCountMapping();
        lastDayAlarmsCountMapping.setTarget("lastDayAlarmsCount");
        lastDayAlarmsCountMapping.setLatestInterval(TimeUnit.DAYS.toMillis(1));
        alarmsCountMappings.add(lastDayAlarmsCountMapping);

        AlarmsCountMapping activeAlarmsCountMapping = new AlarmsCountMapping();
        activeAlarmsCountMapping.setStatusList(Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK));
        activeAlarmsCountMapping.setTarget("activeAlarmsCount");
        alarmsCountMappings.add(activeAlarmsCountMapping);
        return alarmsCountMappings;
    }

    private void performAlarmsCountTest(TbMsgType type) throws Exception {
        int totalEntitiesCount = 10 + (int) (Math.random() * 20);
        List<AlarmInfo> alarms;

        for (int i = 0; i < totalEntitiesCount; i++) {
            EntityId deviceId = new DeviceId(Uuids.timeBased());
            alarms = generateAlarms(deviceId, null);
            expectedAllAlarmsCountMap.put(deviceId, alarms.size());
            expectedActiveAlarmsCountMap.put(deviceId, countActive(alarms));
            expectedLastDayAlarmsCountMap.put(deviceId, countLastDay(alarms));
        }

        node.init(ctx, nodeConfiguration);

        expectedAllAlarmsCountMap.keySet().forEach(entityId -> {
            Alarm alarm = new Alarm();
            alarm.setOriginator(entityId);
            alarm.setPropagate(true);
            try {
                TbMsg alarmMsg = TbMsg.newMsg()
                        .type(type)
                        .originator(entityId)
                        .copyMetaData(TbMsgMetaData.EMPTY)
                        .dataType(TbMsgDataType.JSON)
                        .data(JacksonUtil.toString(alarm))
                        .build();
                node.onMsg(ctx, alarmMsg);
            } catch (Exception e) {
                log.error("Exception occurred during onMsg processing: ", e);
            }
        });

        verifyResult(totalEntitiesCount);
    }

    private void testWithCountAlarmsForPropagationEntitiesEnabled() throws Exception {
        EntityId rootEntityId = new TenantId(Uuids.timeBased());
        List<AlarmInfo> parentEntityAlarms = new ArrayList<>();
        int parentCount = 10 + (int) (Math.random() * 20);

        int totalChildCount = 0;

        Map<EntityId, EntityId> childToParentMap = new HashMap<>();

        for (int i = 0; i < parentCount; i++) {
            EntityId parentEntityId = new AssetId(Uuids.timeBased());
            List<AlarmInfo> childAlarms = new ArrayList<>();
            int childCount = 10 + (int) (Math.random() * 20);

            totalChildCount += childCount;

            for (int c = 0; c < childCount; c++) {
                EntityId childEntityId = new DeviceId(Uuids.timeBased());
                childToParentMap.put(childEntityId, parentEntityId);
                List<AlarmInfo> alarms = generateAlarms(childEntityId, null);
                expectedAllAlarmsCountMap.put(childEntityId, alarms.size());
                expectedActiveAlarmsCountMap.put(childEntityId, countActive(alarms));
                expectedLastDayAlarmsCountMap.put(childEntityId, countLastDay(alarms));
                childAlarms.addAll(alarms);
            }
            List<AlarmInfo> alarms = generateAlarms(parentEntityId, childAlarms);
            parentEntityAlarms.addAll(alarms);
            expectedAllAlarmsCountMap.put(parentEntityId, alarms.size());
            expectedActiveAlarmsCountMap.put(parentEntityId, countActive(alarms));
            expectedLastDayAlarmsCountMap.put(parentEntityId, countLastDay(alarms));
        }

        List<AlarmInfo> rootEntityAlarms = generateAlarms(rootEntityId, parentEntityAlarms);
        expectedAllAlarmsCountMap.put(rootEntityId, rootEntityAlarms.size());
        expectedActiveAlarmsCountMap.put(rootEntityId, countActive(rootEntityAlarms));
        expectedLastDayAlarmsCountMap.put(rootEntityId, countLastDay(rootEntityAlarms));

        node.init(ctx, nodeConfiguration);

        expectedAllAlarmsCountMap.keySet().forEach(entityId -> {
            EntityId parentEntityId = childToParentMap.get(entityId);
            if (parentEntityId != null) {
                AlarmId alarmId = new AlarmId(Uuids.timeBased());
                Alarm alarm = createAlarm(entityId);
                Set<EntityId> parentEntityIds = new HashSet<>();
                parentEntityIds.add(parentEntityId);
                parentEntityIds.add(rootEntityId);
                parentEntityIds.add(alarm.getOriginator());
                when(ctx.getAlarmService().getPropagationEntityIds(Mockito.any(), eq(Collections.emptyList()))).thenReturn(parentEntityIds);
                try {
                    TbMsg alarmMsg = TbMsg.newMsg()
                            .type(TbMsgType.ALARM)
                            .originator(entityId)
                            .copyMetaData(TbMsgMetaData.EMPTY)
                            .dataType(TbMsgDataType.JSON)
                            .data(JacksonUtil.toString(alarm))
                            .build();
                    node.onMsg(ctx, alarmMsg);
                } catch (Exception e) {
                    log.error("Exception occurred during onMsg processing: ", e);
                }
            }
        });

        verifyResult(totalChildCount * 3);
    }

    private Alarm createAlarm(EntityId entityId) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(Uuids.timeBased()));
        alarm.setOriginator(entityId);
        alarm.setPropagate(true);
        return alarm;
    }

    private void testWithCountAlarmsForPropagationEntitiesEnabledAndPropagationTypesSelected(List<EntityType> propagationEntityTypes) throws Exception {
        int parentCount = 10 + (int) (Math.random() * 20);

        int totalChildCount = 0;

        Map<EntityId, EntityId> childToParentMap = new HashMap<>();

        for (int i = 0; i < parentCount; i++) {
            EntityId parentEntityId = new AssetId(Uuids.timeBased());
            List<AlarmInfo> childAlarms = new ArrayList<>();

            int childCount = 10 + (int) (Math.random() * 20);

            totalChildCount += childCount;

            for (int c = 0; c < childCount; c++) {
                EntityId childEntityId = new DeviceId(Uuids.timeBased());
                childToParentMap.put(childEntityId, parentEntityId);
                List<AlarmInfo> alarms = generateAlarms(childEntityId, Collections.emptyList());
                expectedAllAlarmsCountMap.put(childEntityId, alarms.size());
                expectedActiveAlarmsCountMap.put(childEntityId, countActive(alarms));
                expectedLastDayAlarmsCountMap.put(childEntityId, countLastDay(alarms));
                childAlarms.addAll(alarms);
            }

            List<AlarmInfo> alarms = generateAlarms(parentEntityId, childAlarms);
            expectedAllAlarmsCountMap.put(parentEntityId, alarms.size());
            expectedActiveAlarmsCountMap.put(parentEntityId, countActive(alarms));
            expectedLastDayAlarmsCountMap.put(parentEntityId, countLastDay(alarms));
        }

        node.init(ctx, nodeConfiguration);

        expectedAllAlarmsCountMap.keySet().forEach(entityId -> {
            EntityId parentEntityId = childToParentMap.get(entityId);
            if (parentEntityId != null) {
                Alarm alarm = createAlarm(entityId);
                Set<EntityId> parentEntityIds = new HashSet<>();
                parentEntityIds.add(parentEntityId);
                when(ctx.getAlarmService().getPropagationEntityIds(Mockito.any(), eq(propagationEntityTypes))).thenReturn(parentEntityIds);
                try {
                    TbMsg alarmMsg = TbMsg.newMsg()
                            .type(TbMsgType.ALARM)
                            .originator(entityId)
                            .copyMetaData(TbMsgMetaData.EMPTY)
                            .dataType(TbMsgDataType.JSON)
                            .data(JacksonUtil.toString(alarm))
                            .build();
                    node.onMsg(ctx, alarmMsg);
                } catch (Exception e) {
                    log.error("Exception occurred during onMsg processing: ", e);
                }
            }
        });

        verifyResult(totalChildCount * 2);
    }

    private void verifyResult(int totalEntitiesCount) {
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, new Times(totalEntitiesCount)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));
        List<TbMsg> messages = captor.getAllValues();
        for (TbMsg msg : messages) {
            verifyMessage(msg);
        }
    }

    private int countActive(List<AlarmInfo> alarms) {
        List<AlarmStatus> activeStatuses = Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK);
        return (int) alarms.stream().filter(alarm -> activeStatuses.contains(alarm.getStatus())).count();
    }

    private int countLastDay(List<AlarmInfo> alarms) {
        long maxTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        return (int) alarms.stream().filter(alarm -> alarm.getCreatedTime() >= maxTime).count();
    }

    private List<AlarmInfo> generateAlarms(EntityId entityId, List<AlarmInfo> childAlarms) {
        int alarmsCount = 10 + (int) (Math.random() * 20);
        List<AlarmInfo> alarms;
        if (CollectionUtils.isEmpty(childAlarms)) {
            alarms = new ArrayList<>(alarmsCount);
        } else {
            alarms = new ArrayList<>(childAlarms.size());
            alarms.addAll(childAlarms);
        }
        for (int i = 0; i < alarmsCount; i++) {

            AlarmInfo alarm = new AlarmInfo();

            long createdTime;

            do {
                long interval = TimeUnit.DAYS.toMillis((int) (Math.random() * 2)) + TimeUnit.HOURS.toMillis(12)
                        + (int) (Math.random() * 60000);

                createdTime = System.currentTimeMillis() - interval;
            } while (alarmCreatedTimes.contains(createdTime));

            alarmCreatedTimes.add(createdTime);

            alarm.setId(new AlarmId(Uuids.startOf(createdTime)));
            int alarmStatusOrdinal = (int) Math.floor(Math.random() * AlarmStatus.values().length);
            var alarmStatus = AlarmStatus.values()[alarmStatusOrdinal];
            alarm.setCleared(alarmStatus.isCleared());
            alarm.setAcknowledged(alarmStatus.isAck());
            alarm.setStartTs(createdTime);
            alarm.setCreatedTime(createdTime);
            alarm.setSeverity(AlarmSeverity.CRITICAL);
            alarm.setOriginator(entityId);
            alarm.setType(StringUtils.randomAlphanumeric(15));
            alarm.setPropagate(true);
            alarms.add(alarm);
        }

        PageData<AlarmInfo> pageData = new PageData<>(alarms, 1, alarms.size(), false);
        when(alarmService.findAlarms(ArgumentMatchers.any(), argThat(query ->
                query != null && query.getAffectedEntityId().equals(entityId)
        ))).thenReturn(pageData);
        return alarms;
    }

    private void verifyMessage(TbMsg msg) {
        Assertions.assertTrue(msg.isTypeOf(TbMsgType.POST_TELEMETRY_REQUEST));
        EntityId entityId = msg.getOriginator();
        Assertions.assertNotNull(entityId);
        String data = msg.getData();
        Assertions.assertNotNull(data);
        JsonObject dataJson = gson.fromJson(data, JsonObject.class);

        Assertions.assertTrue(dataJson.has("allAlarmsCount") ||
                dataJson.has("lastDayAlarmsCount") ||
                dataJson.has("activeAlarmsCount")
        );
        if (dataJson.has("allAlarmsCount")) {
            JsonElement elem = dataJson.get("allAlarmsCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assertions.assertEquals(expectedAllAlarmsCountMap.get(entityId).intValue(), intVal);
        }
        if (dataJson.has("lastDayAlarmsCount")) {
            JsonElement elem = dataJson.get("lastDayAlarmsCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assertions.assertEquals(expectedLastDayAlarmsCountMap.get(entityId).intValue(), intVal);
        }
        if (dataJson.has("activeAlarmsCount")) {
            JsonElement elem = dataJson.get("activeAlarmsCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assertions.assertEquals(expectedActiveAlarmsCountMap.get(entityId).intValue(), intVal);
        }
    }

    private static List<Long> findAlarmCounts(RuleEngineAlarmService service, AlarmQuery query, List<AlarmFilter> filters) {
        List<Long> alarmCounts = new ArrayList<>();
        for (AlarmFilter filter : filters) {
            alarmCounts.add(0l);
        }
        PageData<AlarmInfo> alarms;
        do {
            alarms = service.findAlarms(TenantId.SYS_TENANT_ID, query);
            for (int i = 0; i < filters.size(); i++) {
                Predicate<AlarmInfo> filter = matchAlarmFilter(filters.get(i));
                long count = alarms.getData().stream().filter(filter).map(AlarmInfo::getId).distinct().count() + alarmCounts.get(i);
                alarmCounts.set(i, count);
            }
            if (alarms.hasNext()) {
                query = new AlarmQuery(query.getAffectedEntityId(), query.getPageLink(), query.getSearchStatus(), query.getStatus(), null, false);
            }
        } while (alarms.hasNext());
        return alarmCounts;
    }

    private static Predicate<AlarmInfo> matchAlarmFilter(AlarmFilter filter) {
        return alarmInfo -> {
            if (!matches(filter.getTypesList(), alarmInfo.getType())) {
                return false;
            }
            if (!matches(filter.getSeverityList(), alarmInfo.getSeverity())) {
                return false;
            }
            if (!matches(filter.getStatusList(), alarmInfo.getStatus())) {
                return false;
            }
            if (filter.getStartTime() != null) {
                if (alarmInfo.getCreatedTime() <= filter.getStartTime()) {
                    return false;
                }
            }
            return true;
        };
    }

    private static <T> boolean matches(List<T> filterList, T value) {
        if (filterList != null && !filterList.isEmpty()) {
            return filterList.contains(value);
        } else {
            return true;
        }
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
