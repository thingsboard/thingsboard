/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.audit.AuditLogLevelFilter;
import org.thingsboard.server.dao.audit.AuditLogLevelMask;
import org.thingsboard.server.dao.audit.AuditLogLevelProperties;
import org.thingsboard.server.dao.component.ComponentDescriptorService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractServiceTest.class, loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan("org.thingsboard.server")
public abstract class AbstractServiceTest {

    protected ObjectMapper mapper = new ObjectMapper();

    public static final TenantId SYSTEM_TENANT_ID = TenantId.SYS_TENANT_ID;

    @Autowired
    protected UserService userService;

    @Autowired
    protected ApiUsageStateService apiUsageStateService;

    @Autowired
    protected AdminSettingsService adminSettingsService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected EntityService entityService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected TimeseriesService tsService;

    @Autowired
    protected EventService eventService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected AlarmService alarmService;
    @Autowired
    protected AlarmCommentService alarmCommentService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected EdgeService edgeService;

    @Autowired
    protected EdgeEventService edgeEventService;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    @Autowired
    protected TenantProfileService tenantProfileService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    protected AssetProfileService assetProfileService;

    @Autowired
    protected ResourceService resourceService;

    @Autowired
    protected OtaPackageService otaPackageService;

    @Autowired
    protected RpcService rpcService;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected AlarmRuleService alarmRuleService;

    public class IdComparator<D extends HasId> implements Comparator<D> {
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }


    protected RuleNodeDebugEvent generateEvent(TenantId tenantId, EntityId entityId) throws IOException {
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(Uuids.timeBased());
        }
        return RuleNodeDebugEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId("server A")
                .data(JacksonUtil.toString(readFromResource("TestJsonData.json")))
                .build();
    }
//
//    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource) throws IOException {
//        return getOrCreateDescriptor(scope, type, clazz, configurationDescriptorResource, null);
//    }
//
//    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource, String actions) throws IOException {
//        ComponentDescriptor descriptor = componentDescriptorService.findByClazz(clazz);
//        if (descriptor == null) {
//            descriptor = new ComponentDescriptor();
//            descriptor.setName("test");
//            descriptor.setClazz(clazz);
//            descriptor.setScope(scope);
//            descriptor.setType(type);
//            descriptor.setActions(actions);
//            descriptor.setConfigurationDescriptor(readFromResource(configurationDescriptorResource));
//            componentDescriptorService.saveComponent(descriptor);
//        }
//        return descriptor;
//    }

    public JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    @Bean
    public AuditLogLevelFilter auditLogLevelFilter() {
        Map<String, String> mask = new HashMap<>();
        for (EntityType entityType : EntityType.values()) {
            mask.put(entityType.name().toLowerCase(), AuditLogLevelMask.RW.name());
        }
        var props = new AuditLogLevelProperties();
        props.setMask(mask);
        return new AuditLogLevelFilter(props);
    }

    protected DeviceProfile createDeviceProfile(TenantId tenantId, String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName(name);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription(name + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    protected AssetProfile createAssetProfile(TenantId tenantId, String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(name);
        assetProfile.setDescription(name + " Test");
        assetProfile.setDefault(false);
        assetProfile.setDefaultRuleChainId(null);
        return assetProfile;
    }

    public TenantId createTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant " + Uuids.timeBased());
        Tenant savedTenant = tenantService.saveTenant(tenant);
        assertNotNull(savedTenant);
        return savedTenant.getId();
    }

    protected Edge constructEdge(TenantId tenantId, String name, String type) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(name);
        edge.setType(type);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        return edge;
    }

    protected OtaPackage constructDefaultOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId) {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(OtaPackageType.FIRMWARE);
        firmware.setTitle("My firmware");
        firmware.setVersion("3.3.3");
        firmware.setFileName("filename.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        return firmware;
    }

}
