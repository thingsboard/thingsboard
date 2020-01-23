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
            disabled: '=ngDisabled',
            titleText: '@?',
            keyPlaceholderText: '@?',
            valuePlaceholderText: '@?',
            noDataText: '@?',
            gatewayFormConfig: '='
        },
        controller: GatewayFormController,
        controllerAs: 'vm',
        templateUrl: gatewayFormTemplate
    };
}

/*@ngInject*/
function GatewayFormController($scope, $injector, $document, $mdExpansionPanel, importExport, attributeService, $mdDialog, $mdUtil, types, $window, $q) {    // eslint-disable-line
    $scope.$mdExpansionPanel = $mdExpansionPanel;
    let vm = this;
    const attributeNameClinet = "current_configuration";
    const attributeNameServer = "configuration_drafts";
    const attributeNameShared = "configuration";
    const attributeNameLogShared = "RemoteLoggingLevel";
    vm.types = types;
    vm.configurations = $scope.configurations = {
        singleSelect: '',
        host: $document[0].domain,
        port: 1883,
        remoteConfiguration: true,
        accessToken: '',
        entityType: '',
        entityId: '',
        storageType: "memoryStorage",    //  "memoryStorage"; fileStorage
        readRecordsCount: 100,
        maxRecordsCount: 10000,
        dataFolderPath: './data/',
        maxFilesCount: 5,
        securityType: "accessToken",   // "accessToken", "tls"
        caCertPath: '/etc/thingsboard-gateway/ca.pem',
        privateKeyPath: '/etc/thingsboard-gateway/privateKey.pem',
        certPath: '/etc/thingsboard-gateway/certificate.pem',
        connectors: {},
        remoteLoggingLevel: "DEBUG" // level login
    };
    getGatewaysListByUser(true);

    vm.hashCode = (str) => {
        var hash = 0;
        var i, char;
        if (str.length === 0) return hash;
        for (i = 0; i < str.length; i++) {
            char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return hash;
    };


    vm.getAccessToken = (deviceObj) => {
        vm.deviceService.findByName(deviceObj.name, {ignoreErrors: true})
            .then(
                function (success) {
                    vm.deviceService.getDeviceCredentials(success.id.id).then(
                        (device) => {
                            vm.configurations.accessToken = device.credentialsId;
                            vm.configurations.entityType = device.deviceId.entityType;
                            vm.configurations.entityId = device.deviceId.id;
                            vm.getAttributeStart();
                        }
                    );
                }
            )
    };

    vm.createDevice = (deviceObj) => {
        vm.deviceService.findByName(deviceObj.name, {ignoreErrors: true})
            .then(
                function (success) {
                    vm.deviceService.getDeviceCredentials(success.id.id).then(
                        (device) => {
                            vm.configurations.accessToken = device.credentialsId;
                            vm.configurations.entityType = device.deviceId.entityType;
                            vm.configurations.entityId = device.deviceId.id;
                            vm.getAttributeStart();
                            getGatewaysListByUser();
                        }
                    );
                },
                function (fail) {// eslint-disable-line
                    vm.deviceService.saveDevice(deviceObj).then(
                        (device) => {
                            vm.deviceService.getDeviceCredentials(device.id.id).then(
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

    vm.remouteConfig = () => {
        if (vm.configurations.remoteConfiguration) {
            vm.saveAttributeConfig();
        } else {
            vm.exportConfig();
        }
        vm.setAttribute(attributeNameLogShared, vm.configurations.remoteLoggingLevel.toUpperCase(), types.attributesScope.shared.value);
    };
    vm.saveAttributeConfig = () => {
        vm.setAttribute(attributeNameShared, $window.btoa(angular.toJson(vm.getConfigAllByAttributeJSON())), types.attributesScope.shared.value);
        vm.setAttribute(attributeNameServer, $window.btoa(angular.toJson(vm.getConfigByAttributeTmpJSON())), types.attributesScope.server.value);
    };

    vm.getAttributeStart = () => {
        let initResps = [];
        vm.configurations.connectors = {};
        initResps.push(vm.getAttributeConfig(attributeNameClinet, types.attributesScope.client.value));
        initResps.push(vm.getAttributeConfig(attributeNameServer, types.attributesScope.server.value));
        initResps.push(vm.getAttributeConfig(attributeNameLogShared, types.attributesScope.shared.value));
        $q.all(initResps).then(resp => {
            vm.getAttributeInitFromClient(resp[0]);
            vm.getAttributeInitFromServer(resp[1]);
            vm.getAttributeInitFromShared(resp[2]);
        }, err => {
            console.log("getAttribute_error", err); //eslint-disable-line
        });
    };

    vm.getAttributeConfig = (attributeName, typeValue) => {
        let keys = [attributeName];
        return attributeService.getEntityAttributesValues(vm.configurations.entityType, vm.configurations.entityId, typeValue, keys);

    };

    vm.setAttribute = (attributeName, attributeConfig, typeValue) => {
        let attributes = [
            {
                key: attributeName,
                value: attributeConfig
            }
        ];
        attributeService.saveEntityAttributes(vm.configurations.entityType, vm.configurations.entityId, typeValue, attributes).then(resp => {    //eslint-disable-line
        }, err => {
            console.log("setAttribute_", err); //eslint-disable-line
        });
    };

    vm.exportConfig = () => {
        let fileZip = {};
        fileZip["tb_gateway.yaml"] = vm.getConfig();
        vm.createConfigByExport(fileZip);
        vm.downLoadConfig(fileZip, 'config');
    };

    vm.downLoadConfig = (data, filename) => {
        importExport.exportJSZip(data, filename);
    };

    vm.getConfig = () => {
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
                config += '    configuration: ' + vm.validFileName(connector) + ".json" + '\n';
            }
        }
        return config;
    };

    vm.createConfigByExport = (fileZipAdd) => {
        for (let connector in vm.configurations.connectors) {
            if (vm.configurations.connectors[connector].enabled) {
                fileZipAdd[vm.validFileName(connector) + ".json"] = angular.toJson(vm.configurations.connectors[connector].config);
            }
        }
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
                connect.configuration = vm.validFileName(connector) + ".json";
                connect.name = connector;
                connect.type = vm.configurations.connectors[connector].connector;
                conn.push(connect);
            }
        }
        configMain.connectors = conn;
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
            if (!vm.configurations.connectors[connector].enabled) {
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
        vm.deviceService = $injector.get("deviceService");
        vm.userService = $injector.get("userService");
        vm.currentUser = vm.userService.getCurrentUser();
        if (vm.currentUser.authority === 'TENANT_ADMIN') {
            vm.deviceService.getTenantDevices({limit: 500}).then(
                (success) => {
                    if (success.data.length > 0) {
                        for (let deviceNumber in success.data) {
                            if (success.data[deviceNumber].additionalInfo !== null) {
                                vm.gateways.push(success.data[deviceNumber].name);
                                if (firstInit && vm.gateways.length && success.data[deviceNumber].name === vm.gateways[0]) {
                                    vm.configurations.singleSelect = vm.gateways[0];
                                    let deviceObj = {
                                        "name": vm.configurations.singleSelect, "type": "Gateway", "additionalInfo": {
                                            "gateway": true
                                        }
                                    };
                                    vm.getAccessToken(deviceObj);
                                }
                            }
                        }
                    }
                }
            );
        } else if (vm.currentUser.authority === 'CUSTOMER_USER') {
            vm.deviceService.getCustomerDevices(vm.currentUser.customerId, {limit: 500}).then(
                (success) => {
                    if (success.data.length > 0) {
                        for (let deviceNumber in success.data) {
                            if (success.data[deviceNumber].additionalInfo !== null) {
                                vm.gateways.push(success.data[deviceNumber].name);
                                if (firstInit && vm.gateways.length) {
                                    vm.configurations.singleSelect = vm.gateways[0];
                                    let deviceObj = {
                                        "name": vm.configurations.singleSelect, "type": "Gateway", "additionalInfo": {
                                            "gateway": true
                                        }
                                    };
                                    vm.getAccessToken(deviceObj);
                                }
                            }
                        }
                    }
                }
            );
        }
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
        }
        else{
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
        if (Object.prototype.hasOwnProperty.call(keyVal, 'storage') && Object.prototype.hasOwnProperty.call(keyVal.storage, 'typee')) {
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

    vm.setSaveTypeConfig = (itemVal) => {
        vm.configurations.remoteConfiguration = itemVal.item;
    };

    vm.validFileName = (fileName) => {
        let fileName1 = fileName.replace("_", "");
        let fileName2 = fileName1.replace("-", "");
        let fileName3 = fileName2.replace(/^\s+|\s+$/g, '');
        let fileName4 = fileName3.toLowerCase();
        return fileName4;
    }
}


