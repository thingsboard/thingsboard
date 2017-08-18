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
export default angular.module('thingsboard.types', [])
    .constant('types',
        {
            serverErrorCode: {
                general: 2,
                authentication: 10,
                jwtTokenExpired: 11,
                permissionDenied: 20,
                invalidArguments: 30,
                badRequestParams: 31,
                itemNotFound: 32
            },
            entryPoints: {
                login: "/api/auth/login",
                tokenRefresh: "/api/auth/token",
                nonTokenBased: "/api/noauth"
            },
            id: {
                nullUid: "13814000-1dd2-11b2-8080-808080808080",
            },
            aggregation: {
                min: {
                    value: "MIN",
                    name: "aggregation.min"
                },
                max: {
                    value: "MAX",
                    name: "aggregation.max"
                },
                avg: {
                    value: "AVG",
                    name: "aggregation.avg"
                },
                sum: {
                    value: "SUM",
                    name: "aggregation.sum"
                },
                count: {
                    value: "COUNT",
                    name: "aggregation.count"
                },
                none: {
                    value: "NONE",
                    name: "aggregation.none"
                }
            },
            alarmFields: {
                createdTime: {
                    keyName: 'createdTime',
                    value: "createdTime",
                    name: "alarm.created-time",
                    time: true
                },
                startTime: {
                    keyName: 'startTime',
                    value: "startTs",
                    name: "alarm.start-time",
                    time: true
                },
                endTime: {
                    keyName: 'endTime',
                    value: "endTs",
                    name: "alarm.end-time",
                    time: true
                },
                ackTime: {
                    keyName: 'ackTime',
                    value: "ackTs",
                    name: "alarm.ack-time",
                    time: true
                },
                clearTime: {
                    keyName: 'clearTime',
                    value: "clearTs",
                    name: "alarm.clear-time",
                    time: true
                },
                originator: {
                    keyName: 'originator',
                    value: "originatorName",
                    name: "alarm.originator"
                },
                originatorType: {
                    keyName: 'originatorType',
                    value: "originator.entityType",
                    name: "alarm.originator-type"
                },
                type: {
                    keyName: 'type',
                    value: "type",
                    name: "alarm.type"
                },
                severity: {
                    keyName: 'severity',
                    value: "severity",
                    name: "alarm.severity"
                },
                status: {
                    keyName: 'status',
                    value: "status",
                    name: "alarm.status"
                }
            },
            alarmStatus: {
                activeUnack: "ACTIVE_UNACK",
                activeAck: "ACTIVE_ACK",
                clearedUnack: "CLEARED_UNACK",
                clearedAck: "CLEARED_ACK"
            },
            alarmSearchStatus: {
                any: "ANY",
                active: "ACTIVE",
                cleared: "CLEARED",
                ack: "ACK",
                unack: "UNACK"
            },
            alarmSeverity: {
                "CRITICAL": {
                    name: "alarm.severity-critical",
                    class: "tb-critical",
                    color: "red"
                },
                "MAJOR": {
                    name: "alarm.severity-major",
                    class: "tb-major",
                    color: "orange"
                },
                "MINOR": {
                    name: "alarm.severity-minor",
                    class: "tb-minor",
                    color: "#ffca3d"
                },
                "WARNING": {
                    name: "alarm.severity-warning",
                    class: "tb-warning",
                    color: "#abab00"
                },
                "INDETERMINATE": {
                    name: "alarm.severity-indeterminate",
                    class: "tb-indeterminate",
                    color: "green"
                }
            },
            aliasFilterType: {
                singleEntity: {
                    value: 'singleEntity',
                    name: 'alias.filter-type-single-entity'
                },
                entityList: {
                    value: 'entityList',
                    name: 'alias.filter-type-entity-list'
                },
                entityName: {
                    value: 'entityName',
                    name: 'alias.filter-type-entity-name'
                },
                stateEntity: {
                    value: 'stateEntity',
                    name: 'alias.filter-type-state-entity'
                },
                assetType: {
                    value: 'assetType',
                    name: 'alias.filter-type-asset-type'
                },
                deviceType: {
                    value: 'deviceType',
                    name: 'alias.filter-type-device-type'
                },
                relationsQuery: {
                    value: 'relationsQuery',
                    name: 'alias.filter-type-relations-query'
                },
                assetSearchQuery: {
                    value: 'assetSearchQuery',
                    name: 'alias.filter-type-asset-search-query'
                },
                deviceSearchQuery: {
                    value: 'deviceSearchQuery',
                    name: 'alias.filter-type-device-search-query'
                }
            },
            position: {
                top: {
                    value: "top",
                    name: "position.top"
                },
                bottom: {
                    value: "bottom",
                    name: "position.bottom"
                },
                left: {
                    value: "left",
                    name: "position.left"
                },
                right: {
                    value: "right",
                    name: "position.right"
                }
            },
            datasourceType: {
                function: "function",
                entity: "entity"
            },
            dataKeyType: {
                timeseries: "timeseries",
                attribute: "attribute",
                function: "function",
                alarm: "alarm"
            },
            componentType: {
                filter: "FILTER",
                processor: "PROCESSOR",
                action: "ACTION",
                plugin: "PLUGIN"
            },
            entityType: {
                device: "DEVICE",
                asset: "ASSET",
                rule: "RULE",
                plugin: "PLUGIN",
                tenant: "TENANT",
                customer: "CUSTOMER",
                user: "USER",
                dashboard: "DASHBOARD",
                alarm: "ALARM"
            },
            entityTypeTranslations: {
                "DEVICE": {
                    type: 'entity.type-device',
                    typePlural: 'entity.type-devices',
                    list: 'entity.list-of-devices',
                    nameStartsWith: 'entity.device-name-starts-with'
                },
                "ASSET": {
                    type: 'entity.type-asset',
                    typePlural: 'entity.type-assets',
                    list: 'entity.list-of-assets',
                    nameStartsWith: 'entity.asset-name-starts-with'
                },
                "RULE": {
                    type: 'entity.type-rule',
                    typePlural: 'entity.type-rules',
                    list: 'entity.list-of-rules',
                    nameStartsWith: 'entity.rule-name-starts-with'
                },
                "PLUGIN": {
                    type: 'entity.type-plugin',
                    typePlural: 'entity.type-plugins',
                    list: 'entity.list-of-plugins',
                    nameStartsWith: 'entity.plugin-name-starts-with'
                },
                "TENANT": {
                    type: 'entity.type-tenant',
                    typePlural: 'entity.type-tenants',
                    list: 'entity.list-of-tenants',
                    nameStartsWith: 'entity.tenant-name-starts-with'
                },
                "CUSTOMER": {
                    type: 'entity.type-customer',
                    typePlural: 'entity.type-customers',
                    list: 'entity.list-of-customers',
                    nameStartsWith: 'entity.customer-name-starts-with'
                },
                "USER": {
                    type: 'entity.type-user',
                    typePlural: 'entity.type-users',
                    list: 'entity.list-of-users',
                    nameStartsWith: 'entity.user-name-starts-with'
                },
                "DASHBOARD": {
                    type: 'entity.type-dashboard',
                    typePlural: 'entity.type-dashboards',
                    list: 'entity.list-of-dashboards',
                    nameStartsWith: 'entity.dashboard-name-starts-with'
                },
                "ALARM": {
                    type: 'entity.type-alarm',
                    typePlural: 'entity.type-alarms',
                    list: 'entity.list-of-alarms',
                    nameStartsWith: 'entity.alarm-name-starts-with'
                }
            },
            entitySearchDirection: {
                from: "FROM",
                to: "TO"
            },
            entityRelationType: {
                contains: "Contains",
                manages: "Manages"
            },
            eventType: {
                error: {
                    value: "ERROR",
                    name: "event.type-error"
                },
                lcEvent: {
                    value: "LC_EVENT",
                    name: "event.type-lc-event"
                },
                stats: {
                    value: "STATS",
                    name: "event.type-stats"
                }
            },
            latestTelemetry: {
                value: "LATEST_TELEMETRY",
                name: "attribute.scope-latest-telemetry",
                clientSide: true
            },
            attributesScope: {
                client: {
                    value: "CLIENT_SCOPE",
                    name: "attribute.scope-client",
                    clientSide: true
                },
                server: {
                    value: "SERVER_SCOPE",
                    name: "attribute.scope-server",
                    clientSide: false
                },
                shared: {
                    value: "SHARED_SCOPE",
                    name: "attribute.scope-shared",
                    clientSide: false
                }
            },
            valueType: {
                string: {
                    value: "string",
                    name: "value.string",
                    icon: "mdi:format-text"
                },
                integer: {
                    value: "integer",
                    name: "value.integer",
                    icon: "mdi:numeric"
                },
                double: {
                    value: "double",
                    name: "value.double",
                    icon: "mdi:numeric"
                },
                boolean: {
                    value: "boolean",
                    name: "value.boolean",
                    icon: "mdi:checkbox-marked-outline"
                }
            },
            widgetType: {
                timeseries: {
                    value: "timeseries",
                    name: "widget.timeseries",
                    template: {
                        bundleAlias: "charts",
                        alias: "basic_timeseries"
                    }
                },
                latest: {
                    value: "latest",
                    name: "widget.latest-values",
                    template: {
                        bundleAlias: "cards",
                        alias: "attributes_card"
                    }
                },
                rpc: {
                    value: "rpc",
                    name: "widget.rpc",
                    template: {
                        bundleAlias: "gpio_widgets",
                        alias: "basic_gpio_control"
                    }
                },
                alarm: {
                    value: "alarm",
                    name: "widget.alarm",
                    template: {
                        bundleAlias: "alarm_widgets",
                        alias: "alarms_table"
                    }
                },
                static: {
                    value: "static",
                    name: "widget.static",
                    template: {
                        bundleAlias: "cards",
                        alias: "html_card"
                    }
                }
            },
            widgetActionSources: {
                headerButton: {
                    name: 'widget-action.header-button',
                    value: 'headerButton',
                    multiple: true
                }
            },
            widgetActionTypes: {
                openDashboardState: {
                    name: 'widget-action.open-dashboard-state',
                    value: 'openDashboardState'
                },
                updateDashboardState: {
                    name: 'widget-action.update-dashboard-state',
                    value: 'updateDashboardState'
                },
                openDashboard: {
                    name: 'widget-action.open-dashboard',
                    value: 'openDashboard'
                },
                custom: {
                    name: 'widget-action.custom',
                    value: 'custom'
                }
            },
            systemBundleAlias: {
                charts: "charts",
                cards: "cards"
            },
            translate: {
                customTranslationsPrefix: "custom."
            }
        }
    ).name;
