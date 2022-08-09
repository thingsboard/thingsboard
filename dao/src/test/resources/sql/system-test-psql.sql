--PostgreSQL specific truncate to fit constraints
CREATE table ts_kv_2021_09();
CREATE table ts_kv_2022_07();
CREATE table ts_kv_2022_09();
CREATE table ts_kv_2023_01();
TRUNCATE TABLE device_credentials, device, device_profile, ota_package, rule_node_state, rule_node, rule_chain;