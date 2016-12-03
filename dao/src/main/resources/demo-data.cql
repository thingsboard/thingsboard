--
-- Copyright © 2016 The Thingsboard Authors
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

/** Demo data **/

/** Demo tenant **/

INSERT INTO thingsboard.tenant ( id, region, title, search_text )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Global',
	'Tenant',
	'tenant'
);

/** Demo tenant admin **/

INSERT INTO thingsboard.user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:02+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( 0 ),
	'tenant@thingsboard.org',
	'tenant@thingsboard.org',
	'TENANT_ADMIN'
);

INSERT INTO thingsboard.user_credentials ( id, user_id, enabled, password )
VALUES (
	now ( ),
	minTimeuuid ( '2016-11-01 01:02:02+0000' ),
	true,
	'$2a$10$CUHks/PiEvxSGCKzrHCQGe/MoseIQw6qijIDjSa2sNoIyXkgJGyMO'
);

/** Demo customers **/

INSERT INTO thingsboard.customer ( id, tenant_id, title, search_text )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Customer A',
	'customer a'
);

INSERT INTO thingsboard.customer ( id, tenant_id, title, search_text )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:03+0001' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Customer B',
	'customer b'
);

INSERT INTO thingsboard.customer ( id, tenant_id, title, search_text )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:03+0002' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Customer C',
	'customer c'
);

/** Demo customer user **/

INSERT INTO thingsboard.user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:04+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'customer@thingsboard.org',
	'customer@thingsboard.org',
	'CUSTOMER_USER'
);

INSERT INTO thingsboard.user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:04+0001' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'customerA@thingsboard.org',
	'customera@thingsboard.org',
	'CUSTOMER_USER'
);

INSERT INTO thingsboard.user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:04+0002' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0001' ),
	'customerB@thingsboard.org',
	'customerb@thingsboard.org',
	'CUSTOMER_USER'
);

INSERT INTO thingsboard.user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES (
	minTimeuuid ( '2016-11-01 01:02:04+0003' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0002' ),
	'customerC@thingsboard.org',
	'customerc@thingsboard.org',
	'CUSTOMER_USER'
);


INSERT INTO thingsboard.user_credentials ( id, user_id, enabled, password )
VALUES (
	now ( ),
	minTimeuuid ( '2016-11-01 01:02:04+0000' ),
	true,
	'$2a$10$1Ki3Nl10pagxZncDQZtU.uHttir3HGKzLeovxCNKdSSJa3PU49L1C'
);

INSERT INTO thingsboard.user_credentials ( id, user_id, enabled, password )
VALUES (
	now ( ),
	minTimeuuid ( '2016-11-01 01:02:04+0001' ),
	true,
	'$2a$10$1Ki3Nl10pagxZncDQZtU.uHttir3HGKzLeovxCNKdSSJa3PU49L1C'
);

INSERT INTO thingsboard.user_credentials ( id, user_id, enabled, password )
VALUES (
	now ( ),
	minTimeuuid ( '2016-11-01 01:02:04+0002' ),
	true,
	'$2a$10$1Ki3Nl10pagxZncDQZtU.uHttir3HGKzLeovxCNKdSSJa3PU49L1C'
);

INSERT INTO thingsboard.user_credentials ( id, user_id, enabled, password )
VALUES (
	now ( ),
	minTimeuuid ( '2016-11-01 01:02:04+0003' ),
	true,
	'$2a$10$1Ki3Nl10pagxZncDQZtU.uHttir3HGKzLeovxCNKdSSJa3PU49L1C'
);

/** Demo device **/

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'Test Device A1',
	'test device a1'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0001' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'Test Device A2',
	'test device a2'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0002' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'Test Device A3',
	'test device a3'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0003' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0001' ),
	'Test Device B1',
	'test device b1'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0004' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0002' ),
	'Test Device C1',
	'test device c1'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, name, search_text, additional_info)
