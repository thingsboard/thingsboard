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
package org.thingsboard.server.service.edqs;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.edqs.processor.EdqsConverter;

import java.io.FileReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.thingsboard.common.util.JacksonUtil.toJsonNode;


@RequiredArgsConstructor
@Slf4j
//@Service
public class EdqsDataLoader {

    private final EdqsService edqsService;
    private final EdqsConverter edqsConverter;

    public final static TenantId MAIN = TenantId.fromUUID(UUID.fromString("2a209df0-c7ff-11ea-a3e0-f321b0429d60"));

    private final String folder = "/home/viacheslav/Downloads/schwarz";

    private ExecutorService executor = Executors.newFixedThreadPool(5, ThingsBoardThreadFactory.forName("edqs-publisher"));

//    @AfterStartUp(order = 100)
    public void load() throws Exception {
        loadCustomers();
        loadDeviceProfile();
        loadDevices();
        loadAssets();
        loadEdges();
        loadEntityViews();
        loadTenants();
        loadUsers();
        loadDashboards();
        loadRuleChains();
        loadWidgetType();
        loadWidgetBundle();
        loadConverters();
        loadIntegrations();
        loadSchedulerEvents();
        loadRoles();
        loadApiUsageStates();
        loadAssetProfile();
        loadEntityGroups();
        loadRelations();

        loadAttributes();
        loadTs();
    }

    private void loadCustomers() throws Exception {
        load("customer.csv", (values) -> {
            Customer customer = new Customer();
            customer.setTitle(values.get("title"));
            customer.setId(new CustomerId(UUID.fromString(values.get("id"))));
            customer.setCreatedTime(Long.parseLong(values.get("created_time")));
            customer.setTenantId(tenantId(values.get("tenant_id")));
            var parentCustomerId = values.get("parent_customer_id");
            if (StringUtils.isNotEmpty(parentCustomerId)) {
                customer.setParentCustomerId(new CustomerId(UUID.fromString(parentCustomerId)));
            }
            edqsService.onUpdate(customer.getTenantId(), customer.getId(), customer);
        });
    }

    private void loadDevices() throws Exception {
        load("device.csv", (values) -> {
            Device device = new Device();
            device.setType(values.get("type"));
            device.setName(values.get("name"));
            device.setLabel(values.get("label"));
            device.setId(new DeviceId(uuid(values.get("id"))));
            device.setCreatedTime(parseLong(values.get("created_time")));
            device.setCustomerId(customerId(values.get("customer_id")));
            device.setTenantId(tenantId(values.get("tenant_id")));
            device.setDeviceProfileId(new DeviceProfileId(uuid(values.get("device_profile_id"))));
            device.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(device.getTenantId(), device.getId(), device);
        });
    }

    private void loadAssets() throws Exception {
        load("asset.csv", (values) -> {
            Asset asset = new Asset();
            asset.setType(values.get("type"));
            asset.setName(values.get("name"));
            asset.setLabel(values.get("label"));
            asset.setId(new AssetId(uuid(values.get("id"))));
            asset.setCreatedTime(parseLong(values.get("created_time")));
            asset.setCustomerId(customerId(values.get("customer_id")));
            asset.setTenantId(tenantId(values.get("tenant_id")));
            asset.setAssetProfileId(new AssetProfileId(uuid(values.get("asset_profile_id"))));
            asset.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(asset.getTenantId(), asset.getId(), asset);
        });
    }

    private void loadEdges() throws Exception {
        load("edge.csv", (values) -> {
            Edge edge = new Edge();
            edge.setId(new EdgeId(uuid(values.get("id"))));
            edge.setCreatedTime(parseLong(values.get("created_time")));
            edge.setType(values.get("type"));
            edge.setName(values.get("name"));
            edge.setLabel(values.get("label"));
            edge.setCustomerId(customerId(values.get("customer_id")));
            edge.setTenantId(tenantId(values.get("tenant_id")));
            edge.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(edge.getTenantId(), edge.getId(), edge);
        });
    }

    private void loadEntityViews() throws Exception {
        load("entity_view.csv", (values) -> {
            EntityView entityView = new EntityView();
            entityView.setId(new EntityViewId(uuid(values.get("id"))));
            entityView.setCreatedTime(parseLong(values.get("created_time")));
            entityView.setType(values.get("type"));
            entityView.setName(values.get("name"));
            entityView.setCustomerId(customerId(values.get("customer_id")));
            entityView.setTenantId(tenantId(values.get("tenant_id")));
            entityView.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(entityView.getTenantId(), entityView.getId(), entityView);
        });
    }

