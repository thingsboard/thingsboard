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

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0000' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'default',
	'Test Device A1',
	'test device a1'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0001' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'default',
	'Test Device A2',
	'test device a2'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0002' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0000' ),
	'default',
	'Test Device A3',
	'test device a3'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0003' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0001' ),
	'default',
	'Test Device B1',
	'test device b1'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text)
VALUES (
	minTimeuuid ( '2016-11-01 01:02:05+0004' ),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( '2016-11-01 01:02:03+0002' ),
	'default',
	'Test Device C1',
	'test device c1'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text, additional_info)
VALUES (
	c8f1a6f0-b993-11e6-8a04-9ff4e1b7933c,
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( 0 ),
	'default',
	'DHT11 Demo Device',
	'dht11 demo device',
	'{"description":"Demo device that is used in sample applications that upload data from DHT11 temperature and humidity sensor"}'
);

INSERT INTO thingsboard.device ( id, tenant_id, customer_id, type, name, search_text, additional_info)
VALUES (
	c8f1a6f0-b993-11e6-8a04-9ff4e1b7933d,
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( 0 ),
	'default',
	'Raspberry Pi Demo Device',
	'raspberry pi demo device',
	'{"description":"Demo device that is used in Raspberry Pi GPIO control sample application"}'
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

INSERT INTO thingsboard.device_credentials ( id, device_id, credentials_type, credentials_id)
VALUES (
	now(),
	c8f1a6f0-b993-11e6-8a04-9ff4e1b7933d,
	'ACCESS_TOKEN',
	'RASPBERRY_PI_DEMO_TOKEN'
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
       "host": "smtp.sendgrid.net",
       "port": 2525,
       "username": "apikey",
       "password": "your_api_key",
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

INSERT INTO thingsboard.dashboard ( id, tenant_id, customer_id, configuration, title, search_text)
VALUES (
	now(),
	minTimeuuid ( '2016-11-01 01:02:01+0000' ),
	minTimeuuid ( 0 ),
	'{"description":"Demo dashboard for Raspberry PI GPIO Demo","widgets":[{"isSystemType":true,"bundleAlias":"gpio_widgets","typeAlias":"raspberry_pi_gpio_control","type":"rpc","title":"New widget","sizeX":6,"sizeY":10,"config":{"targetDeviceAliases":[],"showTitle":true,"backgroundColor":"#fff","color":"rgba(0, 0, 0, 0.87)","padding":"0px","settings":{"parseGpioStatusFunction":"return body[pin] === true;","gpioStatusChangeRequest":{"method":"setGpioStatus","paramsBody":"{\n   \"pin\": \"{$pin}\",\n   \"enabled\": \"{$enabled}\"\n}"},"requestTimeout":500,"switchPanelBackgroundColor":"#008a00","gpioStatusRequest":{"method":"getGpioStatus","paramsBody":"{}"},"gpioList":[{"pin":7,"label":"GPIO 4 (GPCLK0)","row":3,"col":0,"_uniqueKey":0},{"pin":11,"label":"GPIO 17","row":5,"col":0,"_uniqueKey":1},{"pin":12,"label":"GPIO 18","row":5,"col":1,"_uniqueKey":2},{"_uniqueKey":3,"pin":13,"label":"GPIO 27","row":6,"col":0},{"_uniqueKey":4,"pin":15,"label":"GPIO 22","row":7,"col":0},{"_uniqueKey":5,"pin":16,"label":"GPIO 23","row":7,"col":1},{"_uniqueKey":6,"pin":18,"label":"GPIO 24","row":8,"col":1},{"_uniqueKey":7,"pin":22,"label":"GPIO 25","row":10,"col":1},{"_uniqueKey":8,"pin":29,"label":"GPIO 5","row":14,"col":0},{"_uniqueKey":9,"pin":31,"label":"GPIO 6","row":15,"col":0},{"_uniqueKey":10,"pin":32,"label":"GPIO 12","row":15,"col":1},{"_uniqueKey":11,"pin":33,"label":"GPIO 13","row":16,"col":0},{"_uniqueKey":12,"pin":35,"label":"GPIO 19","row":17,"col":0},{"_uniqueKey":13,"pin":36,"label":"GPIO 16","row":17,"col":1},{"_uniqueKey":14,"pin":37,"label":"GPIO 26","row":18,"col":0},{"_uniqueKey":15,"pin":38,"label":"GPIO 20","row":18,"col":1},{"_uniqueKey":16,"pin":40,"label":"GPIO 21","row":19,"col":1}]},"title":"Raspberry Pi GPIO Control Panel","datasources":[],"targetDeviceAliasIds":["1"]},"row":0,"col":0},{"isSystemType":true,"bundleAlias":"gpio_widgets","typeAlias":"raspberry_pi_gpio_panel","type":"latest","title":"New widget","sizeX":7,"sizeY":10,"config":{"showTitle":true,"backgroundColor":"#fff","color":"rgba(0, 0, 0, 0.87)","padding":"0px","settings":{"gpioList":[{"pin":1,"label":"3.3V","row":0,"col":0,"color":"#fc9700","_uniqueKey":0},{"pin":2,"label":"5V","row":0,"col":1,"color":"#fb0000","_uniqueKey":1},{"pin":3,"label":"GPIO 2 (I2C1_SDA)","row":1,"col":0,"color":"#02fefb","_uniqueKey":2},{"color":"#fb0000","pin":4,"label":"5V","row":1,"col":1},{"color":"#02fefb","pin":5,"label":"GPIO 3 (I2C1_SCL)","row":2,"col":0},{"color":"#000000","pin":6,"label":"GND","row":2,"col":1},{"color":"#00fd00","pin":7,"label":"GPIO 4 (GPCLK0)","row":3,"col":0},{"color":"#fdfb00","pin":8,"label":"GPIO 14 (UART_TXD)","row":3,"col":1},{"color":"#000000","pin":9,"label":"GND","row":4,"col":0},{"color":"#fdfb00","pin":10,"label":"GPIO 15 (UART_RXD)","row":4,"col":1},{"color":"#00fd00","pin":11,"label":"GPIO 17","row":5,"col":0},{"color":"#00fd00","pin":12,"label":"GPIO 18","row":5,"col":1},{"color":"#00fd00","pin":13,"label":"GPIO 27","row":6,"col":0},{"color":"#000000","pin":14,"label":"GND","row":6,"col":1},{"color":"#00fd00","pin":15,"label":"GPIO 22","row":7,"col":0},{"color":"#00fd00","pin":16,"label":"GPIO 23","row":7,"col":1},{"color":"#fc9700","pin":17,"label":"3.3V","row":8,"col":0},{"color":"#00fd00","pin":18,"label":"GPIO 24","row":8,"col":1},{"color":"#fd01fd","pin":19,"label":"GPIO 10 (SPI_MOSI)","row":9,"col":0},{"color":"#000000","pin":20,"label":"GND","row":9,"col":1},{"color":"#fd01fd","pin":21,"label":"GPIO 9 (SPI_MISO)","row":10,"col":0},{"color":"#00fd00","pin":22,"label":"GPIO 25","row":10,"col":1},{"color":"#fd01fd","pin":23,"label":"GPIO 11 (SPI_SCLK)","row":11,"col":0},{"color":"#fd01fd","pin":24,"label":"GPIO 8 (SPI_CE0)","row":11,"col":1},{"color":"#000000","pin":25,"label":"GND","row":12,"col":0},{"color":"#fd01fd","pin":26,"label":"GPIO 7 (SPI_CE1)","row":12,"col":1},{"color":"#ffffff","pin":27,"label":"ID_SD","row":13,"col":0},{"color":"#ffffff","pin":28,"label":"ID_SC","row":13,"col":1},{"color":"#00fd00","pin":29,"label":"GPIO 5","row":14,"col":0},{"color":"#000000","pin":30,"label":"GND","row":14,"col":1},{"color":"#00fd00","pin":31,"label":"GPIO 6","row":15,"col":0},{"color":"#00fd00","pin":32,"label":"GPIO 12","row":15,"col":1},{"color":"#00fd00","pin":33,"label":"GPIO 13","row":16,"col":0},{"color":"#000000","pin":34,"label":"GND","row":16,"col":1},{"color":"#00fd00","pin":35,"label":"GPIO 19","row":17,"col":0},{"color":"#00fd00","pin":36,"label":"GPIO 16","row":17,"col":1},{"color":"#00fd00","pin":37,"label":"GPIO 26","row":18,"col":0},{"color":"#00fd00","pin":38,"label":"GPIO 20","row":18,"col":1},{"color":"#000000","pin":39,"label":"GND","row":19,"col":0},{"color":"#00fd00","pin":40,"label":"GPIO 21","row":19,"col":1}],"ledPanelBackgroundColor":"#008a00"},"title":"Raspberry Pi GPIO Status Panel","datasources":[{"type":"device","dataKeys":[{"name":"7","type":"attribute","label":"7","color":"#2196f3","settings":{},"_hash":0.20925966435886978},{"name":"11","type":"attribute","label":"11","color":"#4caf50","settings":{},"_hash":0.330267349594344},{"name":"12","type":"attribute","label":"12","color":"#f44336","settings":{},"_hash":0.5040578704481748},{"name":"13","type":"attribute","label":"13","color":"#ffc107","settings":{},"_hash":0.588956328191639},{"name":"15","type":"attribute","label":"15","color":"#607d8b","settings":{},"_hash":0.9229040530336119},{"name":"16","type":"attribute","label":"16","color":"#9c27b0","settings":{},"_hash":0.8692315253041654},{"name":"18","type":"attribute","label":"18","color":"#8bc34a","settings":{},"_hash":0.41465562857521543},{"name":"22","type":"attribute","label":"22","color":"#3f51b5","settings":{},"_hash":0.36135260043112827},{"name":"29","type":"attribute","label":"29","color":"#e91e63","settings":{},"_hash":0.9904592276182183},{"name":"31","type":"attribute","label":"31","color":"#ffeb3b","settings":{},"_hash":0.038330985429919195},{"name":"32","type":"attribute","label":"32","color":"#03a9f4","settings":{},"_hash":0.4334683890135089},{"name":"33","type":"attribute","label":"33","color":"#ff9800","settings":{},"_hash":0.6487255992492305},{"name":"35","type":"attribute","label":"35","color":"#673ab7","settings":{},"_hash":0.971555321150732},{"name":"36","type":"attribute","label":"36","color":"#cddc39","settings":{},"_hash":0.7826129728424382},{"name":"37","type":"attribute","label":"37","color":"#009688","settings":{},"_hash":0.44925676517537627},{"name":"38","type":"attribute","label":"38","color":"#795548","settings":{},"_hash":0.051518155759787465},{"name":"40","type":"attribute","label":"40","color":"#00bcd4","settings":{},"_hash":0.8733296686871144}],"deviceAliasId":1,"name":"RPi"}],"timewindow":{"realtime":{"timewindowMs":60000}}},"row":0,"col":6}],"deviceAliases":{"1":{"alias":"RPi","deviceId":"c8f1a6f0-b993-11e6-8a04-9ff4e1b7933d"}}}',
    'Raspberry PI GPIO Demo Dashboard',
	'raspberry pi gpio demo dashboard'
);