VALUES (
	c8f1a6f0-b993-11e6-8a04-9ff4e1b7933c,
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( 0 ),
	'DHT11 Demo Device',
	'dht11 demo device',
	'{"description":"Demo device that is used in sample applications that upload data from DHT11 temperature and humidity sensor"}'
);


INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:05+0000' ),
	'ACCESS_TOKEN',
	'A1_TEST_TOKEN'
);

INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:05+0001' ),
	'ACCESS_TOKEN',
	'A2_TEST_TOKEN'
);

INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:05+0002' ),
	'ACCESS_TOKEN',
	'A3_TEST_TOKEN'
);

INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:05+0003' ),
	'ACCESS_TOKEN',
	'B1_TEST_TOKEN'
);

INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:05+0004' ),
	'ACCESS_TOKEN',
	'C1_TEST_TOKEN'
);

INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	c8f1a6f0-b993-11e6-8a04-9ff4e1b7933c,
	'ACCESS_TOKEN',
	'DHT11_DEMO_TOKEN'
);

/** Demo data **/

/** Demo plugins & rules **/

/** Email plugin. Please change username and password here or via configuration **/

INSERT INTO thingsboard.plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:06+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
    'Demo Email Plugin',
    'ACTIVE',
	'mail sender plugin',
	'mail',
	'org.thingsboard.server.extensions.core.plugin.mail.MailPlugin',
	true,
	'{
       "host": "smtp.gmail.com",
       "port": 587,
       "username": "username@gmail.com",
       "password": "password",
       "otherProperties": [
         {
           "key":"mail.smtp.auth",
           "value":"true"
         },
         {
           "key":"mail.smtp.timeout",
           "value":"10000"
         },
         {
           "key":"mail.smtp.starttls.enable",
           "value":"true"
         },
         {
           "key":"mail.smtp.host",
           "value":"smtp.gmail.com"
         },
         {
           "key":"mail.smtp.port",
           "value":"587"
         }
       ]
     }'
);

INSERT INTO thingsboard.plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:07+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
    'Demo Time RPC Plugin',
    'ACTIVE',
	'demo time rpc plugin',
	'time',
	'org.thingsboard.server.extensions.core.plugin.time.TimePlugin',
	false,
	'{"timeFormat":"yyyy MM dd HH:mm:ss.SSS"}'
);

INSERT INTO thingsboard.plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:08+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
    'Demo Device Messaging RPC Plugin',
    'ACTIVE',
	'demo device messaging rpc plugin',
	'messaging',
	'org.thingsboard.server.extensions.core.plugin.messaging.DeviceMessagingPlugin',
	false,
	'{"maxDeviceCountPerCustomer":1024,"defaultTimeout":20000,"maxTimeout":60000}'
);


INSERT INTO thingsboard.rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:09+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Demo Alarm Rule',
	'mail',
    'ACTIVE',
	'demo alarm rule',
	0,
	'[{"clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter", "name":"MsgTypeFilter", "configuration": {"messageTypes":["POST_TELEMETRY","POST_ATTRIBUTES","GET_ATTRIBUTES"]}}
	,
	{"clazz":"org.thingsboard.server.extensions.core.filter.DeviceTelemetryFilter", "name":"Temperature filter", "configuration": {"filter":"typeof temperature !== ''undefined'' && temperature >= 100"}}
	]',
	'{"clazz":"org.thingsboard.server.extensions.core.processor.AlarmDeduplicationProcessor", "name": "AlarmDeduplicationProcessor", "configuration":{
                                                                                                                                "alarmIdTemplate": "[$date.get(''yyyy-MM-dd HH:mm'')] Device $cs.get(''serialNumber'')($cs.get(''model'')) temperature is high!",
                                                                                                                                "alarmBodyTemplate": "[$date.get(''yyyy-MM-dd HH:mm:ss'')] Device $cs.get(''serialNumber'')($cs.get(''model'')) temperature is $temp.valueAsString!"
                                                                                                                              }}',
	'{"clazz":"org.thingsboard.server.extensions.core.action.mail.SendMailAction", "name":"Send Mail Action", "configuration":{
                                                                                                                                "sendFlag": "isNewAlarm",
                                                                                                                                "fromTemplate": "thingsboard@gmail.com",
                                                                                                                                "toTemplate": "thingsboard@gmail.com",
                                                                                                                                "subjectTemplate": "$alarmId",
                                                                                                                                "bodyTemplate": "$alarmBody"
                                                                                                                              }}'
);

