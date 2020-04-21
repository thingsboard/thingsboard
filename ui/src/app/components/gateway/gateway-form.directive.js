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
            ctx: '=',
            isStateForm: '=',
            deviceName: '=',
            isReadOnly: '=',
            isState: '='
        },
        controller: GatewayFormController,
        controllerAs: 'vm',
        templateUrl: gatewayFormTemplate,
    };
}

/*@ngInject*/
function GatewayFormController($scope, $injector, $document, $mdExpansionPanel, toast, importExport, attributeService, deviceService, userService, $mdDialog, $mdUtil, types, $window, $q, entityService, utils, $translate) {
    let vm = this;
    const currentConfigurationAttribute = "current_configuration";
    const configurationDraftsAttribute = "configuration_drafts";
    const configurationAttribute = "configuration";
    const remoteLoggingLevelAttribute = "RemoteLoggingLevel";

    const templateLogsConfig = '[loggers]}}keys=root, service, connector, converter, tb_connection, storage, extension}}[handlers]}}keys=consoleHandler, serviceHandler, connectorHandler, converterHandler, tb_connectionHandler, storageHandler, extensionHandler}}[formatters]}}keys=LogFormatter}}[logger_root]}}level=ERROR}}handlers=consoleHandler}}[logger_connector]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=connector}}[logger_storage]}}level={ERROR}}}handlers=storageHandler}}formatter=LogFormatter}}qualname=storage}}[logger_tb_connection]}}level={ERROR}}}handlers=tb_connectionHandler}}formatter=LogFormatter}}qualname=tb_connection}}[logger_service]}}level={ERROR}}}handlers=serviceHandler}}formatter=LogFormatter}}qualname=service}}[logger_converter]}}level={ERROR}}}handlers=converterHandler}}formatter=LogFormatter}}qualname=converter}}[logger_extension]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=extension}}[handler_consoleHandler]}}class=StreamHandler}}level={ERROR}}}formatter=LogFormatter}}args=(sys.stdout,)}}[handler_connectorHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}connector.log", "d", 1, 7,)}}[handler_storageHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}storage.log", "d", 1, 7,)}}[handler_serviceHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}service.log", "d", 1, 7,)}}[handler_converterHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}converter.log", "d", 1, 3,)}}[handler_extensionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}extension.log", "d", 1, 3,)}}[handler_tb_connectionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}tb_connection.log", "d", 1, 3,)}}[formatter_LogFormatter]}}format="%(asctime)s - %(levelname)s - [%(filename)s] - %(module)s - %(lineno)d - %(message)s" }}datefmt="%Y-%m-%d %H:%M:%S"';

    vm.types = types;
    vm.deviceNameForm = (vm.deviceName) ? vm.deviceName : null;
    vm.idForm = Math.random().toString(36).replace(/^0\.[0-9]*/, '');
    vm.configurations = {
        gateway: '',
        host: $document[0].domain,
        port: 1883,
        remoteConfiguration: true,
        accessToken: '',
        storageType: "memoryStorage",
        readRecordsCount: 100,
        maxRecordsCount: 10000,
        dataFolderPath: './data/',
        maxFilesCount: 5,
        securityType: "accessToken",
        caCertPath: '/etc/thingsboard-gateway/ca.pem',
        privateKeyPath: '/etc/thingsboard-gateway/privateKey.pem',
        certPath: '/etc/thingsboard-gateway/certificate.pem',
        connectors: [],
        remoteLoggingLevel: "DEBUG",
        remoteLoggingPathToLogs: './logs/'
    };

    let archiveFileName = '';
    let gatewayNameExists = '';
    let successfulSaved = '';
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
        if (vm.ctx) {
            vm.isStateForm = $scope.isStateForm;
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            if (vm.ctx.datasources && vm.ctx.datasources.length) {
                vm.deviceNameForm = vm.ctx.datasources[0].name;
            }
        }
        initializeConfig();
    });


    $scope.$on('gateway-form-resize', function (event, formId) {
        if (vm.formId == formId) {
            updateWidgetDisplaying();
        }
    });

    function updateWidgetDisplaying() {
        if (vm.ctx) {
            vm.changeAlignment = (vm.ctx.$container[0].offsetWidth <= 425);
        }
    }

    function initWidgetSettings() {
        let widgetTitle;
        if (vm.settings) {
            vm.isReadOnlyForm = (vm.settings.readOnly) ? vm.settings.readOnly : false;
            if (vm.settings.gatewayTitle && vm.settings.gatewayTitle.length) {
                widgetTitle = utils.customTranslation(vm.settings.gatewayTitle, vm.settings.gatewayTitle);
            }
        } else {
            vm.isReadOnlyForm = false;
            widgetTitle = $translate.instant('gateway.gateway');
        }
        if (vm.ctx) {
            vm.ctx.widgetTitle = widgetTitle;
        }

        archiveFileName = vm.settings && vm.settings.archiveFileName && vm.settings.archiveFileName.length ? vm.settings.archiveFileName : 'gatewayConfiguration';
        if (vm.settings) {
            gatewayNameExists = utils.customTranslation(vm.settings.deviceNameExist, vm.settings.deviceNameExist) || $translate.instant('gateway.gateway-exists');
            successfulSaved = utils.customTranslation(vm.settings.successfulSave, vm.settings.successfulSave) || $translate.instant('gateway.gateway-saved');
        } else {
            gatewayNameExists = $translate.instant('gateway.gateway-exists');
            successfulSaved = $translate.instant('gateway.gateway-saved');
        }
    }

    function initializeConfig() {
        updateWidgetDisplaying();
        initWidgetSettings();
        getGatewaysList(true);
    }


    vm.getAccessToken = (deviceId) => {
        if (deviceId.id) {
            getDeviceCredentials(deviceId.id);
        }
    };

    vm.collapsePanel = function (panelId) {
        $mdExpansionPanel(panelId).collapse();
    };

    function getDeviceCredentials(deviceId) {
        return deviceService.getDeviceCredentials(deviceId).then(
            (deviceCredentials) => {
                vm.configurations.accessToken = deviceCredentials.credentialsId;
                getAttributes();
            }
        );
    }

    vm.createDevice = (deviceObj) => {
        deviceService.findByName(deviceObj.name, {ignoreErrors: true})
            .then(
                function () {
                    toast.showError(gatewayNameExists, angular.element('.gateway-form'), 'top left');
                },
                function () {
                    if (vm.settings.gatewayType && vm.settings.gatewayType.length) {
                        deviceObj.type = vm.settings.gatewayType;
                    }
                    deviceService.saveDevice(deviceObj).then(
                        (device) => {
                            getDeviceCredentials(device.id.id).then(() => {
                                getGatewaysList();
                            });
                        }
                    );
                });
    };

    vm.saveAttributeConfig = () => {
        $q.all([
            saveAttribute(configurationAttribute, $window.btoa(angular.toJson(getGatewayConfigJSON())), types.attributesScope.shared.value),
            saveAttribute(configurationDraftsAttribute, $window.btoa(angular.toJson(getDraftConnectorJSON())), types.attributesScope.server.value),
            saveAttribute(remoteLoggingLevelAttribute, vm.configurations.remoteLoggingLevel.toUpperCase(), types.attributesScope.shared.value)
        ]).then(() => {
            toast.showSuccess(successfulSaved, 2000, angular.element('.gateway-form'), 'top left');
        })
    };

    function getAttributes() {
        let promises = [];
        promises.push(getAttribute(currentConfigurationAttribute, types.attributesScope.client.value));
        promises.push(getAttribute(configurationDraftsAttribute, types.attributesScope.server.value));
        promises.push(getAttribute(remoteLoggingLevelAttribute, types.attributesScope.shared.value));
        $q.all(promises).then((response) => {
            processCurrentConfiguration(response[0]);
            processConfigurationDrafts(response[1]);
            processLoggingLevel(response[2]);
        });
    }

    function getAttribute(attributeName, attributeScope) {
        return attributeService.getEntityAttributesValues(vm.configurations.gateway.id.entityType, vm.configurations.gateway.id.id, attributeScope, attributeName);
    }

    function saveAttribute(attributeName, attributeValue, attributeScope) {
        let attributes = [{
            key: attributeName,
            value: attributeValue
        }];
        return attributeService.saveEntityAttributes(vm.configurations.gateway.id.entityType, vm.configurations.gateway.id.id, attributeScope, attributes);
    }

    vm.exportConfig = () => {
        let filesZip = {};
        filesZip["tb_gateway.yaml"] = generateYAMLConfigurationFile();
        generateConfigConnectorFiles(filesZip);
        generateLogConfigFile(filesZip);
        importExport.exportJSZip(filesZip, archiveFileName);
        saveAttribute(remoteLoggingLevelAttribute, vm.configurations.remoteLoggingLevel.toUpperCase(), types.attributesScope.shared.value);
    };

    function generateYAMLConfigurationFile() {
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
        for (let i = 0; i < vm.configurations.connectors.length; i++) {
            if (vm.configurations.connectors[i].enabled) {
                config += '  -\n';
                config += '    name: ' + vm.configurations.connectors[i].name + '\n';
                config += '    type: ' + vm.configurations.connectors[i].configType + '\n';
                config += '    configuration: ' + generateFileName(vm.configurations.connectors[i].name) + '\n';
            }
        }
        return config;
    }

    function generateConfigConnectorFiles(fileZipAdd) {
        for (let i = 0; i < vm.configurations.connectors.length; i++) {
            if (vm.configurations.connectors[i].enabled) {
                fileZipAdd[generateFileName(vm.configurations.connectors[i].name)] = angular.toJson(vm.configurations.connectors[i].config);
            }
        }
    }

    function generateLogConfigFile(fileZipAdd) {
        fileZipAdd["logs.conf"] = getLogsConfig();
    }

    function getLogsConfig() {
        return templateLogsConfig
            .replace(/{ERROR}/g, vm.configurations.remoteLoggingLevel)
            .replace(/{.\/logs\/}/g, vm.configurations.remoteLoggingPathToLogs);
    }

    function getGatewayConfigJSON() {
        let gatewayConfig = {};
        gatewayConfig["thingsboard"] = gatewayMainConfigJSON();
        gatewayConnectorConfigJSON(gatewayConfig);
        return gatewayConfig;
    }

    function gatewayMainConfigJSON() {
        let configuration = {};

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
        configuration.thingsboard = thingsBoard;

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
        configuration.storage = storage;

        let connectors = [];
        for (let i = 0; i < vm.configurations.connectors.length; i++) {
            if (vm.configurations.connectors[i].enabled) {
                let connector = {
                    configuration: generateFileName(vm.configurations.connectors[i].name),
                    name: vm.configurations.connectors[i].name,
                    type: vm.configurations.connectors[i].configType
                };
                connectors.push(connector);
            }
        }
        configuration.connectors = connectors;

        configuration.logs = $window.btoa(getLogsConfig());

        return configuration;
    }

    function gatewayConnectorConfigJSON(gatewayConfiguration) {
        for (let i = 0; i < vm.configurations.connectors.length; i++) {
            if (vm.configurations.connectors[i].enabled) {
                let typeConnector = vm.configurations.connectors[i].configType;
                if (!angular.isArray(gatewayConfiguration[typeConnector])) {
                    gatewayConfiguration[typeConnector] = [];
                }

                let connectorConfig = {
                    name: vm.configurations.connectors[i].name,
                    config: vm.configurations.connectors[i].config
                };
                gatewayConfiguration[typeConnector].push(connectorConfig);
            }
        }
    }

    function getDraftConnectorJSON() {
        let draftConnector = {};
        for (let i = 0; i < vm.configurations.connectors.length; i++) {
            if (!vm.configurations.connectors[i].enabled) {
                let connector = {
                    connector: vm.configurations.connectors[i].configType,
                    config: vm.configurations.connectors[i].config
                };
                draftConnector[vm.configurations.connectors[i].name] = connector;
            }
        }
        return draftConnector;
    }

    function getGatewaysList(firstInit) {
        vm.gateways = [];
        entityService.getEntitiesByNameFilter(types.entityType.device, "", -1).then((devices) => {
            for (let i = 0; i < devices.length; i++) {
                const device = devices[i];
                if (device.additionalInfo !== null && device.additionalInfo.gateway === true) {
                    vm.gateways.push(device);
                    if (vm.deviceNameForm && firstInit && vm.gateways.length && device.name === vm.deviceNameForm) {
                        vm.configurations.gateway = device;
                        vm.getAccessToken(device.id);
                    } else if (firstInit && vm.gateways.length && device.name === vm.gateways[0].name) {
                        vm.configurations.gateway = device;
                        vm.getAccessToken(device.id);
                    }
                }
            }
        });
    }

    function processCurrentConfiguration(response) {
        if (response.length > 0) {
            vm.configurations.connectors = [];
            let attribute = angular.fromJson($window.atob(response[0].value));
            for (var attributeKey in attribute) {
                let keyValue = attribute[attributeKey];
                if (attributeKey === "thingsboard") {
                    if (keyValue !== null && Object.keys(keyValue).length > 0) {
                        setConfigGateway(keyValue);
                    }
                } else {
                    for (let connectorType in keyValue) {
                        let name = "No name";
                        if (Object.prototype.hasOwnProperty.call(keyValue[connectorType], 'name')) {
                            name = keyValue[connectorType].name;
                        }
                        let connector = {
                            enabled: true,
                            configType: attributeKey,
                            config: keyValue[connectorType].config,
                            name: name
                        };
                        vm.configurations.connectors.push(connector);
                    }
                }
            }
        }
    }

    function processConfigurationDrafts(response) {
        if (response.length > 0) {
            let attribute = angular.fromJson($window.atob(response[0].value));
            for (let key in attribute) {
                let connector = {
                    enabled: false,
                    configType: attribute[key].connector,
                    config: attribute[key].config,
                    name: key
                };
                vm.configurations.connectors.push(connector);
            }
        }
    }

    function processLoggingLevel(response) {
        if (response.length > 0) {
            if (vm.types.gatewayLogLevel[response[0].value.toLowerCase()]) {
                vm.configurations.remoteLoggingLevel = response[0].value.toUpperCase();
            }
        } else {
            vm.configurations.remoteLoggingLevel = vm.types.gatewayLogLevel.debug;
        }
    }

    function setConfigGateway(keyValue) {
        if (Object.prototype.hasOwnProperty.call(keyValue, 'thingsboard')) {
            vm.configurations.host = keyValue.thingsboard.host;
            vm.configurations.port = keyValue.thingsboard.port;
            vm.configurations.remoteConfiguration = keyValue.thingsboard.remoteConfiguration;
            if (Object.prototype.hasOwnProperty.call(keyValue.thingsboard.security, 'accessToken')) {
                vm.configurations.securityType = 'accessToken';
                vm.configurations.accessToken = keyValue.thingsboard.security.accessToken;
            } else {
                vm.configurations.securityType = 'tls';
                vm.configurations.caCertPath = keyValue.thingsboard.security.caCert;
                vm.configurations.privateKeyPath = keyValue.thingsboard.security.private_key;
                vm.configurations.certPath = keyValue.thingsboard.security.cert;
            }
        }

        if (Object.prototype.hasOwnProperty.call(keyValue, 'storage') && Object.prototype.hasOwnProperty.call(keyValue.storage, 'type')) {
            if (keyValue.storage.type === 'memory') {
                vm.configurations.storageType = 'memoryStorage';
                vm.configurations.readRecordsCount = keyValue.storage.read_records_count;
                vm.configurations.maxRecordsCount = keyValue.storage.max_records_count;
            } else if (keyValue.storage.type === 'file') {
                vm.configurations.storageType = 'fileStorage';
                vm.configurations.dataFolderPath = keyValue.storage.data_folder_path;
                vm.configurations.maxFilesCount = keyValue.storage.max_file_count;
                vm.configurations.readRecordsCount = keyValue.storage.read_records_count;
                vm.configurations.maxRecordsCount = keyValue.storage.max_records_count;
            }
        }
    }

    function generateFileName(fileName) {
        return fileName.replace("_", "")
            .replace("-", "")
            .replace(/^\s+|\s+/g, '')
            .toLowerCase() + '.json';
    }
}


