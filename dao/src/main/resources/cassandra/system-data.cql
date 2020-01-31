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
INSERT INTO thingsboard.user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES ( minTimeuuid ( '2016-11-01 01:01:01+0000' ), minTimeuuid ( 0 ), minTimeuuid ( 0 ), 'sysadmin@thingsboard.org',
'sysadmin@thingsboard.org', 'SYS_ADMIN' );

INSERT INTO thingsboard.user_credentials ( id, user_id, enabled, password )
VALUES ( now ( ), minTimeuuid ( '2016-11-01 01:01:01+0000' ), true,
'$2a$10$5JTB8/hxWc9WAy62nCGSxeefl3KWmipA9nFpVdDa0/xfIseeBB4Bu' );

/** System settings **/
INSERT INTO thingsboard.admin_settings ( id, key, json_value )
VALUES ( now ( ), 'general', '{
	"baseUrl": "http://localhost:8080"
}' );

INSERT INTO thingsboard.admin_settings ( id, key, json_value )
VALUES ( now ( ), 'mail', '{
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