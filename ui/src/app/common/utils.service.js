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
import tinycolor from "tinycolor2";
import jsonSchemaDefaults from "json-schema-defaults";
import thingsboardTypes from "./types.constant";

export default angular.module('thingsboard.utils', [thingsboardTypes])
    .factory('utils', Utils)
    .name;

/*@ngInject*/
function Utils($mdColorPalette, types) {

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
        isDescriptorSchemaNotEmpty: isDescriptorSchemaNotEmpty,
        filterSearchTextEntities: filterSearchTextEntities
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

    function parseException(exception) {
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
                        data.lineNumber = lineInfoGroups[2] - 2;
                        if (lineInfoGroups.length >= 5) {
                            data.columnNumber = lineInfoGroups[4];
                        }
                    }
                }
            }
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

}
