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
import thingsboardTypes from '../../common/types.constant';
import thingsboardUtils from '../../common/utils.service';
import thingsboardEntityAliasSelect from '../entity-alias-select.directive';
import thingsboardDatasource from '../datasource.directive';
import thingsboardTimewindow from '../timewindow.directive';
import thingsboardLegendConfig from '../legend-config.directive';
import thingsboardJsonForm from '../json-form.directive';
import thingsboardManageWidgetActions from './action/manage-widget-actions.directive';
import 'angular-ui-ace';

import fixAceEditor from './../ace-editor-fix';

import './widget-config.scss';

/* eslint-disable import/no-unresolved, import/default */

import widgetConfigTemplate from './widget-config.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.widgetConfig', [thingsboardTypes,
    thingsboardUtils,
    thingsboardJsonForm,
    thingsboardEntityAliasSelect,
    thingsboardDatasource,
    thingsboardTimewindow,
    thingsboardLegendConfig,
    thingsboardManageWidgetActions,
    'ui.ace'])
    .directive('tbWidgetConfig', WidgetConfig)
    .name;

/*@ngInject*/
function WidgetConfig($compile, $templateCache, $rootScope, $translate, $timeout, types, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(widgetConfigTemplate);

        element.html(template);

        scope.types = types;
        scope.widgetEditMode = $rootScope.widgetEditMode;

        scope.emptySettingsSchema = {
            type: "object",
            properties: {}
        };
        
        scope.emptySettingsGroupInfoes=[];
        scope.defaultSettingsForm = [
            '*'
        ];

        scope.styleEditorOptions = {
            useWrapMode: true,
            mode: 'json',
            advanced: {
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            },
            onLoad: function (_ace) {
                fixAceEditor(_ace);
            }
        };

        if (angular.isUndefined(scope.forceExpandDatasources)) {
            scope.forceExpandDatasources = false;
        }

        if (angular.isUndefined(scope.isDataEnabled)) {
            scope.isDataEnabled = true;
        }

        scope.currentSettingsSchema = {};
        scope.currentSettings = angular.copy(scope.emptySettingsSchema);
        scope.currentSettingsGroupInfoes = angular.copy(scope.emptySettingsGroupInfoes);

        scope.targetDeviceAlias = {
            value: null
        }

        scope.alarmSource = {
            value: null
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var config = ngModelCtrl.$viewValue.config;
                var layout = ngModelCtrl.$viewValue.layout;
                if (config) {
                    scope.selectedTab = 0;
                    scope.title = config.title;
                    scope.showTitleIcon = angular.isDefined(config.showTitleIcon) ? config.showTitleIcon : false;
                    scope.titleIcon = angular.isDefined(config.titleIcon) ? config.titleIcon : '';
                    scope.iconColor = angular.isDefined(config.iconColor) ? config.iconColor : 'rgba(0, 0, 0, 0.87)';
                    scope.iconSize = angular.isDefined(config.iconSize) ? config.iconSize : '24px';
                    scope.showTitle = config.showTitle;
                    scope.dropShadow = angular.isDefined(config.dropShadow) ? config.dropShadow : true;
                    scope.enableFullscreen = angular.isDefined(config.enableFullscreen) ? config.enableFullscreen : true;
                    scope.backgroundColor = config.backgroundColor;
                    scope.color = config.color;
                    scope.padding = config.padding;
                    scope.margin = config.margin;
                    scope.widgetStyle =
                        angular.toJson(angular.isDefined(config.widgetStyle) ? config.widgetStyle : {}, true);
                    scope.titleStyle =
                        angular.toJson(angular.isDefined(config.titleStyle) ? config.titleStyle : {
                            fontSize: '16px',
                            fontWeight: 400
                        }, true);
                    scope.units = config.units;
                    scope.decimals = config.decimals;
                    scope.useDashboardTimewindow = angular.isDefined(config.useDashboardTimewindow) ?
                        config.useDashboardTimewindow : true;
                    scope.displayTimewindow = angular.isDefined(config.displayTimewindow) ?
                        config.displayTimewindow : true;
                    scope.timewindow = config.timewindow;
                    scope.showLegend = angular.isDefined(config.showLegend) ?
                        config.showLegend : scope.widgetType === types.widgetType.timeseries.value;
                    scope.legendConfig = config.legendConfig;
                    scope.actions = config.actions;
                    if (!scope.actions) {
                        scope.actions = {};
                    }
                    if (scope.widgetType !== types.widgetType.rpc.value &&
                        scope.widgetType !== types.widgetType.alarm.value &&
                        scope.widgetType !== types.widgetType.static.value
                        && scope.isDataEnabled) {
                        if (scope.datasources) {
                            scope.datasources.splice(0, scope.datasources.length);
                        } else {
                            scope.datasources = [];
                        }
                        if (config.datasources) {
                            for (var i = 0; i < config.datasources.length; i++) {
                                scope.datasources.push({value: config.datasources[i]});
                            }
                        }
                    } else if (scope.widgetType === types.widgetType.rpc.value && scope.isDataEnabled) {
                        if (config.targetDeviceAliasIds && config.targetDeviceAliasIds.length > 0) {
                            var aliasId = config.targetDeviceAliasIds[0];
                            var entityAliases = scope.aliasController.getEntityAliases();
                            if (entityAliases[aliasId]) {
                                scope.targetDeviceAlias.value = entityAliases[aliasId];
                            } else {
                                scope.targetDeviceAlias.value = null;
                            }
                        } else {
                            scope.targetDeviceAlias.value = null;
                        }
                    } else if (scope.widgetType === types.widgetType.alarm.value && scope.isDataEnabled) {
                        scope.alarmSearchStatus = angular.isDefined(config.alarmSearchStatus) ?
                            config.alarmSearchStatus : types.alarmSearchStatus.any;
                        scope.alarmsPollingInterval = angular.isDefined(config.alarmsPollingInterval) ?
                            config.alarmsPollingInterval : 5;
                        if (config.alarmSource) {
                            scope.alarmSource.value = config.alarmSource;
                        } else {
                            scope.alarmSource.value = null;
                        }
                    }

                    scope.settings = config.settings;

                    scope.updateSchemaForm();
                }
                if (layout) {
                    scope.mobileOrder = layout.mobileOrder;
                    scope.mobileHeight = layout.mobileHeight;
                }
            }
        };

        scope.displayAdvanced = function() {
            return scope.widgetSettingsSchema && scope.widgetSettingsSchema.schema;
        }

        scope.updateSchemaForm = function() {
            if (scope.widgetSettingsSchema && scope.widgetSettingsSchema.schema) {
                scope.currentSettingsSchema = scope.widgetSettingsSchema.schema;
                scope.currentSettingsForm = scope.widgetSettingsSchema.form || angular.copy(scope.defaultSettingsForm);
                scope.currentSettingsGroupInfoes = scope.widgetSettingsSchema.groupInfoes;
                scope.currentSettings = scope.settings;
            } else {
                scope.currentSettingsForm = angular.copy(scope.defaultSettingsForm);
                scope.currentSettingsSchema = angular.copy(scope.emptySettingsSchema);
                scope.currentSettingsGroupInfoes = angular.copy(scope.emptySettingsGroupInfoes);
                scope.currentSettings = {};
            }
        }

        scope.updateValidity = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var config = value.config;
                if (config) {
                    var valid;
                    if (scope.widgetType === types.widgetType.rpc.value && scope.isDataEnabled) {
                        valid = config && config.targetDeviceAliasIds && config.targetDeviceAliasIds.length > 0;
                        ngModelCtrl.$setValidity('targetDeviceAliasIds', valid);
                    } else if (scope.widgetType === types.widgetType.alarm.value && scope.isDataEnabled) {
                        valid = config && config.alarmSource;
                        ngModelCtrl.$setValidity('alarmSource', valid);
                    } else if (scope.widgetType !== types.widgetType.static.value && scope.isDataEnabled) {
                        valid = config && config.datasources && config.datasources.length > 0;
                        ngModelCtrl.$setValidity('datasources', valid);
                    }
                    try {
                        angular.fromJson(scope.widgetStyle);
                        ngModelCtrl.$setValidity('widgetStyle', true);
                    } catch (e) {
                        ngModelCtrl.$setValidity('widgetStyle', false);
                    }
                    try {
                        angular.fromJson(scope.titleStyle);
                        ngModelCtrl.$setValidity('titleStyle', true);
                    } catch (e) {
                        ngModelCtrl.$setValidity('titleStyle', false);
                    }
                }
            }
        };

        scope.$watch('title + showTitleIcon + titleIcon + iconColor + iconSize + showTitle + dropShadow + enableFullscreen + backgroundColor + ' +
            'color + padding + margin + widgetStyle + titleStyle + mobileOrder + mobileHeight + units + decimals + useDashboardTimewindow + ' +
            'displayTimewindow + alarmSearchStatus + alarmsPollingInterval + showLegend', function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                if (value.config) {
                    var config = value.config;
                    config.title = scope.title;
                    config.showTitleIcon = scope.showTitleIcon;
                    config.titleIcon = scope.titleIcon;
                    config.iconColor = scope.iconColor;
                    config.iconSize = scope.iconSize;
                    config.showTitle = scope.showTitle;
                    config.dropShadow = scope.dropShadow;
                    config.enableFullscreen = scope.enableFullscreen;
                    config.backgroundColor = scope.backgroundColor;
                    config.color = scope.color;
                    config.padding = scope.padding;
                    config.margin = scope.margin;
                    try {
                        config.widgetStyle = angular.fromJson(scope.widgetStyle);
                    } catch (e) {
                        config.widgetStyle = {};
                    }
                    try {
                        config.titleStyle = angular.fromJson(scope.titleStyle);
                    } catch (e) {
                        config.titleStyle = {};
                    }
                    config.units = scope.units;
                    config.decimals = scope.decimals;
                    config.useDashboardTimewindow = scope.useDashboardTimewindow;
                    config.displayTimewindow = scope.displayTimewindow;
                    config.alarmSearchStatus = scope.alarmSearchStatus;
                    config.alarmsPollingInterval = scope.alarmsPollingInterval;
                    config.showLegend = scope.showLegend;
                }
                if (value.layout) {
                    var layout = value.layout;
                    layout.mobileOrder = angular.isNumber(scope.mobileOrder) ? scope.mobileOrder : undefined;
                    layout.mobileHeight = scope.mobileHeight;
                }
                ngModelCtrl.$setViewValue(value);
                scope.updateValidity();
            }
        });

        scope.$watch('currentSettings', function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                if (value.config) {
                    value.config.settings = scope.currentSettings;
                    ngModelCtrl.$setViewValue(value);
                }
            }
        }, true);

        scope.$watch('timewindow', function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                if (value.config) {
                    value.config.timewindow = scope.timewindow;
                    ngModelCtrl.$setViewValue(value);
                }
            }
        }, true);

        scope.$watch('legendConfig', function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                if (value.config) {
                    value.config.legendConfig = scope.legendConfig;
                    ngModelCtrl.$setViewValue(value);
                }
            }
        }, true);

        scope.$watch('datasources', function () {
            if (ngModelCtrl.$viewValue && ngModelCtrl.$viewValue.config
                && scope.widgetType !== types.widgetType.rpc.value
                && scope.widgetType !== types.widgetType.alarm.value
                && scope.widgetType !== types.widgetType.static.value && scope.isDataEnabled) {
                var value = ngModelCtrl.$viewValue;
                var config = value.config;
                if (config.datasources) {
                    config.datasources.splice(0, config.datasources.length);
                } else {
                    config.datasources = [];
                }
                if (scope.datasources) {
                    for (var i = 0; i < scope.datasources.length; i++) {
                        config.datasources.push(scope.datasources[i].value);
                    }
                }
                ngModelCtrl.$setViewValue(value);
                scope.updateValidity();
            }
        }, true);

        scope.$watch('targetDeviceAlias.value', function () {
            if (ngModelCtrl.$viewValue && ngModelCtrl.$viewValue.config && scope.widgetType === types.widgetType.rpc.value && scope.isDataEnabled) {
                var value = ngModelCtrl.$viewValue;
                var config = value.config;
                if (scope.targetDeviceAlias.value) {
                    config.targetDeviceAliasIds = [scope.targetDeviceAlias.value.id];
                } else {
                    config.targetDeviceAliasIds = [];
                }
                ngModelCtrl.$setViewValue(value);
                scope.updateValidity();
            }
        });

        scope.$watch('alarmSource.value', function () {
            if (ngModelCtrl.$viewValue && ngModelCtrl.$viewValue.config && scope.widgetType === types.widgetType.alarm.value && scope.isDataEnabled) {
                var value = ngModelCtrl.$viewValue;
                var config = value.config;
                if (scope.alarmSource.value) {
                    config.alarmSource = scope.alarmSource.value;
                } else {
                    config.alarmSource = null;
                }
                ngModelCtrl.$setViewValue(value);
                scope.updateValidity();
            }
        });

        scope.$watch('actions', function () {
            if (ngModelCtrl.$viewValue && ngModelCtrl.$viewValue.config) {
                var value = ngModelCtrl.$viewValue;
                var config = value.config;
                config.actions = scope.actions;
                ngModelCtrl.$setViewValue(value);
                scope.updateValidity();
                /*if (scope.theForm) {
                    scope.theForm.$setDirty();
                }*/
            }
        }, true);

        scope.addDatasource = function () {
            var newDatasource;
            if (scope.functionsOnly) {
                newDatasource = angular.copy(utils.getDefaultDatasource(scope.datakeySettingsSchema.schema));
                newDatasource.dataKeys = [scope.generateDataKey('Sin', types.dataKeyType.function)];
            } else {
                newDatasource = { type: types.datasourceType.entity,
                    dataKeys: []
                };
            }
            var datasource = {value: newDatasource};
            scope.datasources.push(datasource);
            if (scope.theForm) {
                scope.theForm.$setDirty();
            }
        }

        scope.removeDatasource = function ($event, datasource) {
            var index = scope.datasources.indexOf(datasource);
            if (index > -1) {
                scope.datasources.splice(index, 1);
                if (scope.theForm) {
                    scope.theForm.$setDirty();
                }
            }
        };

        scope.generateDataKey = function (chip, type) {

            if (angular.isObject(chip)) {
                chip._hash = Math.random();
                return chip;
            }

            var label = chip;
            if (type === types.dataKeyType.alarm) {
                var alarmField = types.alarmFields[chip];
                if (alarmField) {
                    label = $translate.instant(alarmField.name)+'';
                }
            }
            label = scope.genNextLabel(label);

            var result = {
                name: chip,
                type: type,
                label: label,
                color: scope.genNextColor(),
                settings: {},
                _hash: Math.random()
            };

            if (type === types.dataKeyType.function) {
                result.name = 'f(x)';
                result.funcBody = utils.getPredefinedFunctionBody(chip);
                if (!result.funcBody) {
                    result.funcBody = "return prevValue + 1;";
                }
            }

            if (angular.isDefined(scope.datakeySettingsSchema.schema)) {
                result.settings = utils.generateObjectFromJsonSchema(scope.datakeySettingsSchema.schema);
            }

            return result;
        };

        scope.genNextLabel = function (name) {
            var label = name;
            var value = ngModelCtrl.$viewValue;
            var i = 1;
            var matches = false;
            do {
                matches = false;
                var datasources = scope.widgetType == types.widgetType.alarm.value ? [value.config.alarmSource] : value.config.datasources;
                if (datasources) {
                    for (var d=0;d<datasources.length;d++) {
                        var datasource = datasources[d];
                        if (datasource && datasource.dataKeys) {
                            for (var k = 0; k < datasource.dataKeys.length; k++) {
                                var dataKey = datasource.dataKeys[k];
                                if (dataKey.label === label) {
                                    i++;
                                    label = name + ' ' + i;
                                    matches = true;
                                }
                            }
                        }
                    }
                }
            } while (matches);
            return label;
        }

        scope.genNextColor = function () {
            var i = 0;
            var value = ngModelCtrl.$viewValue;
            var datasources = scope.widgetType == types.widgetType.alarm.value ? [value.config.alarmSource] : value.config.datasources;
            if (datasources) {
                for (var d=0;d<datasources.length;d++) {
                    var datasource = datasources[d];
                    if (datasource && datasource.dataKeys) {
                        i += datasource.dataKeys.length;
                    }
                }
            }
            return utils.getMaterialColor(i);
        }

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            forceExpandDatasources: '=?',
            isDataEnabled: '=?',
            widgetType: '=',
            typeParameters: '=',
            actionSources: '=',
            widgetSettingsSchema: '=',
            datakeySettingsSchema: '=',
            aliasController: '=',
            functionsOnly: '=',
            fetchEntityKeys: '&',
            fetchDashboardStates: '&',
            onCreateEntityAlias: '&',
            theForm: '='
        },
        link: linker
    };
}

/* eslint-enable angular/angularelement */
