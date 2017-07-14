--
-- Copyright © 2016-2017 The Thingsboard Authors
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

/** SYSTEM **/

/** System admin **/
INSERT INTO tb_user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES ( '1e746125a797660a91992ebcb67fe33', '1b21dd2138140008080808080808080', '1b21dd2138140008080808080808080', 'sysadmin@thingsboard.org',
         'sysadmin@thingsboard.org', 'SYS_ADMIN' );

INSERT INTO user_credentials ( id, user_id, enabled, password )
VALUES ( '1e7461261441950a91992ebcb67fe33', '1e746125a797660a91992ebcb67fe33', true,
         '$2a$10$5JTB8/hxWc9WAy62nCGSxeefl3KWmipA9nFpVdDa0/xfIseeBB4Bu' );

/** System settings **/
INSERT INTO admin_settings ( id, key, json_value )
VALUES ( '1e746126a2266e4a91992ebcb67fe33', 'general', '{
	"baseUrl": "http://localhost:8080"
}' );

INSERT INTO admin_settings ( id, key, json_value )
VALUES ( '1e746126eaaefa6a91992ebcb67fe33', 'mail', '{
	"mailFrom": "Thingsboard <sysadmin@localhost.localdomain>",
	"smtpProtocol": "smtp",
	"smtpHost": "localhost",
	"smtpPort": "25",
	"timeout": "10000",
	"enableTls": "false",
	"username": "",
	"password": ""
}' );

/** System plugins and rules **/
INSERT INTO plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration )
VALUES ( '1e7461160cb2da2a91992ebcb67fe33', '1b21dd2138140008080808080808080', 'System Telemetry Plugin', 'ACTIVE',
         'system telemetry plugin', 'telemetry',
         'org.thingsboard.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin', true, '{}' );

INSERT INTO rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action )
VALUES ( '1e7461165abad4ca91992ebcb67fe33', '1b21dd2138140008080808080808080', 'System Telemetry Rule', 'telemetry', 'ACTIVE',
         'system telemetry rule', 0,
         '[{"clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter", "name":"TelemetryFilter", "configuration": {"messageTypes":["POST_TELEMETRY","POST_ATTRIBUTES","GET_ATTRIBUTES"]}}]',
         null,
         '{"clazz":"org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction", "name":"TelemetryMsgConverterAction", "configuration":{}}'
);

INSERT INTO plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration )
VALUES ( '1e746116b3b8994a91992ebcb67fe33', '1b21dd2138140008080808080808080', 'System RPC Plugin', 'ACTIVE',
         'system rpc plugin', 'rpc', 'org.thingsboard.server.extensions.core.plugin.rpc.RpcPlugin', true, '{
       "defaultTimeout": 20000
     }' );
