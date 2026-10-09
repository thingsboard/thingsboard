--
-- SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
-- SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

CREATE INDEX IF NOT EXISTS idx_entity_group_by_type_name_and_owner_id ON entity_group(type, name, owner_id);

CREATE INDEX IF NOT EXISTS idx_rpc_tenant_id_device_id ON rpc(tenant_id, device_id);

CREATE INDEX IF NOT EXISTS idx_customer_tenant_id_parent_customer_id ON customer(tenant_id, parent_customer_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_external_id ON rule_node(rule_chain_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_type ON rule_node(type);

CREATE INDEX IF NOT EXISTS idx_entity_group_external_id ON entity_group(external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_type_id_configuration_version ON rule_node(type, id, configuration_version);

CREATE INDEX IF NOT EXISTS idx_api_usage_state_entity_id ON api_usage_state(entity_id);

CREATE INDEX IF NOT EXISTS idx_scheduler_event_originator_id ON scheduler_event(tenant_id, originator_id);

CREATE INDEX IF NOT EXISTS idx_blob_entity_created_time ON blob_entity(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_blob_entity_id ON blob_entity(id);

CREATE INDEX IF NOT EXISTS idx_agent_customer_id ON agent(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_agent_agent_profile_id ON agent(agent_profile_id);

CREATE INDEX IF NOT EXISTS idx_agent_application_application_profile_id ON agent_application(application_profile_id);

CREATE INDEX IF NOT EXISTS idx_agent_application_tenant_id ON agent_application(tenant_id);

CREATE INDEX IF NOT EXISTS idx_agent_bulk_action_agent_profile_id ON agent_bulk_action(agent_profile_id, application_profile_id);

CREATE INDEX IF NOT EXISTS idx_agent_bulk_action_application_profile_id ON agent_bulk_action(application_profile_id);

CREATE INDEX IF NOT EXISTS idx_agent_bulk_action_created_time ON agent_bulk_action(created_time);

CREATE INDEX IF NOT EXISTS idx_agent_bulk_action_stuck ON agent_bulk_action(status)
    WHERE status IN ('QUEUED', 'IN_PROGRESS');

CREATE INDEX IF NOT EXISTS idx_agent_app_event_app_start_status ON agent_app_event(application_id, start_status);

CREATE INDEX IF NOT EXISTS idx_agent_app_event_bulk_action_processing_status ON agent_app_event(bulk_action_id, processing_status) WHERE bulk_action_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_agent_app_event_tenant_agent ON agent_app_event(tenant_id, agent_id);

CREATE INDEX IF NOT EXISTS idx_agent_app_event_updated_time ON agent_app_event(updated_time);

CREATE INDEX IF NOT EXISTS idx_agent_app_event_bulk_action_created_time ON agent_app_event(created_time) WHERE bulk_action_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_agent_app_event_agent_action_type ON agent_app_event(agent_id, action_type);

CREATE INDEX IF NOT EXISTS idx_agent_app_event_stuck_agent_sweep ON agent_app_event(updated_time)
    WHERE agent_scoped
    AND start_status = 'DELIVERED'
    AND (processing_status IS NULL OR processing_status NOT IN ('FINISHED', 'ERROR'));

-- At most one active agent-scoped event per agent
CREATE UNIQUE INDEX IF NOT EXISTS uq_agent_app_event_active_agent ON agent_app_event(agent_id)
    WHERE agent_scoped
    AND start_status IN ('PENDING', 'DELIVERED')
    AND (processing_status IS NULL OR processing_status NOT IN ('FINISHED', 'ERROR', 'START_FAILED'));

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

CREATE INDEX IF NOT EXISTS idx_group_permission_tenant_id ON group_permission(tenant_id);

CREATE INDEX IF NOT EXISTS idx_custom_menu ON custom_menu(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS mobile_app_bundle_tenant_id ON mobile_app_bundle(tenant_id);

CREATE INDEX IF NOT EXISTS idx_job_tenant_id ON job(tenant_id);

CREATE INDEX IF NOT EXISTS idx_encryption_key_tenant_id ON encryption_key(tenant_id);

CREATE INDEX IF NOT EXISTS idx_report_tenant_id_created_time ON report(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_report_id ON report(id);

CREATE INDEX IF NOT EXISTS idx_report_public_key ON report(public_key) WHERE public_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ai_model_tenant_id ON ai_model(tenant_id);

CREATE INDEX IF NOT EXISTS idx_report_template_tenant_id ON report_template(tenant_id);

CREATE INDEX IF NOT EXISTS idx_api_key_tenant_id_user_id ON api_key(tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_iot_hub_installed_item_tenant_id ON iot_hub_installed_item(tenant_id);

CREATE INDEX IF NOT EXISTS idx_iot_hub_installed_item_item_type ON iot_hub_installed_item(tenant_id, item_type);

CREATE INDEX IF NOT EXISTS idx_iot_hub_installed_item_item_id ON iot_hub_installed_item(tenant_id, item_id);
