--PostgreSQL specific truncate to fit constraints
TRUNCATE TABLE device_credentials, device, device_profile, asset, asset_profile, ota_package, rule_node_state, rule_node, rule_chain, alarm_comment, alarm, entity_alarm;

-- Decrease seq_id column to make sure to cover cases of new sequential cycle during the tests
ALTER SEQUENCE edge_event_seq_id_seq MAXVALUE 77;