    private void loadTenants() throws Exception {
        load("tenant.csv", (values) -> {
            Tenant tenant = new Tenant();
            tenant.setId(new TenantId(uuid(values.get("id"))));
            tenant.setCreatedTime(parseLong(values.get("created_time")));
            tenant.setEmail(values.get("email"));
            tenant.setTitle(values.get("title"));
            tenant.setCountry(values.get("country"));
            tenant.setState(values.get("state"));
            tenant.setCity(values.get("city"));
            tenant.setAddress(values.get("address"));
            tenant.setAddress2(values.get("address2"));
            tenant.setZip(values.get("zip"));
            tenant.setPhone(values.get("phone"));
            tenant.setRegion(values.get("region"));
            tenant.setTenantProfileId(new TenantProfileId(uuid(values.get("tenant_profile_id"))));
            tenant.setAdditionalInfo(toJsonNode(values.get("additional_info")));
            edqsService.onUpdate(MAIN, tenant.getId(), tenant);
        });
    }

    private void loadUsers() throws Exception {
        load("user.csv", (values) -> {
            User user = new User();
            user.setId(new UserId(uuid(values.get("id"))));
            user.setCreatedTime(parseLong(values.get("created_time")));
            user.setTenantId(tenantId(values.get("tenant_id")));
            user.setFirstName(values.get("first_name"));
            user.setLastName(values.get("last_name"));
            user.setEmail(values.get("email"));
            user.setPhone(values.get("phone"));
            user.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(user.getTenantId(), user.getId(), user);
        });
    }

    private void loadDashboards() throws Exception {
        load("dashboard.csv", (values) -> {
            Dashboard dashboard = new Dashboard();
            dashboard.setId(new DashboardId(uuid(values.get("id"))));
            dashboard.setCreatedTime(parseLong(values.get("created_time")));
            dashboard.setTenantId(tenantId(values.get("tenant_id")));
            dashboard.setTitle(values.get("title"));

            edqsService.onUpdate(dashboard.getTenantId(), dashboard.getId(), dashboard);
        });
    }

    private void loadEntityGroups() throws Exception {
        load("entity_group.csv", (values) -> {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setId(new EntityGroupId(uuid(values.get("id"))));
            entityGroup.setCreatedTime(parseLong(values.get("created_time")));
            entityGroup.setName(values.get("name"));
            entityGroup.setOwnerId(entityId(values.get("owner_type"), values.get("owner_id")));
            entityGroup.setType(EntityType.valueOf(values.get("type")));
            edqsService.onUpdate(MAIN, entityGroup.getId(), entityGroup);
        });
    }

    private void loadRelations() throws Exception {
        load("relation.csv", (values) -> {
            EntityRelation entityRelation = new EntityRelation();
            entityRelation.setFrom(entityId(values.get("from_type"), values.get("from_id")));
            entityRelation.setTo(entityId(values.get("to_type"), values.get("to_id")));
            entityRelation.setTypeGroup(RelationTypeGroup.valueOf(values.get("relation_type_group")));
            entityRelation.setType(values.get("relation_type"));
            edqsService.onUpdate(MAIN, ObjectType.RELATION, entityRelation);
        });
    }

    private void loadRuleChains() throws Exception {
        load("rule_chain.csv", (values) -> {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setId(new RuleChainId(uuid(values.get("id"))));
            ruleChain.setCreatedTime(parseLong(values.get("created_time")));
            ruleChain.setName(values.get("name"));
            ruleChain.setTenantId(tenantId(values.get("tenant_id")));
            ruleChain.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(ruleChain.getTenantId(), ruleChain.getId(), ruleChain);
        });
    }

    private void loadWidgetType() throws Exception {
        load("widget_type.csv", (values) -> {
            WidgetType widgetType = new WidgetType();
            widgetType.setId(new WidgetTypeId(uuid(values.get("id"))));
            widgetType.setCreatedTime(parseLong(values.get("created_time")));
            widgetType.setName(values.get("name"));
            widgetType.setTenantId(tenantId(values.get("tenant_id")));

            edqsService.onUpdate(widgetType.getTenantId(), widgetType.getId(), widgetType);
        });
    }

    private void loadWidgetBundle() throws Exception {
        load("widgets_bundle.csv", (values) -> {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setId(new WidgetsBundleId(uuid(values.get("id"))));
            widgetsBundle.setCreatedTime(parseLong(values.get("created_time")));
            widgetsBundle.setTitle(values.get("title"));
            widgetsBundle.setTenantId(tenantId(values.get("tenant_id")));

            edqsService.onUpdate(widgetsBundle.getTenantId(), widgetsBundle.getId(), widgetsBundle);
        });
    }

