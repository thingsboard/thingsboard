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
/* eslint-disable import/no-unresolved, import/default */

import materialIconsCodepoints from 'raw-loader!material-design-icons/iconfont/codepoints';

/* eslint-enable import/no-unresolved, import/default */

import tinycolor from 'tinycolor2';
import jsonSchemaDefaults from 'json-schema-defaults';
import base64js from 'base64-js';
import {utf8Encode, utf8Decode} from './utf8-support';

import thingsboardTypes from './types.constant';

export default angular.module('thingsboard.utils', [thingsboardTypes])
    .factory('utils', Utils)
    .name;

const varsRegex = /\$\{([^}]*)\}/g;

/*@ngInject*/
function Utils($mdColorPalette, $rootScope, $window, $translate, $q, $timeout, types) {

    var predefinedFunctions = {},
        predefinedFunctionsList = [],
        materialColors = [],
        materialIcons = [];

    var commonMaterialIcons = [ 'more_horiz', 'more_vert', 'open_in_new', 'visibility', 'play_arrow', 'arrow_back', 'arrow_downward',
        'arrow_forward', 'arrow_upwards', 'close', 'refresh', 'menu', 'show_chart', 'multiline_chart', 'pie_chart', 'insert_chart', 'people',
        'person', 'domain', 'devices_other', 'now_widgets', 'dashboards', 'map', 'pin_drop', 'my_location', 'extension', 'search',
        'settings', 'notifications', 'notifications_active', 'info', 'info_outline', 'warning', 'list', 'file_download', 'import_export',
        'share', 'add', 'edit', 'done' ];

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

    var defaultAlarmFields = [
        types.alarmFields.createdTime.keyName,
        types.alarmFields.originator.keyName,
        types.alarmFields.type.keyName,
        types.alarmFields.severity.keyName,
        types.alarmFields.status.keyName
    ];

    var defaultAlarmDataKeys = [];

    var imageAspectMap = {};

    var service = {
        getDefaultDatasource: getDefaultDatasource,
        generateObjectFromJsonSchema: generateObjectFromJsonSchema,
        getDefaultDatasourceJson: getDefaultDatasourceJson,
        getDefaultAlarmDataKeys: getDefaultAlarmDataKeys,
        getMaterialColor: getMaterialColor,
        getMaterialIcons: getMaterialIcons,
        getCommonMaterialIcons: getCommonMaterialIcons,
        getPredefinedFunctionBody: getPredefinedFunctionBody,
        getPredefinedFunctionsList: getPredefinedFunctionsList,
        genMaterialColor: genMaterialColor,
        objectHashCode: objectHashCode,
        parseException: parseException,
        processWidgetException: processWidgetException,
        isDescriptorSchemaNotEmpty: isDescriptorSchemaNotEmpty,
        filterSearchTextEntities: filterSearchTextEntities,
        guid: guid,
        cleanCopy: cleanCopy,
        isLocalUrl: isLocalUrl,
        validateDatasources: validateDatasources,
        createKey: createKey,
        createAdditionalDataKey: createAdditionalDataKey,
        createLabelFromDatasource: createLabelFromDatasource,
        insertVariable: insertVariable,
        customTranslation: customTranslation,
        objToBase64: objToBase64,
        base64toObj: base64toObj,
        loadImageAspect: loadImageAspect,
        sortObjectKeys: sortObjectKeys
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

    function getMaterialIcons() {
        var deferred = $q.defer();
        if (materialIcons.length) {
            deferred.resolve(materialIcons);
        } else {
            $timeout(function() {
                var codepointsArray = materialIconsCodepoints.split("\n");
                codepointsArray.forEach(function (codepoint) {
                    if (codepoint && codepoint.length) {
                        var values = codepoint.split(' ');
                        if (values && values.length == 2) {
                            materialIcons.push(values[0]);
                        }
                    }
                });
                deferred.resolve(materialIcons);
            });
        }
        return deferred.promise;
    }

    function getCommonMaterialIcons() {
        return commonMaterialIcons;
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
            datasource.dataKeys[0].settings = generateObjectFromJsonSchema(dataKeySchema);
        }
        return datasource;
    }

    function generateObjectFromJsonSchema(schema) {
        var obj = jsonSchemaDefaults(schema);
        deleteNullProperties(obj);
        return obj;
    }

    function deleteNullProperties(obj) {
        if (angular.isUndefined(obj) || obj == null) {
            return;
        }
        for (var propName in obj) {
            if (obj[propName] === null || angular.isUndefined(obj[propName])) {
                delete obj[propName];
            } else if (angular.isObject(obj[propName])) {
                deleteNullProperties(obj[propName]);
            } else if (angular.isArray(obj[propName])) {
                for (var i=0;i<obj[propName].length;i++) {
                    deleteNullProperties(obj[propName][i]);
                }
            }
        }
    }

    function getDefaultDatasourceJson(dataKeySchema) {
        return angular.toJson(getDefaultDatasource(dataKeySchema));
    }

    function initDefaultAlarmDataKeys() {
        for (var i=0;i<defaultAlarmFields.length;i++) {
            var name = defaultAlarmFields[i];
            var dataKey = {
                name: name,
                type: types.dataKeyType.alarm,
                label: $translate.instant(types.alarmFields[name].name)+'',
                color: getMaterialColor(i),
                settings: {},
                _hash: Math.random()
            };
            defaultAlarmDataKeys.push(dataKey);
        }
    }

    function getDefaultAlarmDataKeys() {
        if (!defaultAlarmDataKeys.length) {
            initDefaultAlarmDataKeys();
        }
        return angular.copy(defaultAlarmDataKeys);
    }

    function isDescriptorSchemaNotEmpty(descriptor) {
        if (descriptor && descriptor.schema && descriptor.schema.properties) {
            for(var prop in descriptor.schema.properties) {
                if (Object.prototype.hasOwnProperty.call(descriptor.schema.properties, prop)) {
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

    function cleanCopy(object) {
        var copy = angular.copy(object);
        for (var prop in copy) {
            if (prop && prop.startsWith('$$')) {
                delete copy[prop];
            }
        }
        return copy;
    }

    function genNextColor(datasources, initialIndex) {
        var index = initialIndex || 0;
        if (datasources) {
            for (var i = 0; i < datasources.length; i++) {
                var datasource = datasources[i];
                index += datasource.dataKeys.length;
            }
        }
        return getMaterialColor(index);
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

    function validateDatasources(datasources) {
        datasources.forEach(function (datasource) {
            if (datasource.type === 'device') {
                datasource.type = types.datasourceType.entity;
                datasource.entityType = types.entityType.device;
                if (datasource.deviceId) {
                    datasource.entityId = datasource.deviceId;
                } else if (datasource.deviceAliasId) {
                    datasource.entityAliasId = datasource.deviceAliasId;
                }
                if (datasource.deviceName) {
                    datasource.entityName = datasource.deviceName;
                }
            }
            if (datasource.type === types.datasourceType.entity && datasource.entityId) {
                datasource.name = datasource.entityName;
            }
        });
        return datasources;
    }

    function createKey(keyInfo, type, datasources) {
        var label;
        if (type === types.dataKeyType.alarm && !keyInfo.label) {
            var alarmField = types.alarmFields[keyInfo.name];
            if (alarmField) {
                label = $translate.instant(alarmField.name)+'';
            }
        }
        if (!label) {
            label = keyInfo.label || keyInfo.name;
        }
        var dataKey = {
            name: keyInfo.name,
            type: type,
            label: label,
            funcBody: keyInfo.funcBody,
            settings: {},
            _hash: Math.random()
        }
        if (keyInfo.units) {
            dataKey.units = keyInfo.units;
        }
        if (angular.isDefined(keyInfo.decimals)) {
            dataKey.decimals = keyInfo.decimals;
        }
        if (keyInfo.color) {
            dataKey.color = keyInfo.color;
        } else {
            dataKey.color = genNextColor(datasources);
        }
        if (keyInfo.postFuncBody && keyInfo.postFuncBody.length) {
            dataKey.usePostProcessing = true;
            dataKey.postFuncBody = keyInfo.postFuncBody;
        }
        return dataKey;
    }

    function createAdditionalDataKey(dataKey, datasource, timeUnit, datasources, additionalKeysNumber) {
        let additionalDataKey = angular.copy(dataKey);
        if (dataKey.settings.comparisonSettings.comparisonValuesLabel) {
            additionalDataKey.label = createLabelFromDatasource(datasource, dataKey.settings.comparisonSettings.comparisonValuesLabel);
        } else {
            additionalDataKey.label = dataKey.label + ' ' + $translate.instant('legend.comparison-time-ago.'+timeUnit);
        }
        additionalDataKey.pattern = additionalDataKey.label;
        if (dataKey.settings.comparisonSettings.color) {
            additionalDataKey.color = dataKey.settings.comparisonSettings.color;
        } else {
            additionalDataKey.color = genNextColor(datasources, additionalKeysNumber);
        }
        additionalDataKey._hash = Math.random();
        return additionalDataKey;
    }

    function createLabelFromDatasource(datasource, pattern) {
        var label = angular.copy(pattern);
        var match = varsRegex.exec(pattern);
        while (match !== null) {
            var variable = match[0];
            var variableName = match[1];
            if (variableName === 'dsName') {
                label = label.split(variable).join(datasource.name);
            } else if (variableName === 'entityName') {
                label = label.split(variable).join(datasource.entityName);
            } else if (variableName === 'deviceName') {
                label = label.split(variable).join(datasource.entityName);
            } else if (variableName === 'entityLabel') {
                label = label.split(variable).join(datasource.entityLabel || datasource.entityName);
            } else if (variableName === 'aliasName') {
                label = label.split(variable).join(datasource.aliasName);
            } else if (variableName === 'entityDescription') {
                label = label.split(variable).join(datasource.entityDescription);
            }
            match = varsRegex.exec(pattern);
        }
        return label;
    }

    function insertVariable(pattern, name, value) {
        var result = angular.copy(pattern);
        var match = varsRegex.exec(pattern);
        while (match !== null) {
            var variable = match[0];
            var variableName = match[1];
            if (variableName === name) {
                result = result.split(variable).join(value);
            }
            match = varsRegex.exec(pattern);
        }
        return result;
    }

    function customTranslation(translationValue, defaultValue) {
        var result = '';
        var translationId = types.translate.customTranslationsPrefix + translationValue;
        var translation = $translate.instant(translationId);
        if (translation != translationId) {
            result = translation + '';
        } else {
            result = defaultValue;
        }
        return result;
    }

    function objToBase64(obj) {
        var json = angular.toJson(obj);
        var encoded = utf8Encode(json);
        var b64Encoded = base64js.fromByteArray(encoded);
        return b64Encoded;
    }

    function base64toObj(b64Encoded) {
        var encoded = base64js.toByteArray(b64Encoded);
        var json = utf8Decode(encoded);
        var obj = angular.fromJson(json);
        return obj;
    }

    function loadImageAspect(imageUrl) {
        var deferred = $q.defer();
        if (imageUrl && imageUrl.length) {
            var urlHashCode = hashCode(imageUrl);
            var aspect = imageAspectMap[urlHashCode];
            if (angular.isUndefined(aspect)) {
                var testImage = document.createElement('img'); // eslint-disable-line
                testImage.style.position = 'absolute';
                testImage.style.left = '-99999px';
                testImage.style.top = '-99999px';
                testImage.onload = function() {
                    aspect = testImage.width / testImage.height;
                    document.body.removeChild(testImage); //eslint-disable-line
                    imageAspectMap[urlHashCode] = aspect;
                    deferred.resolve(aspect);
                };
                testImage.onerror = function() {
                    aspect = 0;
                    imageAspectMap[urlHashCode] = aspect;
                    deferred.resolve(aspect);
                };
                document.body.appendChild(testImage); //eslint-disable-line
                testImage.src = imageUrl;
            } else {
                deferred.resolve(aspect);
            }
        } else {
            deferred.resolve(0);
        }
        return deferred.promise;
    }

    function sortObjectKeys(obj) {
        var sortedObj = {};
        var keys = Object.keys(obj).sort();
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            sortedObj[key] = obj[key];
        }
        return sortedObj;
    }

}
