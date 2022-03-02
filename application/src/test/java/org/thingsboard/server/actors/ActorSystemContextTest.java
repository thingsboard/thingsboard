/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.rule.RuleNodeStateService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;
import org.thingsboard.server.service.executors.SharedEventLoopGroupService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.script.JsInvokeService;
import org.thingsboard.server.service.session.DeviceSessionCacheService;
import org.thingsboard.server.service.sms.SmsExecutorService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ActorSystemContext.class)
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "cache.type=caffeine",
})
public class ActorSystemContextTest {

    @Autowired
    ActorSystemContext ctx;

    @MockBean
    private TbApiUsageStateService apiUsageStateService;

    @MockBean
    private TbApiUsageClient apiUsageClient;

    @MockBean
    private TbServiceInfoProvider serviceInfoProvider;

    @MockBean
    private ActorService actorService;

    @MockBean
    private ComponentDiscoveryService componentService;

    @MockBean
    private DataDecodingEncodingService encodingService;

    @MockBean
    private DeviceService deviceService;

    @MockBean
    private TbTenantProfileCache tenantProfileCache;

    @MockBean
    private TbDeviceProfileCache deviceProfileCache;

    @MockBean
    private AssetService assetService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private TenantProfileService tenantProfileService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private UserService userService;

    @MockBean
    private RuleChainService ruleChainService;

    @MockBean
    private RuleNodeStateService ruleNodeStateService;

    @MockBean
    private PartitionService partitionService;

    @MockBean
    private TbClusterService clusterService;

    @MockBean
    private TimeseriesService tsService;

    @MockBean
    private AttributesService attributesService;

    @MockBean
    private EventService eventService;

    @MockBean
    private RelationService relationService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private EntityViewService entityViewService;

    @MockBean
    private TelemetrySubscriptionService tsSubService;

    @MockBean
    private AlarmSubscriptionService alarmService;

    @MockBean
    private JsInvokeService jsSandbox;

    @MockBean
    private MailExecutorService mailExecutor;

    @MockBean
    private SmsExecutorService smsExecutor;

    @MockBean
    private DbCallbackExecutorService dbCallbackExecutor;

    @MockBean
    private ExternalCallExecutorService externalCallExecutorService;

    @MockBean
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @MockBean
    private MailService mailService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private SmsSenderFactory smsSenderFactory;

    @MockBean
    private ClaimDevicesService claimDevicesService;

    @MockBean
    private JsInvokeStats jsInvokeStats;

    @MockBean
    private DeviceStateService deviceStateService;

    @MockBean
    private DeviceSessionCacheService deviceSessionCacheService;

    @MockBean
    private TbCoreToTransportService tbCoreToTransportService;

    @MockBean
    private TbRuleEngineDeviceRpcService tbRuleEngineDeviceRpcService;

    @MockBean
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    @MockBean
    private EdgeService edgeService;

    @MockBean
    private EdgeEventService edgeEventService;

    @MockBean
    private EdgeRpcService edgeRpcService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private OtaPackageService otaPackageService;

    @MockBean
    private TbRpcService tbRpcService;

    @MockBean
    private CassandraCluster cassandraCluster;

    @MockBean
    private CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;

    @MockBean
    private CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void givenCaffeineCache_whenInit_thenIsLocalCacheTrue() {
        assertThat(ctx.getCacheType()).isEqualTo("caffeine");
        assertThat(ctx.isLocalCacheType()).as("caffeine is the local cache type").isTrue();
    }

}
