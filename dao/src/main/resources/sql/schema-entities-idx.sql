--
-- Copyright © 2016-2026 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type ON alarm(originator_id, type, start_ts DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_originator_created_time ON alarm(originator_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_created_time ON alarm(tenant_id, created_time DESC);

-- Drop index by 'status' column and replace with new indexes that have only active alarms;
CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type_active
    ON alarm USING btree (originator_id, type) WHERE cleared = false;

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_alarm_type_active
    ON alarm USING btree (tenant_id, type) WHERE cleared = false;

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_alarm_type_created_time ON alarm(tenant_id, type, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_assignee_created_time ON alarm(tenant_id, assignee_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_entity_alarm_created_time ON entity_alarm(tenant_id, entity_id, created_time DESC);

-- Cover index by alarm type to optimize propagated alarm queries;
CREATE INDEX IF NOT EXISTS idx_entity_alarm_entity_id_alarm_type_created_time_alarm_id ON entity_alarm
USING btree (tenant_id, entity_id, alarm_type, created_time DESC) INCLUDE(alarm_id);

CREATE INDEX IF NOT EXISTS idx_entity_alarm_alarm_id ON entity_alarm(alarm_id);

CREATE INDEX IF NOT EXISTS idx_relation_to_id ON relation(relation_type_group, to_type, to_id);

CREATE INDEX IF NOT EXISTS idx_relation_from_id ON relation(relation_type_group, from_type, from_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id ON device(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id_and_type ON device(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_device_type ON device(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_device_device_profile_id ON device(tenant_id, device_profile_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id ON asset(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id_and_type ON asset(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_asset_type ON asset(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_asset_profile_id ON asset(tenant_id, asset_profile_id);

CREATE INDEX IF NOT EXISTS idx_attribute_kv_by_key_and_last_update_ts ON attribute_kv(entity_id, attribute_key, last_update_ts desc);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id_and_created_time ON audit_log(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_id ON audit_log(id);

CREATE INDEX IF NOT EXISTS idx_edge_event_tenant_id_and_created_time ON edge_event(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_edge_event_tenant_id_edge_id_created_time ON edge_event(tenant_id, edge_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_edge_event_id ON edge_event(id);

CREATE INDEX IF NOT EXISTS idx_rpc_tenant_id_device_id ON rpc(tenant_id, device_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_external_id ON rule_node(rule_chain_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_type_id_configuration_version ON rule_node(type, id, configuration_version);

CREATE INDEX IF NOT EXISTS idx_api_usage_state_entity_id ON api_usage_state(entity_id);

CREATE INDEX IF NOT EXISTS idx_alarm_comment_alarm_id ON alarm_comment(alarm_id);

CREATE INDEX IF NOT EXISTS idx_notification_target_tenant_id_created_time ON notification_target(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_template_tenant_id_created_time ON notification_template(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_rule_tenant_id_trigger_type_created_time ON notification_rule(tenant_id, trigger_type, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id_user_created_time ON notification_request(tenant_id, created_time DESC)
    WHERE originator_entity_type = 'USER';

CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id ON notification_request(tenant_id);

CREATE INDEX IF NOT EXISTS idx_notification_request_rule_id_originator_entity_id ON notification_request(rule_id, originator_entity_id)
    WHERE originator_entity_type = 'ALARM';

CREATE INDEX IF NOT EXISTS idx_notification_request_status ON notification_request(status)
    WHERE status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_notification_id ON notification(id);

CREATE INDEX IF NOT EXISTS idx_notification_notification_request_id ON notification(request_id);

CREATE INDEX IF NOT EXISTS idx_notification_delivery_method_recipient_id_created_time ON notification(delivery_method, recipient_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_delivery_method_recipient_id_unread ON notification(delivery_method, recipient_id) WHERE status <> 'READ';

CREATE INDEX IF NOT EXISTS idx_resource_etag ON resource(tenant_id, etag);

CREATE INDEX IF NOT EXISTS idx_resource_type_public_resource_key ON resource(resource_type, public_resource_key);

CREATE INDEX IF NOT EXISTS mobile_app_bundle_tenant_id ON mobile_app_bundle(tenant_id);

CREATE INDEX IF NOT EXISTS idx_job_tenant_id ON job(tenant_id);

CREATE INDEX IF NOT EXISTS idx_ai_model_tenant_id ON ai_model(tenant_id);

CREATE INDEX IF NOT EXISTS idx_api_key_user_id ON api_key(user_id);