    private void loadConverters() throws Exception {
        load("converter.csv", (values) -> {
            Converter converter = new Converter();
            converter.setId(new ConverterId(uuid(values.get("id"))));
            converter.setCreatedTime(parseLong(values.get("created_time")));
            converter.setName(values.get("name"));
            converter.setType(ConverterType.valueOf(values.get("type")));
            converter.setTenantId(tenantId(values.get("tenant_id")));
            converter.setEdgeTemplate(parseBoolean(values.get("is_edge_template")));
            converter.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(converter.getTenantId(), converter.getId(), converter);
        });
    }

    private void loadIntegrations() throws Exception {
        load("integration.csv", (values) -> {
            Integration integration = new Integration();
            integration.setId(new IntegrationId(uuid(values.get("id"))));
            integration.setCreatedTime(parseLong(values.get("created_time")));
            integration.setName(values.get("name"));
            integration.setType(IntegrationType.valueOf(values.get("type")));
            integration.setTenantId(tenantId(values.get("tenant_id")));
            integration.setEdgeTemplate(parseBoolean(values.get("is_edge_template")));
            integration.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(integration.getTenantId(), integration.getId(), integration);
        });
    }

    private void loadSchedulerEvents() throws Exception {
        load("scheduler_event.csv", (values) -> {
            SchedulerEvent schedulerEvent = new SchedulerEvent();
            schedulerEvent.setId(new SchedulerEventId(uuid(values.get("id"))));
            schedulerEvent.setCreatedTime(parseLong(values.get("created_time")));
            schedulerEvent.setName(values.get("name"));
            schedulerEvent.setType(values.get("type"));
            schedulerEvent.setTenantId(tenantId(values.get("tenant_id")));
            schedulerEvent.setConfiguration(toJsonNode(values.get("configuration")));
            schedulerEvent.setSchedule(toJsonNode(values.get("schedule")));
            schedulerEvent.setOriginatorId(entityId(values.get("originator_type"), values.get("originator_id")));
            schedulerEvent.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(schedulerEvent.getTenantId(), schedulerEvent.getId(), schedulerEvent);
        });
    }

    private void loadRoles() throws Exception {
        load("role.csv", (values) -> {
            Role role = new Role();
            role.setId(new RoleId(uuid(values.get("id"))));
            role.setCreatedTime(parseLong(values.get("created_time")));
            role.setName(values.get("name"));
            role.setType(RoleType.valueOf(values.get("type")));
            role.setTenantId(tenantId(values.get("tenant_id")));
            role.setAdditionalInfo(toJsonNode(values.get("additional_info")));

            edqsService.onUpdate(role.getTenantId(), role.getId(), role);
        });
    }

    private void loadApiUsageStates() throws Exception {
        load("api_usage_state.csv", (values) -> {
            ApiUsageState apiUsageState = new ApiUsageState();
            apiUsageState.setId(new ApiUsageStateId(uuid(values.get("id"))));
            apiUsageState.setCreatedTime(parseLong(values.get("created_time")));
            apiUsageState.setEntityId(entityId(values.get("entity_type"), values.get("entity_id")));
            apiUsageState.setTenantId(tenantId(values.get("tenant_id")));

            edqsService.onUpdate(apiUsageState.getTenantId(), apiUsageState.getId(), apiUsageState);
        });
    }

    private void loadDeviceProfile() throws Exception {
        load("device_profile.csv", (values) -> {
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setId(new DeviceProfileId(uuid(values.get("id"))));
            deviceProfile.setCreatedTime(parseLong(values.get("created_time")));
            deviceProfile.setName(values.get("name"));
            deviceProfile.setType(DeviceProfileType.valueOf(values.get("type")));
            deviceProfile.setTenantId(tenantId(values.get("tenant_id")));

            edqsService.onUpdate(deviceProfile.getTenantId(), deviceProfile.getId(), deviceProfile);
        });
    }

    private void loadAssetProfile() throws Exception {
        load("asset_profile.csv", (values) -> {
            AssetProfile assetProfile = new AssetProfile();
            assetProfile.setId(new AssetProfileId(uuid(values.get("id"))));
            assetProfile.setCreatedTime(parseLong(values.get("created_time")));
            assetProfile.setName(values.get("name"));
            assetProfile.setTenantId(tenantId(values.get("tenant_id")));

            edqsService.onUpdate(assetProfile.getTenantId(), assetProfile.getId(), assetProfile);
        });
    }