INSERT INTO thingsboard.rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:10+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Demo Alarm Rule',
	'mail',
    'ACTIVE',
	'demo alarm rule',
	0,
	'[{"clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter", "name":"MsgTypeFilter", "configuration": {"messageTypes":["POST_TELEMETRY","POST_ATTRIBUTES","GET_ATTRIBUTES"]}}
	,
	{"clazz":"org.thingsboard.server.extensions.core.filter.DeviceTelemetryFilter", "name":"Temperature filter", "configuration": {"filter":"typeof temperature !== ''undefined'' && temperature >= 100"}}
	]',
	'{"clazz":"org.thingsboard.server.extensions.core.processor.AlarmDeduplicationProcessor", "name": "AlarmDeduplicationProcessor", "configuration":{
                                                                                                                                "alarmIdTemplate": "[$date.get(''yyyy-MM-dd HH:mm'')] Device $cs.get(''serialNumber'')($cs.get(''model'')) temperature is high!",
                                                                                                                                "alarmBodyTemplate": "[$date.get(''yyyy-MM-dd HH:mm:ss'')] Device $cs.get(''serialNumber'')($cs.get(''model'')) temperature is $temperature.valueAsString!"
                                                                                                                              }}',
	'{"clazz":"org.thingsboard.server.extensions.core.action.mail.SendMailAction", "name":"Send Mail Action", "configuration":{
                                                                                                                                "sendFlag": "isNewAlarm",
                                                                                                                                "fromTemplate": "thingsboard@gmail.com",
                                                                                                                                "toTemplate": "thingsboard@gmail.com",
                                                                                                                                "subjectTemplate": "$alarmId",
                                                                                                                                "bodyTemplate": "$alarmBody"
                                                                                                                              }}'
);

INSERT INTO thingsboard.rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:10+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Demo getTime RPC Rule',
	'time',
    'ACTIVE',
	'demo alarm rule',
	0,
	'[{"configuration":{"messageTypes":["RPC_REQUEST"]},"name":"RPC Request Filter","clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter"},{"configuration":{"methodNames":[{"name":"getTime"}]},"name":"getTime method filter","clazz":"org.thingsboard.server.extensions.core.filter.MethodNameFilter"}]',
	null,
	'{"configuration":{},"clazz":"org.thingsboard.server.extensions.core.action.rpc.RpcPluginAction","name":"getTimeAction"}'
);

INSERT INTO thingsboard.rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:11+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Demo Time RPC Rule',
	'time',
    'ACTIVE',
	'demo time rpc rule',
	0,
	'[{"configuration":{"messageTypes":["RPC_REQUEST"]},"name":"RPC Request Filter","clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter"},{"configuration":{"methodNames":[{"name":"getTime"}]},"name":"getTime method filter","clazz":"org.thingsboard.server.extensions.core.filter.MethodNameFilter"}]',
	null,
	'{"configuration":{},"clazz":"org.thingsboard.server.extensions.core.action.rpc.RpcPluginAction","name":"getTimeAction"}'
);

INSERT INTO thingsboard.rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:12+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	'Demo Messaging RPC Rule',
	'messaging',
    'ACTIVE',
	'demo messaging rpc rule',
	0,
	'[{"configuration":{"messageTypes":["RPC_REQUEST"]},"name":"RPC Request Filter","clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter"},{"configuration":{"methodNames":[{"name":"getDevices"},{"name":"sendMsg"}]},"name":"Messaging methods filter","clazz":"org.thingsboard.server.extensions.core.filter.MethodNameFilter"}]',
	null,
	'{"configuration":{},"clazz":"org.thingsboard.server.extensions.core.action.rpc.RpcPluginAction","name":"Messaging RPC Action"}'
);

