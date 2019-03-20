/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
import $ from 'jquery';
import moment from 'moment';
import tinycolor from 'tinycolor2';

import thingsboardLedLight from '../components/led-light.directive';
import thingsboardTimeseriesTableWidget from '../widget/lib/timeseries-table-widget';
import thingsboardAlarmsTableWidget from '../widget/lib/alarms-table-widget';
import thingsboardEntitiesTableWidget from '../widget/lib/entities-table-widget';
import thingsboardEntitiesHierarchyWidget from '../widget/lib/entities-hierarchy-widget';
import thingsboardExtensionsTableWidget from '../widget/lib/extensions-table-widget';

import thingsboardRpcWidgets from '../widget/lib/rpc';

import TbFlot from '../widget/lib/flot-widget';
import TbAnalogueLinearGauge from '../widget/lib/analogue-linear-gauge';
import TbAnalogueRadialGauge from '../widget/lib/analogue-radial-gauge';
import TbAnalogueCompass from '../widget/lib/analogue-compass';
import TbCanvasDigitalGauge from '../widget/lib/canvas-digital-gauge';
import TbMapWidget from '../widget/lib/map-widget';
import TbMapWidgetV2 from '../widget/lib/map-widget2';
import TripAnimationWidget from '../widget/lib/tripAnimation/trip-animation-widget';

import 'jquery.terminal/js/jquery.terminal.min.js';
import 'jquery.terminal/css/jquery.terminal.min.css';

import 'oclazyload';
import cssjs from '../../vendor/css.js/css';

import thingsboardTypes from '../common/types.constant';
import thingsboardUtils from '../common/utils.service';

export default angular.module('thingsboard.api.widget', ['oc.lazyLoad', thingsboardLedLight, thingsboardTimeseriesTableWidget,
    thingsboardAlarmsTableWidget, thingsboardEntitiesTableWidget, thingsboardEntitiesHierarchyWidget, thingsboardExtensionsTableWidget, thingsboardRpcWidgets, thingsboardTypes, thingsboardUtils, TripAnimationWidget])
    .factory('widgetService', WidgetService)
    .name;