    private void loadAttributes() throws Exception {
        load("attribute.csv", (values) -> {
            EntityId entityId = EntityIdFactory.getByTypeAndId(values.get("entity_type"), values.get("entity_id"));
            long ts = parseLong(values.get("last_update_ts"));
            AttributeScope scope = AttributeScope.valueOf(values.get("attribute_type"));
            String key = values.get("attribute_key");
            KvEntry kvEntry;
            if (StringUtils.isNotEmpty(values.get("bool_v"))) {
                kvEntry = new BooleanDataEntry(key, "t".equals(values.get("bool_v")));
            } else if (StringUtils.isNotEmpty(values.get("str_v"))) {
                kvEntry = new StringDataEntry(key, values.get("str_v"));
            } else if (StringUtils.isNotEmpty(values.get("long_v"))) {
                kvEntry = new LongDataEntry(key, parseLong(values.get("long_v")));
            } else if (StringUtils.isNotEmpty(values.get("dbl_v"))) {
                kvEntry = new DoubleDataEntry(key, Double.parseDouble(values.get("dbl_v")));
            } else if (StringUtils.isNotEmpty(values.get("json_v"))) {
                kvEntry = new JsonDataEntry(key, values.get("json_v"));
            } else {
                kvEntry = new StringDataEntry(key, "");
            }
            AttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(ts, kvEntry);
            AttributeKv attributeKv = new AttributeKv(entityId, scope, attributeKvEntry, 0);
            edqsService.onUpdate(MAIN, ObjectType.ATTRIBUTE_KV, attributeKv);
        });
    }

    private void loadTs() throws Exception {
        load("ts_kv.csv", (values) -> {
            var entityTypeStr = values.get("find_entity_type");
            if (StringUtils.isEmpty(entityTypeStr)) {
                return;
            }
            EntityId entityId = EntityIdFactory.getByTypeAndId(values.get("find_entity_type"), values.get("entity_id"));
            long ts = parseLong(values.get("ts"));
            String key = values.get("key");
            KvEntry kvEntry;
            if (StringUtils.isNotEmpty(values.get("bool_v"))) {
                kvEntry = new BooleanDataEntry(key, "t".equals(values.get("bool_v")));
            } else if (StringUtils.isNotEmpty(values.get("str_v"))) {
                kvEntry = new StringDataEntry(key, values.get("str_v"));
            } else if (StringUtils.isNotEmpty(values.get("long_v"))) {
                kvEntry = new LongDataEntry(key, parseLong(values.get("long_v")));
            } else if (StringUtils.isNotEmpty(values.get("dbl_v"))) {
                kvEntry = new DoubleDataEntry(key, Double.parseDouble(values.get("dbl_v")));
            } else if (StringUtils.isNotEmpty(values.get("json_v"))) {
                kvEntry = new JsonDataEntry(key, values.get("json_v"));
            } else {
                kvEntry = new StringDataEntry(key, "");
            }
            BasicTsKvEntry tsKvEntry = new BasicTsKvEntry(ts, kvEntry);
            edqsService.onUpdate(MAIN, ObjectType.LATEST_TS_KV, new LatestTsKv(entityId, tsKvEntry, 0L));
        });
    }

    private void load(String file, Consumer<Map<String, String>> function) throws Exception {
        Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("loader-" + file)).submit(() -> {
            try {
                long ts = System.currentTimeMillis();
                CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator('|');
                CsvMapper mapper = new CsvMapper();
                MappingIterator<Map<String, String>> it = mapper
                        .readerFor(Map.class)
                        .with(schema)
                        .readValues(new FileReader(folder + "/" + file));

                int success = 0;
                int failure = 0;
                while (it.hasNextValue()) {
                    Map<String, String> row = it.nextValue();
                    try {
                        function.accept(row);
                        success++;
                        if (success % 1000 == 0) {
                            log.info("Loaded [{}] from [{}]", success, file);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse str: [{}]", row, e);
                        failure++;
                    }
                }
                log.info("Loaded [{}] from [{}] in {}ms. Failures {}", success, file, (System.currentTimeMillis() - ts), failure);
            } catch (Throwable t) {
                log.error("Failed to load data from [{}]", file, t);
            }
        });
    }

    private static TenantId tenantId(String id) {
        return TenantId.fromUUID(UUID.fromString(id));
    }

    private static CustomerId customerId(String id) {
        var c = new CustomerId(UUID.fromString(id));
        return c.isNullUid() ? null : c;
    }

    private static EntityId entityId(String type, String id) {
        return EntityIdFactory.getByTypeAndId(type, id);
    }

    private static UUID uuid(String id) {
        return UUID.fromString(id);
    }

    private static long parseLong(String time) {
        return Long.parseLong(time);
    }

    private static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

}
