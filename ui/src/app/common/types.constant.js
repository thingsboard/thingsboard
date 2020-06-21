/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
                credentialsExpired: 15,
                permissionDenied: 20,
                invalidArguments: 30,
                badRequestParams: 31,
                itemNotFound: 32,
                tooManyRequests: 33,
                tooManyUpdates: 34
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
            auditLogActionType: {
                "ADDED": {
                    name: "audit-log.type-added"
                },
                "DELETED": {
                    name: "audit-log.type-deleted"
                },
                "UPDATED": {
                    name: "audit-log.type-updated"
                },
                "ATTRIBUTES_UPDATED": {
                    name: "audit-log.type-attributes-updated"
                },
                "ATTRIBUTES_DELETED": {
                    name: "audit-log.type-attributes-deleted"
                },
                "RPC_CALL": {
                    name: "audit-log.type-rpc-call"
                },
                "CREDENTIALS_UPDATED": {
                    name: "audit-log.type-credentials-updated"
                },
                "ASSIGNED_TO_CUSTOMER": {
                    name: "audit-log.type-assigned-to-customer"
                },
                "UNASSIGNED_FROM_CUSTOMER": {
                    name: "audit-log.type-unassigned-from-customer"
                },
                "ACTIVATED": {
                    name: "audit-log.type-activated"
                },
                "SUSPENDED": {
                    name: "audit-log.type-suspended"
                },
                "CREDENTIALS_READ": {
                    name: "audit-log.type-credentials-read"
                },
                "ATTRIBUTES_READ": {
                    name: "audit-log.type-attributes-read"
                },
                "RELATION_ADD_OR_UPDATE": {
                    name: "audit-log.type-relation-add-or-update"
                },
                "RELATION_DELETED": {
                    name: "audit-log.type-relation-delete"
                },
                "RELATIONS_DELETED": {
                    name: "audit-log.type-relations-delete"
                },
                "ALARM_ACK": {
                    name: "audit-log.type-alarm-ack"
                },
                "ALARM_CLEAR": {
                    name: "audit-log.type-alarm-clear"
                },
                "LOGIN": {
                    name: "audit-log.type-login"
                },
                "LOGOUT": {
                    name: "audit-log.type-logout"
                },
                "LOCKOUT": {
                    name: "audit-log.type-lockout"
                },
                "ASSIGNED_TO_EDGE": {
                    name: "audit-log.type-assigned-to-edge"
                },
                "UNASSIGNED_FROM_EDGE": {
                    name: "audit-log.type-unassigned-from-edge"
                }
            },
            auditLogActionStatus: {
                "SUCCESS": {
                    value: "SUCCESS",
                    name: "audit-log.status-success"
                },
                "FAILURE": {
                    value: "FAILURE",
                    name: "audit-log.status-failure"
                }
            },
            auditLogMode: {
                tenant: "tenant",
                entity: "entity",
                user: "user",
                customer: "customer"
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
                entityViewType: {
                    value: 'entityViewType',
                    name: 'alias.filter-type-entity-view-type'
                },
                edgeType: {
                    value: 'edgeType',
                    name: 'alias.filter-type-edge-type'
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
                },
                entityViewSearchQuery: {
                    value: 'entityViewSearchQuery',
                    name: 'alias.filter-type-entity-view-search-query'
                },
                edgeSearchQuery: {
                    value: 'edgeSearchQuery',
                    name: 'alias.filter-type-edge-search-query'
                }
            },
            direction: {
                column: {
                    value: "column",
                    name: "direction.column"
                },
                row: {
                    value: "row",
                    name: "direction.row"
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
                alarm: "alarm",
                entityField: "entityField"
            },
            contentType: {
                "JSON": {
                    value: "JSON",
                    name: "content-type.json",
                    code: "json"
                },
                "TEXT": {
                    value: "TEXT",
                    name: "content-type.text",
                    code: "text"
                },
                "BINARY": {
                    value: "BINARY",
                    name: "content-type.binary",
                    code: "text"
                }
            },
            componentType: {
                enrichment: "ENRICHMENT",
                filter: "FILTER",
                transformation: "TRANSFORMATION",
                action: "ACTION",
                external: "EXTERNAL"
            },
            entityType: {
                device: "DEVICE",
                asset: "ASSET",
                tenant: "TENANT",
                customer: "CUSTOMER",
                user: "USER",
                dashboard: "DASHBOARD",
                alarm: "ALARM",
                rulechain: "RULE_CHAIN",
                rulenode: "RULE_NODE",
                entityView: "ENTITY_VIEW",
                edge: "EDGE"
            },
            importEntityColumnType: {
                name: {
                    name: 'import.column-type.name',
                    value: 'name'
                },
                type: {
                    name: 'import.column-type.type',
                    value: 'type'
                },
                label: {
                    name: 'import.column-type.label',
                    value: 'label'
                },
                clientAttribute: {
                    name: 'import.column-type.client-attribute',
                    value: 'CLIENT_ATTRIBUTE'
                },
                sharedAttribute: {
                    name: 'import.column-type.shared-attribute',
                    value: 'SHARED_ATTRIBUTE'
                },
                serverAttribute: {
                    name: 'import.column-type.server-attribute',
                    value: 'SERVER_ATTRIBUTE'
                },
                timeseries: {
                    name: 'import.column-type.timeseries',
                    value: 'TIMESERIES'
                },
                entityField: {
                    name: 'import.column-type.entity-field',
                    value: 'ENTITY_FIELD'
                },
                accessToken: {
                    name: 'import.column-type.access-token',
                    value: 'ACCESS_TOKEN'
                },
                isGateway: {
                    name: 'import.column-type.isgateway',
                    value: 'gateway'
                },
                description: {
                    name: 'import.column-type.description',
                    value: 'description'
                }
            },
            aliasEntityType: {
                current_customer: "CURRENT_CUSTOMER",
                current_tenant: "CURRENT_TENANT"
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
                "ENTITY_VIEW": {
                    type: 'entity.type-entity-view',
                    typePlural: 'entity.type-entity-views',
                    list: 'entity.list-of-entity-views',
                    nameStartsWith: 'entity.entity-view-name-starts-with'
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
                },
                "RULE_CHAIN": {
                    type: 'entity.type-rulechain',
                    typePlural: 'entity.type-rulechains',
                    list: 'entity.list-of-rulechains',
                    nameStartsWith: 'entity.rulechain-name-starts-with'
                },
                "RULE_NODE": {
                    type: 'entity.type-rulenode',
                    typePlural: 'entity.type-rulenodes',
                    list: 'entity.list-of-rulenodes',
                    nameStartsWith: 'entity.rulenode-name-starts-with'
                },
                "CURRENT_CUSTOMER": {
                    type: 'entity.type-current-customer',
                    list: 'entity.type-current-customer'
                },
                "CURRENT_TENANT": {
                    type: 'entity.type-current-tenant',
                    list: 'entity.type-current-tenant'
                },
                "EDGE": {
                    type: 'entity.type-edge',
                    typePlural: 'entity.type-edges',
                    list: 'entity.list-of-edges',
                    nameStartsWith: 'entity.edge-name-starts-with'
                }
            },
            entityField: {
                createdTime: {
                    keyName: 'createdTime',
                    name: 'entity-field.created-time',
                    value: 'createdTime',
                    time: true
                },
                name: {
                    keyName: 'name',
                    name: 'entity-field.name',
                    value: 'name'
                },
                type: {
                    keyName: 'type',
                    name: 'entity-field.type',
                    value: 'type'
                },
                firstName: {
                    keyName: 'firstName',
                    name: 'entity-field.first-name',
                    value: 'firstName'
                },
                lastName: {
                    keyName: 'lastName',
                    name: 'entity-field.last-name',
                    value: 'lastName'
                },
                email: {
                    keyName: 'email',
                    name: 'entity-field.email',
                    value: 'email'
                },
                title: {
                    keyName: 'title',
                    name: 'entity-field.title',
                    value: 'title'
                },
                country: {
                    keyName: 'country',
                    name: 'entity-field.country',
                    value: 'country'
                },
                state: {
                    keyName: 'state',
                    name: 'entity-field.state',
                    value: 'state'
                },
                city: {
                    keyName: 'city',
                    name: 'entity-field.city',
                    value: 'city'
                },
                address: {
                    keyName: 'address',
                    name: 'entity-field.address',
                    value: 'address'
                },
                address2: {
                    keyName: 'address2',
                    name: 'entity-field.address2',
                    value: 'address2'
                },
                zip: {
                    keyName: 'zip',
                    name: 'entity-field.zip',
                    value: 'zip'
                },
                phone: {
                    keyName: 'phone',
                    name: 'entity-field.phone',
                    value: 'phone'
                },
                label: {
                    keyName: 'label',
                    name: 'entity-field.label',
                    value: 'label'
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
            debugEventType: {
                debugRuleNode: {
                    value: "DEBUG_RULE_NODE",
                    name: "event.type-debug-rule-node"
                },
                debugRuleChain: {
                    value: "DEBUG_RULE_CHAIN",
                    name: "event.type-debug-rule-chain"
                }
            },
            extensionType: {
                http: "HTTP",
                mqtt: "MQTT",
                opc: "OPC UA",
                modbus: "MODBUS"
            },
            gatewayConfigType: {
                mqtt:  {
                    value: "mqtt",
                    name: "MQTT"
                },
                modbus:  {
                    value: "modbus",
                    name: "Modbus"
                },
                opcua:  {
                    value: "opcua",
                    name: "OPC-UA"
                },
                ble:  {
                    value: "ble",
                    name: "BLE"
                },
                request:  {
                    value: "request",
                    name: "Request"
                },
                can:  {
                    value: "can",
                    name: "CAN"
                },
                bacnet: {
                    value: "bacnet",
                    name: "BACnet"
                },
                custom:  {
                    value: "custom",
                    name: "Custom"
                }
            },
            gatewayLogLevel: {
                none: "NONE",
                critical: "CRITICAL",
                error: "ERROR",
                warning: "WARNING",
                info: "INFO",
                debug: "DEBUG"
            },
            extensionValueType: {
                string: 'value.string',
                long: 'value.long',
                double: 'value.double',
                boolean: 'value.boolean'
            },
            extensionTransformerType: {
                toDouble: 'extension.to-double',
                custom: 'extension.custom'
            },
            mqttConverterTypes: {
                json: 'extension.converter-json',
                custom: 'extension.custom'
            },
            mqttCredentialTypes: {
                anonymous:  {
                    value: "anonymous",
                    name: "extension.anonymous"
                },
                basic: {
                    value: "basic",
                    name: "extension.basic"
                },
                pem: {
                    value: "cert.PEM",
                    name: "extension.pem"
                }
            },
            extensionOpcSecurityTypes: {
                Basic128Rsa15: "Basic128Rsa15",
                Basic256: "Basic256",
                Basic256Sha256: "Basic256Sha256",
                None: "None"
            },
            extensionIdentityType: {
                anonymous: "extension.anonymous",
                username: "extension.username"
            },
            extensionKeystoreType: {
                PKCS12: "PKCS12",
                JKS: "JKS"
            },
            extensionModbusFunctionCodes: {
                1: "Read Coils (1)",
                2: "Read Discrete Inputs (2)",
                3: "Read Multiple Holding Registers (3)",
                4: "Read Input Registers (4)"
            },
            extensionModbusTransports: {
                tcp: "TCP",
                udp: "UDP",
                rtu: "RTU"
            },
            extensionModbusRtuParities: {
                none: "none",
                even: "even",
                odd: "odd"
            },
            extensionModbusRtuEncodings: {
                ascii: "ascii",
                rtu: "rtu"
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
            coreRuleChainType: "CORE",
            edgeRuleChainType: "EDGE",
            ruleNodeTypeComponentTypes: ["FILTER", "ENRICHMENT", "TRANSFORMATION", "ACTION", "EXTERNAL"],
            ruleChainNodeComponent: {
                type: 'RULE_CHAIN',
                name: 'rule chain',
                clazz: 'tb.internal.RuleChain',
                configurationDescriptor: {
                    nodeDefinition: {
                        description: "",
                        details: "Forwards incoming messages to specified Rule Chain",
                        inEnabled: true,
                        outEnabled: false,
                        relationTypes: [],
                        customRelations: false,
                        defaultConfiguration: {}
                    }
                }
            },
            unknownNodeComponent: {
                type: 'UNKNOWN',
                name: 'unknown',
                clazz: 'tb.internal.Unknown',
                configurationDescriptor: {
                    nodeDefinition: {
                        description: "",
                        details: "",
                        inEnabled: true,
                        outEnabled: true,
                        relationTypes: [],
                        customRelations: false,
                        defaultConfiguration: {}
                    }
                }
            },
            inputNodeComponent: {
                type: 'INPUT',
                name: 'Input',
                clazz: 'tb.internal.Input'
            },
            ruleNodeType: {
                FILTER: {
                    value: "FILTER",
                    name: "rulenode.type-filter",
                    details: "rulenode.type-filter-details",
                    nodeClass: "tb-filter-type",
                    icon: "filter_list"
                },
                ENRICHMENT: {
                    value: "ENRICHMENT",
                    name: "rulenode.type-enrichment",
                    details: "rulenode.type-enrichment-details",
                    nodeClass: "tb-enrichment-type",
                    icon: "playlist_add"
                },
                TRANSFORMATION: {
                    value: "TRANSFORMATION",
                    name: "rulenode.type-transformation",
                    details: "rulenode.type-transformation-details",
                    nodeClass: "tb-transformation-type",
                    icon: "transform"
                },
                ACTION: {
                    value: "ACTION",
                    name: "rulenode.type-action",
                    details: "rulenode.type-action-details",
                    nodeClass: "tb-action-type",
                    icon: "flash_on"
                },
                EXTERNAL: {
                    value: "EXTERNAL",
                    name: "rulenode.type-external",
                    details: "rulenode.type-external-details",
                    nodeClass: "tb-external-type",
                    icon: "cloud_upload"
                },
                RULE_CHAIN: {
                    value: "RULE_CHAIN",
                    name: "rulenode.type-rule-chain",
                    details: "rulenode.type-rule-chain-details",
                    nodeClass: "tb-rule-chain-type",
                    icon: "settings_ethernet"
                },
                INPUT: {
                    value: "INPUT",
                    name: "rulenode.type-input",
                    details: "rulenode.type-input-details",
                    nodeClass: "tb-input-type",
                    icon: "input",
                    special: true
                },
                UNKNOWN: {
                    value: "UNKNOWN",
                    name: "rulenode.type-unknown",
                    details: "rulenode.type-unknown-details",
                    nodeClass: "tb-unknown-type",
                    icon: "help_outline"
                }
            },
            messageType: {
                'POST_ATTRIBUTES_REQUEST': {
                    name: 'Post attributes',
                    value: 'POST_ATTRIBUTES_REQUEST'
                },
                'POST_TELEMETRY_REQUEST': {
                    name: 'Post telemetry',
                    value: 'POST_TELEMETRY_REQUEST'
                },
                'TO_SERVER_RPC_REQUEST': {
                    name: 'RPC Request from Device',
                    value: 'TO_SERVER_RPC_REQUEST'
                },
                'RPC_CALL_FROM_SERVER_TO_DEVICE': {
                    name: 'RPC Request to Device',
                    value: 'RPC_CALL_FROM_SERVER_TO_DEVICE'
                },
                'ACTIVITY_EVENT': {
                    name: 'Activity Event',
                    value: 'ACTIVITY_EVENT'
                },
                'INACTIVITY_EVENT': {
                    name: 'Inactivity Event',
                    value: 'INACTIVITY_EVENT'
                },
                'CONNECT_EVENT': {
                    name: 'Connect Event',
                    value: 'CONNECT_EVENT'
                },
                'DISCONNECT_EVENT': {
                    name: 'Disconnect Event',
                    value: 'DISCONNECT_EVENT'
                },
                'ENTITY_CREATED': {
                    name: 'Entity Created',
                    value: 'ENTITY_CREATED'
                },
                'ENTITY_UPDATED': {
                    name: 'Entity Updated',
                    value: 'ENTITY_UPDATED'
                },
                'ENTITY_DELETED': {
                    name: 'Entity Deleted',
                    value: 'ENTITY_DELETED'
                },
                'ENTITY_ASSIGNED': {
                    name: 'Entity Assigned',
                    value: 'ENTITY_ASSIGNED'
                },
                'ENTITY_UNASSIGNED': {
                    name: 'Entity Unassigned',
                    value: 'ENTITY_UNASSIGNED'
                },
                'ATTRIBUTES_UPDATED': {
                    name: 'Attributes Updated',
                    value: 'ATTRIBUTES_UPDATED'
                },
                'ATTRIBUTES_DELETED': {
                    name: 'Attributes Deleted',
                    value: 'ATTRIBUTES_DELETED'
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
                },
                json: {
                    value: "json",
                    name: "value.json",
                    icon: "mdi:json"
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
                },
                customPretty: {
                    name: 'widget-action.custom-pretty',
                    value: 'customPretty'
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
