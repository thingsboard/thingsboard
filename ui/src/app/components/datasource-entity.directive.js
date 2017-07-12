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
import './datasource-entity.scss';

import 'md-color-picker';
import tinycolor from 'tinycolor2';
import $ from 'jquery';
import thingsboardTypes from '../common/types.constant';
import thingsboardDatakeyConfigDialog from './datakey-config-dialog.controller';
import thingsboardTruncate from './truncate.filter';

/* eslint-disable import/no-unresolved, import/default */

import datasourceEntityTemplate from './datasource-entity.tpl.html';
import datakeyConfigDialogTemplate from './datakey-config-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.datasourceEntity', [thingsboardTruncate, thingsboardTypes, thingsboardDatakeyConfigDialog])
    .directive('tbDatasourceEntity', DatasourceEntity)
    .name;

/*@ngInject*/
function DatasourceEntity($compile, $templateCache, $q, $mdDialog, $window, $document, $mdColorPicker, $mdConstant, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(datasourceEntityTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;

        scope.alarmFields = [];
        for (var alarmField in types.alarmFields) {
            scope.alarmFields.push(alarmField);
        }

        scope.selectedTimeseriesDataKey = null;
        scope.timeseriesDataKeySearchText = null;

        scope.selectedAttributeDataKey = null;
        scope.attributeDataKeySearchText = null;

        scope.selectedAlarmDataKey = null;
        scope.alarmDataKeySearchText = null;

        scope.updateValidity = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var dataValid = angular.isDefined(value) && value != null;
                ngModelCtrl.$setValidity('entityData', dataValid);
                if (dataValid) {
                    ngModelCtrl.$setValidity('entityAlias',
                        angular.isDefined(value.entityAliasId) &&
                        value.entityAliasId != null);
                    if (scope.optDataKeys) {
                        ngModelCtrl.$setValidity('entityKeys', true);
                    } else {
                        ngModelCtrl.$setValidity('entityKeys',
                            angular.isDefined(value.dataKeys) &&
                            value.dataKeys != null &&
                            value.dataKeys.length > 0);
                    }
                }
            }
        };

        scope.$watch('entityAlias', function () {
            if (ngModelCtrl.$viewValue) {
                if (scope.entityAlias) {
                    ngModelCtrl.$viewValue.entityAliasId = scope.entityAlias.id;
                } else {
                    ngModelCtrl.$viewValue.entityAliasId = null;
                }
                scope.updateValidity();
                scope.selectedEntityAliasChange();
            }
        });

        scope.$watch('timeseriesDataKeys', function () {
            updateDataKeys();
        }, true);

        scope.$watch('attributeDataKeys', function () {
            updateDataKeys();
        }, true);

        scope.$watch('alarmDataKeys', function () {
            updateDataKeys();
        }, true);

        function updateDataKeys() {
            if (ngModelCtrl.$viewValue) {
                var dataKeys = [];
                dataKeys = dataKeys.concat(scope.timeseriesDataKeys);
                dataKeys = dataKeys.concat(scope.attributeDataKeys);
                dataKeys = dataKeys.concat(scope.alarmDataKeys);
                ngModelCtrl.$viewValue.dataKeys = dataKeys;
                scope.updateValidity();
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var entityAliasId = ngModelCtrl.$viewValue.entityAliasId;
                var entityAliases = scope.aliasController.getEntityAliases();
                if (entityAliases[entityAliasId]) {
                    scope.entityAlias = entityAliases[entityAliasId];
                } else {
                    scope.entityAlias = null;
                }
                var timeseriesDataKeys = [];
                var attributeDataKeys = [];
                var alarmDataKeys = [];
                for (var d in ngModelCtrl.$viewValue.dataKeys) {
                    var dataKey = ngModelCtrl.$viewValue.dataKeys[d];
                    if (dataKey.type === types.dataKeyType.timeseries) {
                        timeseriesDataKeys.push(dataKey);
                    } else if (dataKey.type === types.dataKeyType.attribute) {
                        attributeDataKeys.push(dataKey);
                    } else if (dataKey.type === types.dataKeyType.alarm) {
                        alarmDataKeys.push(dataKey);
                    }
                }
                scope.timeseriesDataKeys = timeseriesDataKeys;
                scope.attributeDataKeys = attributeDataKeys;
                scope.alarmDataKeys = alarmDataKeys;
            }
        };

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.selectedEntityAliasChange = function () {
            if (!scope.timeseriesDataKeySearchText || scope.timeseriesDataKeySearchText === '') {
                scope.timeseriesDataKeySearchText = scope.timeseriesDataKeySearchText === '' ? null : '';
            }
            if (!scope.attributeDataKeySearchText || scope.attributeDataKeySearchText === '') {
                scope.attributeDataKeySearchText = scope.attributeDataKeySearchText === '' ? null : '';
            }
            if (!scope.alarmDataKeySearchText || scope.alarmDataKeySearchText === '') {
                scope.alarmDataKeySearchText = scope.alarmDataKeySearchText === '' ? null : '';
            }
        };

        scope.transformTimeseriesDataKeyChip = function (chip) {
            if (scope.maxDataKeys > 0 && ngModelCtrl.$viewValue.dataKeys.length >= scope.maxDataKeys ) {
                return null;
            } else {
                return scope.generateDataKey({chip: chip, type: types.dataKeyType.timeseries});
            }
        };

        scope.transformAttributeDataKeyChip = function (chip) {
            if (scope.maxDataKeys > 0 && ngModelCtrl.$viewValue.dataKeys.length >= scope.maxDataKeys ) {
                return null;
            } else {
                return scope.generateDataKey({chip: chip, type: types.dataKeyType.attribute});
            }
        };

        scope.transformAlarmDataKeyChip = function (chip) {
            return scope.generateDataKey({chip: chip, type: types.dataKeyType.alarm});
        };

        scope.showColorPicker = function (event, dataKey) {
            $mdColorPicker.show({
                value: dataKey.color,
                defaultValue: '#fff',
                random: tinycolor.random(),
                clickOutsideToClose: false,
                hasBackdrop: false,
                skipHide: true,
                preserveScope: false,

                mdColorAlphaChannel: true,
                mdColorSpectrum: true,
                mdColorSliders: true,
                mdColorGenericPalette: false,
                mdColorMaterialPalette: true,
                mdColorHistory: false,
                mdColorDefaultTab: 2,

                $event: event

            }).then(function (color) {
                dataKey.color = color;
                ngModelCtrl.$setDirty();
            });
        }

        scope.editDataKey = function (event, dataKey, index) {

            $mdDialog.show({
                controller: 'DatakeyConfigDialogController',
                controllerAs: 'vm',
                templateUrl: datakeyConfigDialogTemplate,
                locals: {
                    dataKey: angular.copy(dataKey),
                    dataKeySettingsSchema: scope.datakeySettingsSchema,
                    entityAlias: scope.entityAlias,
                    aliasController: scope.aliasController
                },
                parent: angular.element($document[0].body),
                fullscreen: true,
                targetEvent: event,
                skipHide: true,
                onComplete: function () {
                    var w = angular.element($window);
                    w.triggerHandler('resize');
                }
            }).then(function (dataKey) {
                if (dataKey.type === types.dataKeyType.timeseries) {
                    scope.timeseriesDataKeys[index] = dataKey;
                } else if (dataKey.type === types.dataKeyType.attribute) {
                    scope.attributeDataKeys[index] = dataKey;
                } else if (dataKey.type === types.dataKeyType.alarm) {
                    scope.alarmDataKeys[index] = dataKey;
                }
                ngModelCtrl.$setDirty();
            }, function () {
            });
        };

        scope.dataKeysSearch = function (searchText, type) {
            if (scope.widgetType == types.widgetType.alarm.value) {
                var dataKeys = searchText ? scope.alarmFields.filter(
                    scope.createFilterForDataKey(searchText)) : scope.alarmFields;
                return dataKeys;
            } else {
                if (scope.entityAlias) {
                    var deferred = $q.defer();
                    scope.fetchEntityKeys({entityAliasId: scope.entityAlias.id, query: searchText, type: type})
                        .then(function (dataKeys) {
                            deferred.resolve(dataKeys);
                        }, function (e) {
                            deferred.reject(e);
                        });
                    return deferred.promise;
                } else {
                    return $q.when([]);
                }
            }
        };

        scope.createFilterForDataKey = function (query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(dataKey) {
                return (angular.lowercase(dataKey).indexOf(lowercaseQuery) === 0);
            };
        };

        scope.createKey = function (event, chipsId) {
            var chipsChild = $(chipsId, element)[0].firstElementChild;
            var el = angular.element(chipsChild);
            var chipBuffer = el.scope().$mdChipsCtrl.getChipBuffer();
            event.preventDefault();
            event.stopPropagation();
            el.scope().$mdChipsCtrl.appendChip(chipBuffer.trim());
            el.scope().$mdChipsCtrl.resetChipBuffer();
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            widgetType: '=',
            maxDataKeys: '=',
            optDataKeys: '=',
            aliasController: '=',
            datakeySettingsSchema: '=',
            generateDataKey: '&',
            fetchEntityKeys: '&',
            onCreateEntityAlias: '&'
        },
        link: linker
    };
}

/* eslint-enable angular/angularelement */
