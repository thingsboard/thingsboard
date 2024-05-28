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
package org.thingsboard.server.service.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.nio.ByteBuffer;
import java.util.UUID;

public class EntityUtils {

    static EasyRandom easyRandom;

    static {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(DeviceConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceConfiguration.class))
                .randomize(DeviceTransportConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceTransportConfiguration.class))
                .randomize(DeviceProfileConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceProfileConfiguration.class))
                .randomize(DeviceProfileTransportConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceProfileTransportConfiguration.class))
                .randomize(DeviceProfileProvisionConfiguration.class, () -> easyRandom.nextObject(AllowCreateNewDevicesDeviceProfileProvisionConfiguration.class))
                .randomize(DeviceProfileAlarm.class, DeviceProfileAlarm::new)
                .randomize(JsonNode.class, () -> JacksonUtil.newObjectNode()
                        .put(RandomStringUtils.randomAlphanumeric(10), RandomStringUtils.randomAlphanumeric(10))
                        .put(RandomStringUtils.randomAlphanumeric(10), easyRandom.nextDouble()))
                .randomize(EntityId.class, () -> new DeviceId(UUID.randomUUID()))
                .randomize(KvEntry.class, () -> easyRandom.nextBoolean() ? easyRandom.nextObject(StringDataEntry.class) : easyRandom.nextObject(DoubleDataEntry.class))
                .randomize(Long.class, () -> RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .randomize(ByteBuffer.class, () -> {
                    byte[] bytes = new byte[50];
                    easyRandom.nextBytes(bytes);
                    return ByteBuffer.wrap(bytes);
                });

        easyRandom = new EasyRandom(parameters);
    }

    public static <T> T newRandomizedEntity(Class<T> type) {
        return easyRandom.nextObject(type);
    }


    /**
     TENANT("tenant", "id"),
     CUSTOMER("customer"),
     ADMIN_SETTINGS("admin_settings"),
     QUEUE("queue"),
     RPC("rpc"),
     RULE_CHAIN("rule_chain"),
     OTA_PACKAGE("ota_package"), // TODO: drop constraint in ota_package for device_profile due to circular reference
     DEVICE_PROFILE("device_profile"),
     RESOURCE("resource"),
     ROLE("role"),
     ENTITY_GROUP("entity_group", Pair.of(
     "owner_id", of(TENANT, CUSTOMER)
     )),
     DEVICE_GROUP_OTA_PACKAGE("device_group_ota_package", Pair.of(
     "ota_package_id", of(OTA_PACKAGE)
     )),
     GROUP_PERMISSION("group_permission", tenantId -> {
     return "SELECT group_permission.*, role.name as role_name FROM group_permission INNER JOIN role " +
     "ON role_id = role.id WHERE ";
     }),
     BLOB_ENTITY("blob_entity", true, "created_time", "blob_entity"),
     SCHEDULER_EVENT("scheduler_event"),
     RULE_CHAIN_DEBUG_EVENT("rule_chain_debug_event", true, "ts", "debug_event"),
     RULE_NODE("rule_node", Pair.of(
     "rule_chain_id", of(RULE_CHAIN)
     )),
     RULE_NODE_DEBUG_EVENT("rule_node_debug_event", true, "ts", "debug_event"),
     CONVERTER("converter"),
     CONVERTER_DEBUG_EVENT("converter_debug_event", true, "ts", "debug_event"),
     INTEGRATION("integration"),
     INTEGRATION_DEBUG_EVENT("integration_debug_event", true, "ts", "debug_event"),
     USER("tb_user"),
     USER_CREDENTIALS("user_credentials", Pair.of(
     "user_id", of(USER)
     )),
     USER_AUTH_SETTINGS("user_auth_settings", Pair.of(
     "user_id", of(USER)
     )),
     EDGE("edge"),
     EDGE_EVENT("edge_event", true, "created_time", "edge_event"),
     WIDGETS_BUNDLE("widgets_bundle"),
     WIDGET_TYPE("widget_type"),
     WIDGETS_BUNDLE_WIDGET("widgets_bundle_widget", Pair.of(
     "widgets_bundle_id", of(WIDGETS_BUNDLE)
     ), of("widget_type_id")),
     DASHBOARD("dashboard"),
     DEVICE("device"),
     DEVICE_CREDENTIALS("device_credentials", Pair.of(
     "device_id", of(DEVICE)
     )),
     ASSET_PROFILE("asset_profile"),
     ASSET("asset"),
     ENTITY_VIEW("entity_view"),
     ALARM("alarm"),
     ENTITY_ALARM("entity_alarm", List.of("created_time", "entity_id")),
     ERROR_EVENT("error_event", true, "ts", "event"),
     LC_EVENT("lc_event", true, "ts", "event"),
     RAW_DATA_EVENT("raw_data_event", true, "ts", "event"),
     STATS_EVENT("stats_event", true, "ts", "event"),
     OAUTH2_PARAMS("oauth2_params"),
     OAUTH2_DOMAIN("oauth2_domain", Pair.of(
     "oauth2_params_id", of(OAUTH2_PARAMS)
     )),
     OAUTH2_MOBILE("oauth2_mobile", Pair.of(
     "oauth2_params_id", of(OAUTH2_PARAMS)
     )),
     OAUTH2_REGISTRATION("oauth2_registration", Pair.of(
     "oauth2_params_id", of(OAUTH2_PARAMS)
     )),
     RULE_NODE_STATE("rule_node_state", Pair.of(
     "entity_id", of(DEVICE)
     )),
     AUDIT_LOG("audit_log", true, "created_time", "audit_log"),
     USER_SETTINGS("user_settings", Pair.of(
     "user_id", of(USER)
     ), of("user_id")),
     NOTIFICATION_TARGET("notification_target"),
     NOTIFICATION_TEMPLATE("notification_template"),
     NOTIFICATION_RULE("notification_rule"),
     WHITE_LABELING("white_labeling", List.of("tenant_id", "customer_id", "type")),
     ALARM_TYPES("alarm_types", null, of("type")),
     *
     * */
}
