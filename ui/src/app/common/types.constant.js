/*
 * Copyright © 2016 The Thingsboard Authors
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
            datasourceType: {
                function: "function",
                device: "device"
            },
            dataKeyType: {
                timeseries: "timeseries",
                attribute: "attribute",
                function: "function"
            },
            componentType: {
                filter: "FILTER",
                processor: "PROCESSOR",
                action: "ACTION",
                plugin: "PLUGIN"
            },
            entityType: {
                tenant: "TENANT",
                device: "DEVICE",
                customer: "CUSTOMER",
                rule: "RULE",
                plugin: "PLUGIN"
            },
            eventType: {
                alarm: {
                    value: "ALARM",
                    name: "event.type-alarm"
                },
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
            deviceAttributesScope: {
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
                }
            },
            systemBundleAlias: {
                charts: "charts",
                cards: "cards"
            }
        }
    ).name;
