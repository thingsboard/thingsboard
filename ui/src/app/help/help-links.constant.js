/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
var pluginClazzHelpLinkMap = {
    'org.thingsboard.server.extensions.core.plugin.messaging.DeviceMessagingPlugin': 'pluginDeviceMessaging',
    'org.thingsboard.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin': 'pluginTelemetryStorage',
    'org.thingsboard.server.extensions.core.plugin.rpc.RpcPlugin': 'pluginRpcPlugin',
    'org.thingsboard.server.extensions.core.plugin.mail.MailPlugin': 'pluginMailPlugin',
    'org.thingsboard.server.extensions.rest.plugin.RestApiCallPlugin': 'pluginRestApiCallPlugin',
    'org.thingsboard.server.extensions.core.plugin.time.TimePlugin': 'pluginTimePlugin',
    'org.thingsboard.server.extensions.kafka.plugin.KafkaPlugin': 'pluginKafkaPlugin',
    'org.thingsboard.server.extensions.rabbitmq.plugin.RabbitMqPlugin': 'pluginRabbitMqPlugin'

};

var filterClazzHelpLinkMap = {
    'org.thingsboard.server.extensions.core.filter.MsgTypeFilter': 'filterMsgType',
    'org.thingsboard.server.extensions.core.filter.DeviceTelemetryFilter': 'filterDeviceTelemetry',
    'org.thingsboard.server.extensions.core.filter.MethodNameFilter': 'filterMethodName',
    'org.thingsboard.server.extensions.core.filter.DeviceAttributesFilter': 'filterDeviceAttributes'
};

var processorClazzHelpLinkMap = {
    'org.thingsboard.server.extensions.core.processor.AlarmDeduplicationProcessor': 'processorAlarmDeduplication'
};

var pluginActionsClazzHelpLinkMap = {
    'org.thingsboard.server.extensions.core.action.rpc.RpcPluginAction': 'pluginActionRpc',
    'org.thingsboard.server.extensions.core.action.mail.SendMailAction': 'pluginActionSendMail',
    'org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction': 'pluginActionTelemetry',
    'org.thingsboard.server.extensions.kafka.action.KafkaPluginAction': 'pluginActionKafka',
    'org.thingsboard.server.extensions.rabbitmq.action.RabbitMqPluginAction': 'pluginActionRabbitMq',
    'org.thingsboard.server.extensions.rest.action.RestApiCallPluginAction': 'pluginActionRestApiCall'
};

var helpBaseUrl = "https://thingsboard.io";

export default angular.module('thingsboard.help', [])
    .constant('helpLinks',
        {
            linksMap: {
                outgoingMailSettings: helpBaseUrl + "/docs/user-guide/ui/mail-settings",
                plugins: helpBaseUrl + "/docs/user-guide/rule-engine/#plugins",
                pluginDeviceMessaging: helpBaseUrl + "/docs/reference/plugins/messaging/",
                pluginTelemetryStorage: helpBaseUrl + "/docs/reference/plugins/telemetry/",
                pluginRpcPlugin: helpBaseUrl + "/docs/reference/plugins/rpc/",
                pluginMailPlugin: helpBaseUrl + "/docs/reference/plugins/mail/",
                pluginRestApiCallPlugin: helpBaseUrl + "/docs/reference/plugins/rest/",
                pluginTimePlugin: helpBaseUrl + "/docs/reference/plugins/time/",
                pluginKafkaPlugin: helpBaseUrl + "/docs/reference/plugins/kafka/",
                pluginRabbitMqPlugin: helpBaseUrl + "/docs/reference/plugins/rabbitmq/",
                rules: helpBaseUrl + "/docs/user-guide/rule-engine/#rules",
                filters: helpBaseUrl + "/docs/user-guide/rule-engine/#filters",
                filterMsgType: helpBaseUrl + "/docs/reference/filters/message-type-filter",
                filterDeviceTelemetry: helpBaseUrl + "/docs/reference/filters/device-telemetry-filter",
                filterMethodName: helpBaseUrl + "/docs/reference/filters/method-name-filter/",
                filterDeviceAttributes: helpBaseUrl + "/docs/reference/filters/device-attributes-filter",
                processors: helpBaseUrl + "/docs/user-guide/rule-engine/#processors",
                processorAlarmDeduplication: "http://thingsboard.io/docs/#q=processorAlarmDeduplication",
                pluginActions: helpBaseUrl + "/docs/user-guide/rule-engine/#actions",
                pluginActionRpc: helpBaseUrl + "/docs/reference/actions/rpc-plugin-action",
                pluginActionSendMail: helpBaseUrl + "/docs/reference/actions/send-mail-action",
                pluginActionTelemetry: helpBaseUrl + "/docs/reference/actions/telemetry-plugin-action/",
                pluginActionKafka: helpBaseUrl + "/docs/reference/actions/kafka-plugin-action",
                pluginActionRabbitMq: helpBaseUrl + "/docs/reference/actions/rabbitmq-plugin-action",
                pluginActionRestApiCall: helpBaseUrl + "/docs/reference/actions/rest-api-call-plugin-action",
                tenants: helpBaseUrl + "/docs/user-guide/ui/tenants",
                customers: helpBaseUrl + "/docs/user-guide/ui/customers",
                assets: helpBaseUrl + "/docs/user-guide/ui/assets",
                devices: helpBaseUrl + "/docs/user-guide/ui/devices",
                dashboards: helpBaseUrl + "/docs/user-guide/ui/dashboards",
                users: helpBaseUrl + "/docs/user-guide/ui/users",
                widgetsBundles: helpBaseUrl + "/docs/user-guide/ui/widget-library#bundles",
                widgetsConfig:  helpBaseUrl + "/docs/user-guide/ui/dashboards#widget-configuration",
                widgetsConfigTimeseries:  helpBaseUrl + "/docs/user-guide/ui/dashboards#timeseries",
                widgetsConfigLatest: helpBaseUrl +  "/docs/user-guide/ui/dashboards#latest",
                widgetsConfigRpc: helpBaseUrl +  "/docs/user-guide/ui/dashboards#rpc",
                widgetsConfigAlarm: helpBaseUrl +  "/docs/user-guide/ui/dashboards#alarm",
                widgetsConfigStatic: helpBaseUrl +  "/docs/user-guide/ui/dashboards#static",
            },
            getPluginLink: function(plugin) {
                var link = 'plugins';
                if (plugin && plugin.clazz) {
                    if (pluginClazzHelpLinkMap[plugin.clazz]) {
                        link = pluginClazzHelpLinkMap[plugin.clazz];
                    }
                }
                return link;
            },
            getFilterLink: function(filter) {
                var link = 'filters';
                if (filter && filter.clazz) {
                    if (filterClazzHelpLinkMap[filter.clazz]) {
                        link = filterClazzHelpLinkMap[filter.clazz];
                    }
                }
                return link;
            },
            getProcessorLink: function(processor) {
                var link = 'processors';
                if (processor && processor.clazz) {
                    if (processorClazzHelpLinkMap[processor.clazz]) {
                        link = processorClazzHelpLinkMap[processor.clazz];
                    }
                }
                return link;
            },
            getPluginActionLink: function(pluginAction) {
                var link = 'pluginActions';
                if (pluginAction && pluginAction.clazz) {
                    if (pluginActionsClazzHelpLinkMap[pluginAction.clazz]) {
                        link = pluginActionsClazzHelpLinkMap[pluginAction.clazz];
                    }
                }
                return link;
            }
        }
    ).name;