/** Demo dashboards **/

INSERT INTO thingsboard.dashboard ( id, tenant_id, customer_id, configuration, title, search_text)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( 0 ),
	'{"description":"Demo dashboard for sample applications that upload temperature and humidity received from DHT11 or DHT22 sensors","widgets":[{"isSystemType":true,"bundleAlias":"digital_gauges","typeAlias":"digital_thermometer","type":"latest","title":"New widget","sizeX":5,"sizeY":5,"config":{"datasources":[{"type":"device","dataKeys":[{"name":"temperature","type":"timeseries","label":"temperature","color":"#2196f3","settings":{},"_hash":0.3720839051412099}],"deviceAliasId":1,"name":"DHT11"}],"timewindow":{"realtime":{"timewindowMs":60000}},"showTitle":false,"backgroundColor":"#000000","color":"rgba(0, 0, 0, 0.87)","padding":"0px","settings":{"maxValue":50,"donutStartAngle":90,"showValue":true,"showMinMax":true,"gaugeWidthScale":1,"levelColors":["#304ffe","#7e57c2","#ff4081","#d32f2f"],"refreshAnimationType":"<>","refreshAnimationTime":700,"startAnimationType":"<>","startAnimationTime":700,"titleFont":{"family":"RobotoDraft","size":12,"style":"normal","weight":"500"},"labelFont":{"family":"RobotoDraft","size":8,"style":"normal","weight":"500"},"valueFont":{"family":"Segment7Standard","style":"normal","weight":"500","size":18},"minMaxFont":{"family":"Segment7Standard","size":12,"style":"normal","weight":"500"},"dashThickness":1.5,"decimals":0,"minValue":0,"units":"°C","gaugeColor":"#333333","neonGlowBrightness":35,"gaugeType":"donut","showTitle":false},"title":"Temperature"},"row":0,"col":0},{"isSystemType":true,"bundleAlias":"digital_gauges","typeAlias":"digital_vertical_bar","type":"latest","title":"New widget","sizeX":3,"sizeY":5,"config":{"datasources":[{"type":"device","dataKeys":[{"name":"humidity","type":"timeseries","label":"humidity","color":"#2196f3","settings":{},"_hash":0.9492802776509441}],"deviceAliasId":"1","name":"DHT11"}],"timewindow":{"realtime":{"timewindowMs":60000}},"showTitle":false,"backgroundColor":"#000000","color":"rgba(0, 0, 0, 0.87)","padding":"0px","settings":{"maxValue":100,"donutStartAngle":90,"showValue":true,"showMinMax":true,"gaugeWidthScale":0.75,"levelColors":["#3d5afe","#f44336"],"refreshAnimationType":"<>","refreshAnimationTime":700,"startAnimationType":"<>","startAnimationTime":700,"titleFont":{"family":"RobotoDraft","size":12,"style":"normal","weight":"500"},"labelFont":{"family":"RobotoDraft","size":8,"style":"normal","weight":"500"},"valueFont":{"family":"Segment7Standard","style":"normal","weight":"500","size":14},"minMaxFont":{"family":"Segment7Standard","size":8,"style":"normal","weight":"normal","color":"#cccccc"},"neonGlowBrightness":20,"decimals":0,"showUnitTitle":true,"gaugeColor":"#171a1c","gaugeType":"verticalBar","showTitle":false,"minValue":0,"dashThickness":1.2},"title":"Humidity"},"row":0,"col":5}],"deviceAliases":{"1":{"alias":"DHT11","deviceId":"c8f1a6f0-b993-11e6-8a04-9ff4e1b7933c"}}}',
    'Temperature & Humidity Demo Dashboard',
	'temperature & humidity demo dashboard'
);