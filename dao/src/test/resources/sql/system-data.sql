--
-- Copyright © 2016-2020 The Thingsboard Authors
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
INSERT INTO tb_user ( id, created_time, tenant_id, customer_id, email, search_text, authority )
VALUES ( '5a797660-4612-11e7-a919-92ebcb67fe33', 1592576748000, '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080', 'sysadmin@thingsboard.org',
         'sysadmin@thingsboard.org', 'SYS_ADMIN' );

INSERT INTO user_credentials ( id, created_time, user_id, enabled, password )
VALUES ( '61441950-4612-11e7-a919-92ebcb67fe33', 1592576748000, '5a797660-4612-11e7-a919-92ebcb67fe33', true,
         '$2a$10$5JTB8/hxWc9WAy62nCGSxeefl3KWmipA9nFpVdDa0/xfIseeBB4Bu' );

/** System settings **/
INSERT INTO admin_settings ( id, created_time, key, json_value )
VALUES ( '6a2266e4-4612-11e7-a919-92ebcb67fe33', 1592576748000, 'general', '{
	"baseUrl": "http://localhost:8080"
}' );

INSERT INTO admin_settings ( id, created_time, key, json_value )
VALUES ( '6eaaefa6-4612-11e7-a919-92ebcb67fe33', 1592576748000, 'mail', '{
	"mailFrom": "Thingsboard <sysadmin@localhost.localdomain>",
	"smtpProtocol": "smtp",
	"smtpHost": "localhost",
	"smtpPort": "25",
	"timeout": "10000",
	"enableTls": false,
	"tlsVersion": "TLSv1.2",
	"username": "",
	"password": ""
}' );

INSERT INTO queue (id, created_time, tenant_id, name, topic, poll_interval, partitions, pack_processing_timeout, submit_strategy, processing_strategy)
VALUES ('6a441950-4611-11e7-a919-92ebcb67fe33', 1592576748000, '13814000-1dd2-11b2-8080-808080808080', 'Main', 'tb_rule_engine.main', 25, 10, 60000, '{"type":"BURST","batchSize":1000}', '{"type":"SKIP_ALL_FAILURES","retries":3,"failurePercentage":0.0,"pauseBetweenRetries":3,"maxPauseBetweenRetries":3}')
