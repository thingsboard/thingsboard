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
import tinycolor from "tinycolor2";
import jsonSchemaDefaults from "json-schema-defaults";
import thingsboardTypes from "./types.constant";

export default angular.module('thingsboard.utils', [thingsboardTypes])
    .factory('utils', Utils)
    .name;

/*@ngInject*/
function Utils($mdColorPalette, $rootScope, $window, $q, deviceService, types) {

    var predefinedFunctions = {},
        predefinedFunctionsList = [],
        materialColors = [];

    predefinedFunctions['Sin'] = "return Math.round(1000*Math.sin(time/5000));";
    predefinedFunctions['Cos'] = "return Math.round(1000*Math.cos(time/5000));";
    predefinedFunctions['Random'] =
        "var value = prevValue + Math.random() * 100 - 50;\n" +
        "var multiplier = Math.pow(10, 2 || 0);\n" +
        "var value = Math.round(value * multiplier) / multiplier;\n" +
        "if (value < -1000) {\n" +
        "	value = -1000;\n" +
        "} else if (value > 1000) {\n" +
        "	value = 1000;\n" +
        "}\n" +
        "return value;";

    for (var func in predefinedFunctions) {
        predefinedFunctionsList.push(func);
    }

    var colorPalettes = ['blue', 'green', 'red', 'amber', 'blue-grey', 'purple', 'light-green', 'indigo', 'pink', 'yellow', 'light-blue', 'orange', 'deep-purple', 'lime', 'teal', 'brown', 'cyan', 'deep-orange', 'grey'];
    var colorSpectrum = ['500', 'A700', '600', '700', '800', '900', '300', '400', 'A200', 'A400'];

    angular.forEach($mdColorPalette, function (value, key) {
        angular.forEach(value, function (color, label) {
            if (colorSpectrum.indexOf(label) > -1) {
                var rgb = 'rgb(' + color.value[0] + ',' + color.value[1] + ',' + color.value[2] + ')';
                color = tinycolor(rgb);
                var isDark = color.isDark();
                var colorItem = {
                    value: color.toHexString(),
                    group: key,
                    label: label,
                    isDark: isDark
                };
                materialColors.push(colorItem);
            }
        });
    });

    materialColors.sort(function (colorItem1, colorItem2) {
        var spectrumIndex1 = colorSpectrum.indexOf(colorItem1.label);
        var spectrumIndex2 = colorSpectrum.indexOf(colorItem2.label);
        var result = spectrumIndex1 - spectrumIndex2;
        if (result === 0) {
            var paletteIndex1 = colorPalettes.indexOf(colorItem1.group);
            var paletteIndex2 = colorPalettes.indexOf(colorItem2.group);
            result = paletteIndex1 - paletteIndex2;
        }
        return result;
    });

    var defaultDataKey = {
        name: 'f(x)',
        type: types.dataKeyType.function,
        label: 'Sin',
        color: getMaterialColor(0),
        funcBody: getPredefinedFunctionBody('Sin'),
        settings: {},
        _hash: Math.random()
    };

    var defaultDatasource = {
        type: types.datasourceType.function,
        name: types.datasourceType.function,
        dataKeys: [angular.copy(defaultDataKey)]
    };

    var service = {
        getDefaultDatasource: getDefaultDatasource,
        getDefaultDatasourceJson: getDefaultDatasourceJson,
        getMaterialColor: getMaterialColor,
        getPredefinedFunctionBody: getPredefinedFunctionBody,
        getPredefinedFunctionsList: getPredefinedFunctionsList,
        genMaterialColor: genMaterialColor,
        objectHashCode: objectHashCode,
        parseException: parseException,
        processWidgetException: processWidgetException,
        isDescriptorSchemaNotEmpty: isDescriptorSchemaNotEmpty,
        filterSearchTextEntities: filterSearchTextEntities,
        guid: guid,
        createDatasoucesFromSubscriptionsInfo: createDatasoucesFromSubscriptionsInfo,
        isLocalUrl: isLocalUrl
    }

    return service;

    function getPredefinedFunctionsList() {
        return predefinedFunctionsList;
    }

    function getPredefinedFunctionBody(func) {
        return predefinedFunctions[func];
    }

    function getMaterialColor(index) {
        var colorIndex = index % materialColors.length;
        return materialColors[colorIndex].value;
    }

    function genMaterialColor(str) {
        var hash = Math.abs(hashCode(str));
        return getMaterialColor(hash);
    }

    function hashCode(str) {
        var hash = 0;
        var i, char;
        if (str.length == 0) return hash;
        for (i = 0; i < str.length; i++) {
            char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32bit integer
        }
        return hash;
    }

    function objectHashCode(obj) {
        var hash = 0;
        if (obj) {
            var str = angular.toJson(obj);
            hash = hashCode(str);
        }
        return hash;
    }

    function parseException(exception, lineOffset) {
        var data = {};
        if (exception) {
            if (angular.isString(exception) || exception instanceof String) {
                data.message = exception;
            } else {
                if (exception.name) {
                    data.name = exception.name;
                } else {
                    data.name = 'UnknownError';
                }
                if (exception.message) {
                    data.message = exception.message;
                }
                if (exception.lineNumber) {
                    data.lineNumber = exception.lineNumber;
                    if (exception.columnNumber) {
                        data.columnNumber = exception.columnNumber;
                    }
                } else if (exception.stack) {
                    var lineInfoRegexp = /(.*<anonymous>):(\d*)(:)?(\d*)?/g;
                    var lineInfoGroups = lineInfoRegexp.exec(exception.stack);
                    if (lineInfoGroups != null && lineInfoGroups.length >= 3) {
                        if (angular.isUndefined(lineOffset)) {
                            lineOffset = -2;
                        }
                        data.lineNumber = Number(lineInfoGroups[2]) + lineOffset;
                        if (lineInfoGroups.length >= 5) {
                            data.columnNumber = lineInfoGroups[4];
                        }
                    }
                }
            }
        }
        return data;
    }

    function processWidgetException(exception) {
        var parentScope = $window.parent.angular.element($window.frameElement).scope();
        var data = parseException(exception, -5);
        if ($rootScope.widgetEditMode) {
            parentScope.$emit('widgetException', data);
            parentScope.$apply();
        }
        return data;
    }

    function getDefaultDatasource(dataKeySchema) {
        var datasource = angular.copy(defaultDatasource);
        if (angular.isDefined(dataKeySchema)) {
            datasource.dataKeys[0].settings = jsonSchemaDefaults(dataKeySchema);
        }
        return datasource;
    }

    function getDefaultDatasourceJson(dataKeySchema) {
        return angular.toJson(getDefaultDatasource(dataKeySchema));
    }

    function isDescriptorSchemaNotEmpty(descriptor) {
        if (descriptor && descriptor.schema && descriptor.schema.properties) {
            for(var prop in descriptor.schema.properties) {
                if (descriptor.schema.properties.hasOwnProperty(prop)) {
                    return true;
                }
            }
        }
        return false;
    }

    function filterSearchTextEntities(entities, searchTextField, pageLink, deferred) {
        var response = {
            data: [],
            hasNext: false,
            nextPageLink: null
        };
        var limit = pageLink.limit;
        var textSearch = '';
        if (pageLink.textSearch) {
            textSearch = pageLink.textSearch.toLowerCase();
        }

        for (var i=0;i<entities.length;i++) {
            var entity = entities[i];
            var text = entity[searchTextField].toLowerCase();
            var createdTime = entity.createdTime;
            if (pageLink.textOffset && pageLink.textOffset.length > 0) {
                var comparison = text.localeCompare(pageLink.textOffset);
                if (comparison === 0
                    && createdTime < pageLink.createdTimeOffset) {
                    response.data.push(entity);
                    if (response.data.length === limit) {
                        break;
                    }
                } else if (comparison > 0 && text.startsWith(textSearch)) {
                    response.data.push(entity);
                    if (response.data.length === limit) {
                        break;
                    }
                }
            } else if (textSearch.length > 0) {
                if (text.startsWith(textSearch)) {
                    response.data.push(entity);
                    if (response.data.length === limit) {
                        break;
                    }
                }
            } else {
                response.data.push(entity);
                if (response.data.length === limit) {
                    break;
                }
            }
        }
        if (response.data.length === limit) {
            var lastEntity = response.data[limit-1];
            response.nextPageLink = {
                limit: pageLink.limit,
                textSearch: textSearch,
                idOffset: lastEntity.id.id,
                createdTimeOffset: lastEntity.createdTime,
                textOffset: lastEntity[searchTextField].toLowerCase()
            };
            response.hasNext = true;
        }
        deferred.resolve(response);
    }

    function guid() {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    }

    function genNextColor(datasources) {
        var index = 0;
        if (datasources) {
            for (var i = 0; i < datasources.length; i++) {
                var datasource = datasources[i];
                index += datasource.dataKeys.length;
            }
        }
        return getMaterialColor(index);
    }

    /*var defaultDataKey = {
        name: 'f(x)',
        type: types.dataKeyType.function,
        label: 'Sin',
        color: getMaterialColor(0),
        funcBody: getPredefinedFunctionBody('Sin'),
        settings: {},
        _hash: Math.random()
    };

    var defaultDatasource = {
        type: types.datasourceType.function,
        name: types.datasourceType.function,
        dataKeys: [angular.copy(defaultDataKey)]
    };*/

    function createKey(keyInfo, type, datasources) {
        var dataKey = {
            name: keyInfo.name,
            type: type,
            label: keyInfo.label || keyInfo.name,
            color: genNextColor(datasources),
            funcBody: keyInfo.funcBody,
            settings: {},
            _hash: Math.random()
        }
        return dataKey;
    }

    function createDatasourceKeys(keyInfos, type, datasource, datasources) {
        for (var i=0;i<keyInfos.length;i++) {
            var keyInfo = keyInfos[i];
            var dataKey = createKey(keyInfo, type, datasources);
            datasource.dataKeys.push(dataKey);
        }
    }

    function createDatasourceFromSubscription(subscriptionInfo, datasources, device) {
        var datasource;
        if (subscriptionInfo.type === types.datasourceType.device) {
            datasource = {
                type: subscriptionInfo.type,
                deviceName: device.name,
                name: device.name,
                deviceId: device.id.id,
                dataKeys: []
            }
        } else if (subscriptionInfo.type === types.datasourceType.function) {
            datasource = {
                type: subscriptionInfo.type,
                name: subscriptionInfo.name || types.datasourceType.function,
                dataKeys: []
            }
        }
        datasources.push(datasource);
        if (subscriptionInfo.timeseries) {
            createDatasourceKeys(subscriptionInfo.timeseries, types.dataKeyType.timeseries, datasource, datasources);
        }
        if (subscriptionInfo.attributes) {
            createDatasourceKeys(subscriptionInfo.attributes, types.dataKeyType.attribute, datasource, datasources);
        }
        if (subscriptionInfo.functions) {
            createDatasourceKeys(subscriptionInfo.functions, types.dataKeyType.function, datasource, datasources);
        }
    }

    function processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred) {
        if (index < subscriptionsInfo.length) {
            var subscriptionInfo = subscriptionsInfo[index];
            if (subscriptionInfo.type === types.datasourceType.device) {
                if (subscriptionInfo.deviceId) {
                    deviceService.getDevice(subscriptionInfo.deviceId, true, {ignoreLoading: true}).then(
                        function success(device) {
                            createDatasourceFromSubscription(subscriptionInfo, datasources, device);
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        },
                        function fail() {
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        }
                    );
                } else if (subscriptionInfo.deviceName || subscriptionInfo.deviceNamePrefix
                    || subscriptionInfo.deviceIds) {
                    var promise;
                    if (subscriptionInfo.deviceName) {
                        promise = deviceService.fetchAliasDeviceByNameFilter(subscriptionInfo.deviceName, 1, false, {ignoreLoading: true});
                    } else if (subscriptionInfo.deviceNamePrefix) {
                        promise = deviceService.fetchAliasDeviceByNameFilter(subscriptionInfo.deviceNamePrefix, 100, false, {ignoreLoading: true});
                    } else if (subscriptionInfo.deviceIds) {
                        promise = deviceService.getDevices(subscriptionInfo.deviceIds, {ignoreLoading: true});
                    }
                    promise.then(
                        function success(devices) {
                            if (devices && devices.length > 0) {
                                for (var i = 0; i < devices.length; i++) {
                                    var device = devices[i];
                                    createDatasourceFromSubscription(subscriptionInfo, datasources, device);
                                }
                            }
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        },
                        function fail() {
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        }
                    )
                } else {
                    index++;
                    processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                }
            } else if (subscriptionInfo.type === types.datasourceType.function) {
                createDatasourceFromSubscription(subscriptionInfo, datasources);
                index++;
                processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
            }
        } else {
            deferred.resolve(datasources);
        }
    }

    function createDatasoucesFromSubscriptionsInfo(subscriptionsInfo) {
        var deferred = $q.defer();
        var datasources = [];
        processSubscriptionsInfo(0, subscriptionsInfo, datasources, deferred);
        return deferred.promise;
    }

    function isLocalUrl(url) {
        var parser = document.createElement('a'); //eslint-disable-line
        parser.href = url;
        var host = parser.hostname;
        if (host === "localhost" || host === "127.0.0.1") {
            return true;
        } else {
            return false;
        }
    }

}