/*@ngInject*/
function WidgetService($rootScope, $http, $q, $filter, $ocLazyLoad, $window, $translate, types, utils) {

    $window.$ = $;
    $window.jQuery = $;
    $window.moment = moment;
    $window.tinycolor = tinycolor;
    $window.lazyLoad = $ocLazyLoad;

    $window.TbFlot = TbFlot;
    $window.TbAnalogueLinearGauge = TbAnalogueLinearGauge;
    $window.TbAnalogueRadialGauge = TbAnalogueRadialGauge;
    $window.TbAnalogueCompass = TbAnalogueCompass;
    $window.TbCanvasDigitalGauge = TbCanvasDigitalGauge;
    $window.TbMapWidget = TbMapWidget;
    $window.TbMapWidgetV2 = TbMapWidgetV2;

    $window.cssjs = cssjs;

    var cssParser = new cssjs();
    cssParser.testMode = false;

    var missingWidgetType;
    var errorWidgetType;

    var editingWidgetType;

    var widgetsInfoInMemoryCache = {};
    var widgetsTypeFunctionsInMemoryCache = {};
    var widgetsInfoFetchQueue = {};

    var allWidgetsBundles = undefined;
    var systemWidgetsBundles = undefined;
    var tenantWidgetsBundles = undefined;

    $rootScope.widgetServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        invalidateWidgetsBundleCache();
    });

    initEditingWidgetType();
    initWidgetPlaceholders();

    var service = {
        getWidgetTemplate: getWidgetTemplate,
        getSystemWidgetsBundles: getSystemWidgetsBundles,
        getTenantWidgetsBundles: getTenantWidgetsBundles,
        getAllWidgetsBundles: getAllWidgetsBundles,
        getSystemWidgetsBundlesByPageLink: getSystemWidgetsBundlesByPageLink,
        getTenantWidgetsBundlesByPageLink: getTenantWidgetsBundlesByPageLink,
        getAllWidgetsBundlesByPageLink: getAllWidgetsBundlesByPageLink,
        getWidgetsBundleByAlias: getWidgetsBundleByAlias,
        saveWidgetsBundle: saveWidgetsBundle,
        getWidgetsBundle: getWidgetsBundle,
        deleteWidgetsBundle: deleteWidgetsBundle,
        getBundleWidgetTypes: getBundleWidgetTypes,
        getWidgetInfo: getWidgetInfo,
        getWidgetTypeFunction: getWidgetTypeFunction,
        getInstantWidgetInfo: getInstantWidgetInfo,
        deleteWidgetType: deleteWidgetType,
        saveWidgetType: saveWidgetType,
        saveImportedWidgetType: saveImportedWidgetType,
        getWidgetType: getWidgetType,
        getWidgetTypeById: getWidgetTypeById,
        toWidgetInfo: toWidgetInfo
    }

    return service;

    function initEditingWidgetType() {
        if ($rootScope.widgetEditMode) {
            editingWidgetType =
                toWidgetType({
                    widgetName: $rootScope.editWidgetInfo.widgetName,
                    alias: 'customWidget',
                    type: $rootScope.editWidgetInfo.type,
                    sizeX: $rootScope.editWidgetInfo.sizeX,
                    sizeY: $rootScope.editWidgetInfo.sizeY,
                    resources: $rootScope.editWidgetInfo.resources,
                    templateHtml: $rootScope.editWidgetInfo.templateHtml,
                    templateCss: $rootScope.editWidgetInfo.templateCss,
                    controllerScript: $rootScope.editWidgetInfo.controllerScript,
                    settingsSchema: $rootScope.editWidgetInfo.settingsSchema,
                    dataKeySettingsSchema: $rootScope.editWidgetInfo.dataKeySettingsSchema,
                    defaultConfig: $rootScope.editWidgetInfo.defaultConfig
            }, {id: '1'}, { id: types.id.nullUid }, 'customWidgetBundle');
        }
    }

    function initWidgetPlaceholders() {

        missingWidgetType = {
            widgetName: 'Widget type not found',
            alias: 'undefined',
            sizeX: 8,
            sizeY: 6,
            resources: [],
            templateHtml: '<div class="tb-widget-error-container"><div translate class="tb-widget-error-msg">widget.widget-type-not-found</div></div>',
            templateCss: '',
            controllerScript: 'self.onInit = function() {}',
            settingsSchema: '{}\n',
            dataKeySettingsSchema: '{}\n',
            defaultConfig: '{\n' +
            '"title": "Widget type not found",\n' +
            '"datasources": [],\n' +
            '"settings": {}\n' +
            '}\n'
        };

        errorWidgetType = {
            widgetName: 'Error loading widget',
            alias: 'error',
            sizeX: 8,
            sizeY: 6,
            resources: [],
            templateHtml: '<div class="tb-widget-error-container"><div translate class="tb-widget-error-msg">widget.widget-type-load-error</div>',
            templateCss: '',
            controllerScript: 'self.onInit = function() {}',
            settingsSchema: '{}\n',
            dataKeySettingsSchema: '{}\n',
            defaultConfig: '{\n' +
            '"title": "Widget failed to load",\n' +
            '"datasources": [],\n' +
            '"settings": {}\n' +
            '}\n'
        };
    }

    function toWidgetInfo(widgetType) {

        var widgetInfo = {
            widgetName: widgetType.name,
            alias: widgetType.alias
        }

        var descriptor = widgetType.descriptor;

        widgetInfo.type = descriptor.type;
        widgetInfo.sizeX = descriptor.sizeX;
        widgetInfo.sizeY = descriptor.sizeY;
        widgetInfo.resources = descriptor.resources;
        widgetInfo.templateHtml = descriptor.templateHtml;
        widgetInfo.templateCss = descriptor.templateCss;
        widgetInfo.controllerScript = descriptor.controllerScript;
        widgetInfo.settingsSchema = descriptor.settingsSchema;
        widgetInfo.dataKeySettingsSchema = descriptor.dataKeySettingsSchema;
        widgetInfo.defaultConfig = descriptor.defaultConfig;

        return widgetInfo;
    }

    function toWidgetType(widgetInfo, id, tenantId, bundleAlias) {
        var widgetType = {
            id: id,
            tenantId: tenantId,
            bundleAlias: bundleAlias,
            alias: widgetInfo.alias,
            name: widgetInfo.widgetName
        }

        var descriptor = {
            type: widgetInfo.type,
            sizeX: widgetInfo.sizeX,
            sizeY: widgetInfo.sizeY,
            resources: widgetInfo.resources,
            templateHtml: widgetInfo.templateHtml,
            templateCss: widgetInfo.templateCss,
            controllerScript: widgetInfo.controllerScript,
            settingsSchema: widgetInfo.settingsSchema,
            dataKeySettingsSchema: widgetInfo.dataKeySettingsSchema,
            defaultConfig: widgetInfo.defaultConfig
        }

        widgetType.descriptor = descriptor;

        return widgetType;
    }

    function getWidgetTemplate(type) {
        var deferred = $q.defer();
        var templateWidgetType = types.widgetType.timeseries;
        for (var t in types.widgetType) {
            var widgetType = types.widgetType[t];
            if (widgetType.value === type) {
                templateWidgetType = widgetType;
                break;
            }
        }
        getWidgetType(templateWidgetType.template.bundleAlias,
                      templateWidgetType.template.alias, true).then(
                          function success(widgetType) {
                              var widgetInfo = toWidgetInfo(widgetType);
                              widgetInfo.alias = undefined;
                              deferred.resolve(widgetInfo);
                          },
                          function fail() {
                              deferred.reject();
                          }
        );
        return deferred.promise;
    }

    /** Cache functions **/

    function createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem) {
        return (isSystem ? 'sys_' : '') + bundleAlias + '_' + widgetTypeAlias;
    }

    function getWidgetInfoFromCache(bundleAlias, widgetTypeAlias, isSystem) {
        var key = createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        return widgetsInfoInMemoryCache[key];
    }

    function getWidgetTypeFunctionFromCache(bundleAlias, widgetTypeAlias, isSystem) {
        var key = createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        return widgetsTypeFunctionsInMemoryCache[key];
    }

    function putWidgetInfoToCache(widgetInfo, bundleAlias, widgetTypeAlias, isSystem) {
        var key = createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        widgetsInfoInMemoryCache[key] = widgetInfo;
    }

    function putWidgetTypeFunctionToCache(widgetTypeFunction, bundleAlias, widgetTypeAlias, isSystem) {
        var key = createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        widgetsTypeFunctionsInMemoryCache[key] = widgetTypeFunction;
    }

    function deleteWidgetInfoFromCache(bundleAlias, widgetTypeAlias, isSystem) {
        var key = createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
        delete widgetsInfoInMemoryCache[key];
        delete widgetsTypeFunctionsInMemoryCache[key];
    }

    function deleteWidgetsBundleFromCache(bundleAlias, isSystem) {
        var key = (isSystem ? 'sys_' : '') + bundleAlias;
        for (var cacheKey in widgetsInfoInMemoryCache) {
            if (cacheKey.startsWith(key)) {
                delete widgetsInfoInMemoryCache[cacheKey];
                delete widgetsTypeFunctionsInMemoryCache[cacheKey];
            }
        }
    }

    /** Bundle functions **/

    function invalidateWidgetsBundleCache() {
        allWidgetsBundles = undefined;
        systemWidgetsBundles = undefined;
        tenantWidgetsBundles = undefined;
    }

    function loadWidgetsBundleCache(config) {
        var deferred = $q.defer();
        if (!allWidgetsBundles) {
            var url = '/api/widgetsBundles';
            $http.get(url, config).then(function success(response) {
                allWidgetsBundles = response.data;
                systemWidgetsBundles = [];
                tenantWidgetsBundles = [];
                allWidgetsBundles = $filter('orderBy')(allWidgetsBundles, ['+title', '-createdTime']);
                for (var i = 0; i < allWidgetsBundles.length; i++) {
                    var widgetsBundle = allWidgetsBundles[i];
                    if (widgetsBundle.tenantId.id === types.id.nullUid) {
                        systemWidgetsBundles.push(widgetsBundle);
                    } else {
                        tenantWidgetsBundles.push(widgetsBundle);
                    }
                }
                deferred.resolve();
            }, function fail() {
                deferred.reject();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }


    function getSystemWidgetsBundles(config) {
        var deferred = $q.defer();
        loadWidgetsBundleCache(config).then(
            function success() {
                deferred.resolve(systemWidgetsBundles);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getTenantWidgetsBundles(config) {
        var deferred = $q.defer();
        loadWidgetsBundleCache(config).then(
            function success() {
                deferred.resolve(tenantWidgetsBundles);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllWidgetsBundles(config) {
        var deferred = $q.defer();
        loadWidgetsBundleCache(config).then(
            function success() {
                deferred.resolve(allWidgetsBundles);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getSystemWidgetsBundlesByPageLink(pageLink) {
        var deferred = $q.defer();
        loadWidgetsBundleCache().then(
            function success() {
                utils.filterSearchTextEntities(systemWidgetsBundles, 'title', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getTenantWidgetsBundlesByPageLink(pageLink) {
        var deferred = $q.defer();
        loadWidgetsBundleCache().then(
            function success() {
                utils.filterSearchTextEntities(tenantWidgetsBundles, 'title', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllWidgetsBundlesByPageLink(pageLink) {
        var deferred = $q.defer();
        loadWidgetsBundleCache().then(
            function success() {
                utils.filterSearchTextEntities(allWidgetsBundles, 'title', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getWidgetsBundleByAlias(bundleAlias) {
        var deferred = $q.defer();
        loadWidgetsBundleCache().then(
            function success() {
                var widgetsBundles = $filter('filter')(allWidgetsBundles, {alias: bundleAlias});
                if (widgetsBundles.length > 0) {
                    deferred.resolve(widgetsBundles[0]);
                } else {
                    deferred.resolve();
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function saveWidgetsBundle(widgetsBundle) {
        var deferred = $q.defer();
        var url = '/api/widgetsBundle';
        $http.post(url, widgetsBundle).then(function success(response) {
            invalidateWidgetsBundleCache();
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getWidgetsBundle(widgetsBundleId) {
        var deferred = $q.defer();

        var url = '/api/widgetsBundle/' + widgetsBundleId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });

        return deferred.promise;
    }

    function deleteWidgetsBundle(widgetsBundleId) {
        var deferred = $q.defer();

        getWidgetsBundle(widgetsBundleId).then(
            function success(response) {
                var widgetsBundle = response;
                var url = '/api/widgetsBundle/' + widgetsBundleId;
                $http.delete(url).then(function success() {
                    invalidateWidgetsBundleCache();
                    deleteWidgetsBundleFromCache(widgetsBundle.alias,
                        widgetsBundle.tenantId.id === types.id.nullUid);
                    deferred.resolve();
                }, function fail(response) {
                    deferred.reject(response.data);
                });
            },
            function fail() {
                deferred.reject();
            }
        );

        return deferred.promise;
    }

    function getBundleWidgetTypes(bundleAlias, isSystem) {
        var deferred = $q.defer();
        var url = '/api/widgetTypes?isSystem=' + (isSystem ? 'true' : 'false') +
                    '&bundleAlias='+bundleAlias;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    /** Widget type functions **/

    function getInstantWidgetInfo(widget) {
        var widgetInfo = getWidgetInfoFromCache(widget.bundleAlias, widget.typeAlias, widget.isSystemType);
        if (widgetInfo) {
            return widgetInfo;
        } else {
            return {};
        }
    }

    function resolveWidgetsInfoFetchQueue(key, widgetInfo) {
        var fetchQueue = widgetsInfoFetchQueue[key];
        if (fetchQueue) {
            for (var q in fetchQueue) {
                if (isNaN(q))
                  continue;
                fetchQueue[q].resolve(widgetInfo);
            }
            delete widgetsInfoFetchQueue[key];
        }
    }

    function getWidgetInfo(bundleAlias, widgetTypeAlias, isSystem) {
        var deferred = $q.defer();
        var widgetInfo = getWidgetInfoFromCache(bundleAlias, widgetTypeAlias, isSystem);
        if (widgetInfo) {
            deferred.resolve(widgetInfo);
        } else {
            if ($rootScope.widgetEditMode) {
                loadWidget(editingWidgetType, bundleAlias, isSystem, deferred);
            } else {
                var key = createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
                var fetchQueue = widgetsInfoFetchQueue[key];
                if (fetchQueue) {
                    fetchQueue.push(deferred);
                } else {
                    fetchQueue = [];
                    widgetsInfoFetchQueue[key] = fetchQueue;
                    getWidgetType(bundleAlias, widgetTypeAlias, isSystem).then(
                        function success(widgetType) {
                            loadWidget(widgetType, bundleAlias, isSystem, deferred);
                        }, function fail() {
                            deferred.resolve(missingWidgetType);
                            resolveWidgetsInfoFetchQueue(key, missingWidgetType);
                        }
                    );
                }
            }
        }
        return deferred.promise;
    }

    function getWidgetTypeFunction(bundleAlias, widgetTypeAlias, isSystem) {
        var widgetType = getWidgetTypeFunctionFromCache(bundleAlias, widgetTypeAlias, isSystem);
        return widgetType;
    }

    function createWidgetTypeFunction(widgetInfo, name) {
        var widgetTypeFunctionBody = 'return function '+ name +' (ctx) {\n' +
            '    var self = this;\n' +
            '    self.ctx = ctx;\n\n'; /*+

         '    self.onInit = function() {\n\n' +

         '    }\n\n' +

         '    self.onDataUpdated = function() {\n\n' +

         '    }\n\n' +

         '    self.useCustomDatasources = function() {\n\n' +

         '    }\n\n' +

         '    self.typeParameters = function() {\n\n' +
                    return {
                                useCustomDatasources: false,
                                maxDatasources: -1, //unlimited
                                maxDataKeys: -1, //unlimited
                                dataKeysOptional: false,
                                stateData: false
                           };
         '    }\n\n' +

         '    self.actionSources = function() {\n\n' +
                    return {
                                'headerButton': {
                                   name: 'Header button',
                                   multiple: true
                                }
                            };
              }\n\n' +
         '    self.onResize = function() {\n\n' +

         '    }\n\n' +

         '    self.onEditModeChanged = function() {\n\n' +

         '    }\n\n' +

         '    self.onMobileModeChanged = function() {\n\n' +

         '    }\n\n' +

         '    self.getSettingsSchema = function() {\n\n' +

         '    }\n\n' +

         '    self.getDataKeySettingsSchema = function() {\n\n' +

         '    }\n\n' +

         '    self.onDestroy = function() {\n\n' +

         '    }\n\n' +
         '}';*/

        widgetTypeFunctionBody += widgetInfo.controllerScript;
        widgetTypeFunctionBody += '\n};\n';
        try {
            var widgetTypeFunction = new Function(widgetTypeFunctionBody);
            var widgetType = widgetTypeFunction.apply(this);
            var widgetTypeInstance = new widgetType();
            var result = {
                widgetTypeFunction: widgetType
            };
            if (angular.isFunction(widgetTypeInstance.getSettingsSchema)) {
                result.settingsSchema = widgetTypeInstance.getSettingsSchema();
            }
            if (angular.isFunction(widgetTypeInstance.getDataKeySettingsSchema)) {
                result.dataKeySettingsSchema = widgetTypeInstance.getDataKeySettingsSchema();
            }
            if (angular.isFunction(widgetTypeInstance.typeParameters)) {
                result.typeParameters = widgetTypeInstance.typeParameters();
            } else {
                result.typeParameters = {};
            }
            if (angular.isFunction(widgetTypeInstance.useCustomDatasources)) {
                result.typeParameters.useCustomDatasources = widgetTypeInstance.useCustomDatasources();
            } else {
                result.typeParameters.useCustomDatasources = false;
            }
            if (angular.isUndefined(result.typeParameters.maxDatasources)) {
                result.typeParameters.maxDatasources = -1;
            }
            if (angular.isUndefined(result.typeParameters.maxDataKeys)) {
                result.typeParameters.maxDataKeys = -1;
            }
            if (angular.isUndefined(result.typeParameters.dataKeysOptional)) {
                result.typeParameters.dataKeysOptional = false;
            }
            if (angular.isUndefined(result.typeParameters.stateData)) {
                result.typeParameters.stateData = false;
            }
            if (angular.isFunction(widgetTypeInstance.actionSources)) {
                result.actionSources = widgetTypeInstance.actionSources();
            } else {
                result.actionSources = {};
            }
            for (var actionSourceId in types.widgetActionSources) {
                result.actionSources[actionSourceId] = angular.copy(types.widgetActionSources[actionSourceId]);
                result.actionSources[actionSourceId].name = $translate.instant(result.actionSources[actionSourceId].name) + '';
            }

            return result;
        } catch (e) {
            utils.processWidgetException(e);
            throw e;
        }
    }

    function processWidgetLoadError(errorMessages, cacheKey, deferred) {
        var widgetInfo = angular.copy(errorWidgetType);
        for (var e in errorMessages) {
            var error = errorMessages[e];
            widgetInfo.templateHtml += '<div class="tb-widget-error-msg">' + error + '</div>';
        }
        widgetInfo.templateHtml += '</div>';
        deferred.resolve(widgetInfo);
        resolveWidgetsInfoFetchQueue(cacheKey, widgetInfo);
    }

    function loadWidget(widgetType, bundleAlias, isSystem, deferred) {
        var widgetInfo = toWidgetInfo(widgetType);
        var key = createWidgetInfoCacheKey(bundleAlias, widgetInfo.alias, isSystem);
        loadWidgetResources(widgetInfo, bundleAlias, isSystem).then(
            function success() {
                var widgetType = null;
                try {
                    widgetType = createWidgetTypeFunction(widgetInfo, key);
                } catch (e) {
                    var details = utils.parseException(e);
                    var errorMessage = 'Failed to compile widget script. \n Error: ' + details.message;
                    processWidgetLoadError([errorMessage], key, deferred);
                }
                if (widgetType) {
                    if (widgetType.settingsSchema) {
                        widgetInfo.typeSettingsSchema = widgetType.settingsSchema;
                    }
                    if (widgetType.dataKeySettingsSchema) {
                        widgetInfo.typeDataKeySettingsSchema = widgetType.dataKeySettingsSchema;
                    }
                    widgetInfo.typeParameters = widgetType.typeParameters;
                    widgetInfo.actionSources = widgetType.actionSources;
                    putWidgetInfoToCache(widgetInfo, bundleAlias, widgetInfo.alias, isSystem);
                    putWidgetTypeFunctionToCache(widgetType.widgetTypeFunction, bundleAlias, widgetInfo.alias, isSystem);
                    deferred.resolve(widgetInfo);
                    resolveWidgetsInfoFetchQueue(key, widgetInfo);
                }
            }, function fail(errorMessages) {
                processWidgetLoadError(errorMessages, key, deferred);
            }
        );
    }

    function getWidgetType(bundleAlias, widgetTypeAlias, isSystem) {
        var deferred = $q.defer();
        var url = '/api/widgetType?isSystem=' + (isSystem ? 'true' : 'false') +
            '&bundleAlias='+bundleAlias+'&alias='+widgetTypeAlias;
        $http.get(url, null).then(function success(response) {
            var widgetType = response.data;
            deferred.resolve(widgetType);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getWidgetTypeById(widgetTypeId) {
        var deferred = $q.defer();
        var url = '/api/widgetType/' + widgetTypeId;
        $http.get(url, null).then(function success(response) {
            var widgetType = response.data;
            deferred.resolve(widgetType);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteWidgetType(bundleAlias, widgetTypeAlias, isSystem) {
        var deferred = $q.defer();
        getWidgetType(bundleAlias, widgetTypeAlias, isSystem).then(
            function success(widgetType) {
                var url = '/api/widgetType/' + widgetType.id.id;
                $http.delete(url).then(function success() {
                    deleteWidgetInfoFromCache(bundleAlias, widgetTypeAlias, isSystem);
                    deferred.resolve();
                }, function fail() {
                    deferred.reject();
                });
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function saveWidgetType(widgetInfo, id, bundleAlias) {
        var deferred = $q.defer();
        var widgetType = toWidgetType(widgetInfo, id, undefined, bundleAlias);
        var url = '/api/widgetType';
        $http.post(url, widgetType).then(function success(response) {
            var widgetType = response.data;
            deleteWidgetInfoFromCache(widgetType.bundleAlias, widgetType.alias, widgetType.tenantId.id === types.id.nullUid);
            deferred.resolve(widgetType);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveImportedWidgetType(widgetType) {
        var deferred = $q.defer();
        var url = '/api/widgetType';
        $http.post(url, widgetType).then(function success(response) {
            var widgetType = response.data;
            deleteWidgetInfoFromCache(widgetType.bundleAlias, widgetType.alias, widgetType.tenantId.id === types.id.nullUid);
            deferred.resolve(widgetType);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function loadWidgetResources(widgetInfo, bundleAlias, isSystem) {

        var deferred = $q.defer();
        var errors = [];

        var widgetNamespace = "widget-type-" + (isSystem ? 'sys-' : '') + bundleAlias + '-' + widgetInfo.alias;
        cssParser.cssPreviewNamespace = widgetNamespace;
        cssParser.createStyleElement(widgetNamespace, widgetInfo.templateCss);

        function loadNextOrComplete(i) {
            i++;
            if (i < widgetInfo.resources.length) {
                loadNext(i);
            } else {
                if (errors.length > 0) {
                    deferred.reject(errors);
                } else {
                    deferred.resolve();
                }
            }
        }

        function loadNext(i) {
            var resourceUrl = widgetInfo.resources[i].url;
            if (resourceUrl && resourceUrl.length > 0) {
                $ocLazyLoad.load(resourceUrl).then(
                    function success() {
                        loadNextOrComplete(i);
                    },
                    function fail() {
                        errors.push('Failed to load widget resource: \'' + resourceUrl + '\'');
                        loadNextOrComplete(i);
                    }
                );
            } else {
                loadNextOrComplete(i);
            }
        }

        if (widgetInfo.resources.length > 0) {
            loadNext(0);
        } else {
            deferred.resolve();
        }

        return deferred.promise;
    }

}
