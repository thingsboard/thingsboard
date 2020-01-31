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
import './gateway-form.scss';
/* eslint-disable import/no-unresolved, import/default */

import gatewayFormTemplate from './gateway-form.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.gatewayForm', [])
    .directive('tbGatewayForm', GatewayForm)
    .name;

/*@ngInject*/
function GatewayForm() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            formId: '=',
            ctx: '='
        },
        controller: GatewayFormController,
        controllerAs: 'vm',
        templateUrl: gatewayFormTemplate
    };
}

/*@ngInject*/
function GatewayFormController($scope, $injector, $document, $mdExpansionPanel, toast, importExport, attributeService, deviceService, userService, $mdDialog, $mdUtil, types, $window, $q, entityService) {
    let vm = this;
    const attributeNameClient = "current_configuration";
    const attributeNameServer = "configuration_drafts";
    const attributeNameShared = "configuration";
    const attributeLogShared = "RemoteLoggingLevel";

    vm.remoteLoggingConfig = '[loggers]}}keys=root, service, connector, converter, tb_connection, storage, extension}}[handlers]}}keys=consoleHandler, serviceHandler, connectorHandler, converterHandler, tb_connectionHandler, storageHandler, extensionHandler}}[formatters]}}keys=LogFormatter}}[logger_root]}}level=ERROR}}handlers=consoleHandler}}[logger_connector]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=connector}}[logger_storage]}}level={ERROR}}}handlers=storageHandler}}formatter=LogFormatter}}qualname=storage}}[logger_tb_connection]}}level={ERROR}}}handlers=tb_connectionHandler}}formatter=LogFormatter}}qualname=tb_connection}}[logger_service]}}level={ERROR}}}handlers=serviceHandler}}formatter=LogFormatter}}qualname=service}}[logger_converter]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=converter}}[logger_extension]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=extension}}[handler_consoleHandler]}}class=StreamHandler}}level={ERROR}}}formatter=LogFormatter}}args=(sys.stdout,)}}[handler_connectorHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}connector.log", "d", 1, 7,)}}[handler_storageHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}storage.log", "d", 1, 7,)}}[handler_serviceHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}service.log", "d", 1, 7,)}}[handler_converterHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}converter.log", "d", 1, 3,)}}[handler_extensionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}extension.log", "d", 1, 3,)}}[handler_tb_connectionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}tb_connection.log", "d", 1, 3,)}}[formatter_LogFormatter]}}format="%(asctime)s - %(levelname)s - [%(filename)s] - %(module)s - %(lineno)d - %(message)s" }}datefmt="%Y-%m-%d %H:%M:%S"';

    vm.types = types;

    vm.configurations = {
        singleSelect: '',
        host: $document[0].domain,
        port: 1883,
        remoteConfiguration: true,
        accessToken: '',
        entityType: '',
        entityId: '',
        storageType: "memoryStorage",
        readRecordsCount: 100,
        maxRecordsCount: 10000,
        dataFolderPath: './data/',
        maxFilesCount: 5,
        securityType: "accessToken",
        caCertPath: '/etc/thingsboard-gateway/ca.pem',
        privateKeyPath: '/etc/thingsboard-gateway/privateKey.pem',
        certPath: '/etc/thingsboard-gateway/certificate.pem',
        connectors: {},
        remoteLoggingLevel: "DEBUG",
        remoteLoggingPathToLogs: './logs/'
    };

    vm.securityTypes = [{
        name: 'gateway.security-types.access-token',
        value: 'accessToken'
    }, {
        name: 'gateway.security-types.tls',
        value: 'tls'
    }];

    vm.storageTypes = [{
        name: 'gateway.storage-types.memory-storage',
        value: 'memoryStorage'
    }, {
        name: 'gateway.storage-types.file-storage',
        value: 'fileStorage'
    }];

    $scope.$watch('vm.ctx', function () {
        if (vm.ctx ) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            initializeConfig();
        }
    });

    $scope.$on('gateway-form-resize', function (event, formId) {
        if (vm.formId == formId) {
            updateWidgetDisplaying();
        }
    });

    function updateWidgetDisplaying() {
        vm.changeAlignment = (vm.ctx.$container[0].offsetWidth <= 425);
    }

    function initializeConfig() {
        updateWidgetDisplaying();
        getGatewaysListByUser(true);
    }

    vm.getAccessToken = (deviceId) => {
        if (deviceId.id) {
        //     deviceService.findByName(deviceObj.name, {ignoreErrors: true})
        //         .then(
        //             function (device) {
            getDeviceCredential(deviceId.id);
                //     }
                // )
        }
    };

    function getDeviceCredential(deviceId) {
        return deviceService.getDeviceCredentials(deviceId).then(
            (deviceCredentials) => {
                vm.configurations.accessToken = deviceCredentials.credentialsId;
                vm.configurations.entityType = deviceCredentials.deviceId.entityType;
                vm.configurations.entityId = deviceCredentials.deviceId.id;
                vm.getAttributeStart();
            }
        );
    }

    vm.createDevice = (deviceObj) => {
        deviceService.findByName(deviceObj.name, {ignoreErrors: true})
            .then(
                function (device) {
                    getDeviceCredential(device.id.id).then(() => {
                        getGatewaysListByUser();
                    });
                },
                function () {
                    deviceService.saveDevice(deviceObj).then(
                        (device) => {
                            deviceService.getDeviceCredentials(device.id.id).then(
                                (data) => {
                                    vm.configurations.accessToken = data.credentialsId;
                                    vm.configurations.entityType = device.id.entityType;
                                    vm.configurations.entityId = device.id.id;
                                    vm.getAttributeStart();
                                    getGatewaysListByUser();
                                }
                            );
                        }
                    );
                });
    };

    vm.saveAttributeConfig = () => {
        saveAttribute(attributeNameShared, $window.btoa(angular.toJson(vm.getConfigAllByAttributeJSON())), types.attributesScope.shared.value);
        saveAttribute(attributeNameServer, $window.btoa(angular.toJson(vm.getConfigByAttributeTmpJSON())), types.attributesScope.server.value);
        saveAttribute(attributeLogShared, vm.configurations.remoteLoggingLevel.toUpperCase(), types.attributesScope.shared.value);
    };

    vm.getAttributeStart = () => {
        let initResps = [];
        vm.configurations.connectors = {};
        initResps.push(vm.getAttributeConfig(attributeNameClient, types.attributesScope.client.value));
        initResps.push(vm.getAttributeConfig(attributeNameServer, types.attributesScope.server.value));
        initResps.push(vm.getAttributeConfig(attributeLogShared, types.attributesScope.shared.value));
        $q.all(initResps).then((resp) => {
            vm.getAttributeInitFromClient(resp[0]);
            vm.getAttributeInitFromServer(resp[1]);
            vm.getAttributeInitFromShared(resp[2]);
        });
    };

    vm.getAttributeConfig = (attributeName, typeValue) => {
        let keys = [attributeName];
        return attributeService.getEntityAttributesValues(vm.configurations.entityType, vm.configurations.entityId, typeValue, keys);
    };

    function saveAttribute(attributeName, attributeConfig, typeValue) {
        let attributes = [{
            key: attributeName,
            value: attributeConfig
        }];
        attributeService.saveEntityAttributes(vm.configurations.entityType, vm.configurations.entityId, typeValue, attributes).then(() => {
        });
    }

    vm.exportConfig = () => {
        let fileZip = {};
        fileZip["tb_gateway.yaml"] = generateConfigFileYAML();
        vm.createConfigByExport(fileZip);
        vm.getLogsConfigByExport(fileZip);
        importExport.exportJSZip(fileZip, 'config');
        saveAttribute(attributeLogShared, vm.configurations.remoteLoggingLevel.toUpperCase(), types.attributesScope.shared.value);
    };

    function generateConfigFileYAML() {
        let config;
        config = 'thingsboard:\n';
        config += '  host: ' + vm.configurations.host + '\n';
        config += '  remoteConfiguration: ' + vm.configurations.remoteConfiguration + '\n';
        config += '  port: ' + vm.configurations.port + '\n';
        config += '  security:\n';
        if (vm.configurations.securityType === 'accessToken') {
            config += '    access-token: ' + vm.configurations.accessToken + '\n';
        } else if (vm.configurations.securityType === 'tls') {
            config += '    ca_cert: ' + vm.configurations.caCertPath + '\n';
            config += '    privateKey: ' + vm.configurations.privateKeyPath + '\n';
            config += '    cert: ' + vm.configurations.certPath + '\n';
        }
        config += 'storage:\n';
        if (vm.configurations.storageType === 'memoryStorage') {
            config += '  type: memory\n';
            config += '  read_records_count: ' + vm.configurations.readRecordsCount + '\n';
            config += '  max_records_count: ' + vm.configurations.maxRecordsCount + '\n';
        } else if (vm.configurations.storageType === 'fileStorage') {
            config += '  type: file\n';
            config += '  data_folder_path: ' + vm.configurations.dataFolderPath + '\n';
            config += '  max_file_count: ' + vm.configurations.maxFilesCount + '\n';
            config += '  max_read_records_count: ' + vm.configurations.readRecordsCount + '\n';
            config += '  max_records_per_file: ' + vm.configurations.maxRecordsCount + '\n';
        }
        config += 'connectors:\n';
        for (let connector in vm.configurations.connectors) {
            if (vm.configurations.connectors[connector].enabled) {
                config += '  -\n';
                config += '    name: ' + connector + ' Connector\n';
                config += '    type: ' + vm.configurations.connectors[connector].connector + '\n';
                config += '    configuration: ' + generateFileName(connector) + ".json" + '\n';
            }
        }
        return config;
    }

    vm.createConfigByExport = (fileZipAdd) => {
        for (let connector in vm.configurations.connectors) {
            if (vm.configurations.connectors[connector].enabled) {
                fileZipAdd[generateFileName(connector) + ".json"] = angular.toJson(vm.configurations.connectors[connector].config);
            }
        }
    };

    vm.getLogsConfigByExport = (fileZipAdd) => {
        fileZipAdd["logs.conf"] = vm.getLogsConfig();
    };

    vm.getLogsConfig = () => {
        return vm.remoteLoggingConfig
            .replace(/{ERROR}/g, vm.configurations.remoteLoggingLevel)
            .replace(/{.\/logs\/}/g, vm.configurations.remoteLoggingPathToLogs);
    };

    vm.getConfigAllByAttributeJSON = () => {
        let thingsBoardAll = {};
        thingsBoardAll["thingsboard"] = vm.getConfigMainByAttributeJSON();
        vm.getConfigByAttributeJSON(thingsBoardAll);
        return thingsBoardAll;
    };

    vm.getConfigMainByAttributeJSON = () => {
        let configMain = {};
        let thingsBoard = {};
        thingsBoard.host = vm.configurations.host;
        thingsBoard.remoteConfiguration = vm.configurations.remoteConfiguration;
        thingsBoard.port = vm.configurations.port;
        let security = {};
        if (vm.configurations.securityType === 'accessToken') {
            security.accessToken = (vm.configurations.accessToken) ? vm.configurations.accessToken : ""
        } else {
            security.caCert = vm.configurations.caCertPath;
            security.privateKey = vm.configurations.privateKeyPath;
            security.cert = vm.configurations.certPath;
        }
        thingsBoard.security = security;
        configMain.thingsboard = thingsBoard;

        let storage = {};
        if (vm.configurations.storageType === 'memoryStorage') {
            storage.type = "memory";
            storage.read_records_count = vm.configurations.readRecordsCount;
            storage.max_records_count = vm.configurations.maxRecordsCount;
        } else if (vm.configurations.storageType === 'fileStorage') {
            storage.type = "file";
            storage.data_folder_path = vm.configurations.dataFolderPath;
            storage.max_file_count = vm.configurations.maxFilesCount;
            storage.max_read_records_count = vm.configurations.readRecordsCount;
            storage.max_records_per_file = vm.configurations.maxRecordsCount;
        }
        configMain.storage = storage;

        let conn = [];
        for (let connector in vm.configurations.connectors) {
            if (vm.configurations.connectors[connector].enabled) {
                let connect = {};
                connect.configuration = generateFileName(connector) + ".json";
                connect.name = connector;
                connect.type = vm.configurations.connectors[connector].connector;
                conn.push(connect);
            }
        }
        configMain.connectors = conn;

        configMain.logs = $window.btoa(vm.getLogsConfig());

        return configMain;
    };

    vm.getConfigByAttributeJSON = (thingsBoardBy) => {
        for (let connector in vm.configurations.connectors) {
            if (vm.configurations.connectors[connector].enabled) {
                let typeAr = vm.configurations.connectors[connector].connector;
                let objTypeAll = [];
                for (let conn in vm.configurations.connectors) {
                    if (typeAr === vm.configurations.connectors[conn].connector && vm.configurations.connectors[conn].enabled) {
                        let objType = {};
                        objType["name"] = conn;
                        objType["config"] = vm.configurations.connectors[conn].config;
                        objTypeAll.push(objType);
                    }
                }
                if (objTypeAll.length > 0) {
                    thingsBoardBy[typeAr] = objTypeAll;
                }
            }
        }
    };

    vm.getConfigByAttributeTmpJSON = () => {
        let connects = {};
        for (let connector in vm.configurations.connectors) {
            if (!vm.configurations.connectors[connector].enabled && Object.keys(vm.configurations.connectors[connector].config).length !== 0) {
                let conn = {};
                conn["connector"] = vm.configurations.connectors[connector].connector;
                conn["config"] = vm.configurations.connectors[connector].config;
                connects[connector] = conn;
            }
        }
        return connects;
    };

    function getGatewaysListByUser(firstInit) {
        vm.gateways = [];
        entityService.getEntitiesByNameFilter(types.entityType.device, "", -1).then((devices) => {
            for (let i = 0; i < devices.length; i++) {
                const device = devices[i];
                if (device.additionalInfo !== null && device.additionalInfo.gateway === true) {
                    vm.gateways.push({
                        name: device.name,
                        id: device.id
                    });
                    if (firstInit && vm.gateways.length && device.name === vm.gateways[0].name) {
                        vm.configurations.singleSelect = vm.gateways[0];
                        vm.getAccessToken(device.id);
                    }
                }
            }
        });
    }

    vm.getAttributeInitFromClient = (resp) => {
        if (resp.length > 0) {
            vm.configurations.connectors = {};
            let attribute = angular.fromJson($window.atob(resp[0].value));
            for (var type in attribute) {
                let keyVal = attribute[type];
                if (type === "thingsboard") {
                    if (keyVal !== null && Object.keys(keyVal).length > 0) {
                        vm.setConfigMain(keyVal);
                    }
                } else {
                    for (let typeVal in keyVal) {
                        let typeName = '';
                        if (Object.prototype.hasOwnProperty.call(keyVal[typeVal], 'name')) {
                            typeName = 'name';
                        }
                        let key = "";
                        key = (typeName === "") ? "No name" : ((typeName === 'name') ? keyVal[typeVal].name : keyVal[typeVal][typeName].name);
                        let conn = {};
                        conn["enabled"] = true;
                        conn["connector"] = type;
                        conn["config"] = angular.toJson(keyVal[typeVal].config);
                        vm.configurations.connectors[key] = conn;
                    }
                }
            }
        }
    };

    vm.getAttributeInitFromServer = (resp) => {
        if (resp.length > 0) {
            let attribute = angular.fromJson($window.atob(resp[0].value));
            for (let key in attribute) {
                let conn = {};
                conn["enabled"] = false;
                conn["connector"] = attribute[key].connector;
                conn["config"] = angular.toJson(attribute[key].config);
                vm.configurations.connectors[key] = conn;
            }
        }
    };

    vm.getAttributeInitFromShared = (resp) => {
        if (resp.length > 0) {
            if (vm.types.gatewayLogLevel[resp[0].value.toLowerCase()]) {
                vm.configurations.remoteLoggingLevel = resp[0].value.toUpperCase();
            }
        } else {
            vm.configurations.remoteLoggingLevel = vm.types.gatewayLogLevel.debug;
        }
    };

    vm.setConfigMain = (keyVal) => {
        if (Object.prototype.hasOwnProperty.call(keyVal, 'thingsboard')) {
            vm.configurations.host = keyVal.thingsboard.host;
            vm.configurations.port = keyVal.thingsboard.port;
            vm.configurations.remoteConfiguration = keyVal.thingsboard.remoteConfiguration;
            if (Object.prototype.hasOwnProperty.call(keyVal.thingsboard.security, 'accessToken')) {
                vm.configurations.securityType = 'accessToken';
                vm.configurations.accessToken = keyVal.thingsboard.security.accessToken;
            } else {
                vm.configurations.securityType = 'tls';
                vm.configurations.caCertPath = keyVal.thingsboard.security.caCert;
                vm.configurations.privateKeyPath = keyVal.thingsboard.security.private_key;
                vm.configurations.certPath = keyVal.thingsboard.security.cert;
            }
        }
        if (Object.prototype.hasOwnProperty.call(keyVal, 'storage') && Object.prototype.hasOwnProperty.call(keyVal.storage, 'type')) {
            if (keyVal.storage.type === 'memory') {
                vm.configurations.storageType = 'memoryStorage';
                vm.configurations.readRecordsCount = keyVal.storage.read_records_count;
                vm.configurations.maxRecordsCount = keyVal.storage.max_records_count;
            } else if (keyVal.storage.type === 'file') {
                vm.configurations.storageType = 'fileStorage';
                vm.configurations.dataFolderPath = keyVal.storage.data_folder_path;
                vm.configurations.maxFilesCount = keyVal.storage.max_file_count;
                vm.configurations.readRecordsCount = keyVal.storage.read_records_count;
                vm.configurations.maxRecordsCount = keyVal.storage.max_records_count;
            }
        }
    };

    function generateFileName(fileName) {
        return fileName.replace("_", "")
            .replace("-", "")
            .replace(/^\s+|\s+$/g, '')
            .toLowerCase();
    }
}


