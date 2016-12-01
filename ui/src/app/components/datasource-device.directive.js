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
import './datasource-device.scss';

import 'md-color-picker';
import tinycolor from 'tinycolor2';
import $ from 'jquery';
import thingsboardTypes from '../common/types.constant';
import thingsboardDatakeyConfigDialog from './datakey-config-dialog.controller';
import thingsboardTruncate from './truncate.filter';

/* eslint-disable import/no-unresolved, import/default */

import datasourceDeviceTemplate from './datasource-device.tpl.html';
import datakeyConfigDialogTemplate from './datakey-config-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.datasourceDevice', [thingsboardTruncate, thingsboardTypes, thingsboardDatakeyConfigDialog])
    .directive('tbDatasourceDevice', DatasourceDevice)
    .name;

/*@ngInject*/
function DatasourceDevice($compile, $templateCache, $q, $mdDialog, $window, $document, $mdColorPicker, $mdConstant, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(datasourceDeviceTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;

        scope.selectedTimeseriesDataKey = null;
        scope.timeseriesDataKeySearchText = null;

        scope.selectedAttributeDataKey = null;
        scope.attributeDataKeySearchText = null;

        scope.updateValidity = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var dataValid = angular.isDefined(value) && value != null;
                ngModelCtrl.$setValidity('deviceData', dataValid);
                if (dataValid) {
                    ngModelCtrl.$setValidity('deviceAlias',
                        angular.isDefined(value.deviceAliasId) &&
                        value.deviceAliasId != null);
                    ngModelCtrl.$setValidity('deviceKeys',
                        angular.isDefined(value.dataKeys) &&
                        value.dataKeys != null &&
                        value.dataKeys.length > 0);
                }
            }
        };

        scope.$watch('deviceAlias', function () {
            if (ngModelCtrl.$viewValue) {
                if (scope.deviceAlias) {
                    ngModelCtrl.$viewValue.deviceAliasId = scope.deviceAlias.id;
                } else {
                    ngModelCtrl.$viewValue.deviceAliasId = null;
                }
                scope.updateValidity();
                scope.selectedDeviceAliasChange();
            }
        });

        scope.$watch('timeseriesDataKeys', function () {
            if (ngModelCtrl.$viewValue) {
                var dataKeys = [];
                dataKeys = dataKeys.concat(scope.timeseriesDataKeys);
                dataKeys = dataKeys.concat(scope.attributeDataKeys);
                ngModelCtrl.$viewValue.dataKeys = dataKeys;
                scope.updateValidity();
            }
        }, true);

        scope.$watch('attributeDataKeys', function () {
            if (ngModelCtrl.$viewValue) {
                var dataKeys = [];
                dataKeys = dataKeys.concat(scope.timeseriesDataKeys);
                dataKeys = dataKeys.concat(scope.attributeDataKeys);
                ngModelCtrl.$viewValue.dataKeys = dataKeys;
                scope.updateValidity();
            }
        }, true);

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var deviceAliasId = ngModelCtrl.$viewValue.deviceAliasId;
                if (scope.deviceAliases[deviceAliasId]) {
                    scope.deviceAlias = {id: deviceAliasId, alias: scope.deviceAliases[deviceAliasId].alias,
                        deviceId: scope.deviceAliases[deviceAliasId].deviceId};
                } else {
                    scope.deviceAlias = null;
                }
                var timeseriesDataKeys = [];
                var attributeDataKeys = [];
                for (var d in ngModelCtrl.$viewValue.dataKeys) {
                    var dataKey = ngModelCtrl.$viewValue.dataKeys[d];
                    if (dataKey.type === types.dataKeyType.timeseries) {
                        timeseriesDataKeys.push(dataKey);
                    } else if (dataKey.type === types.dataKeyType.attribute) {
                        attributeDataKeys.push(dataKey);
                    }
                }
                scope.timeseriesDataKeys = timeseriesDataKeys;
                scope.attributeDataKeys = attributeDataKeys;
            }
        };

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.selectedDeviceAliasChange = function () {
            if (!scope.timeseriesDataKeySearchText || scope.timeseriesDataKeySearchText === '') {
                scope.timeseriesDataKeySearchText = scope.timeseriesDataKeySearchText === '' ? null : '';
            }
            if (!scope.attributeDataKeySearchText || scope.attributeDataKeySearchText === '') {
                scope.attributeDataKeySearchText = scope.attributeDataKeySearchText === '' ? null : '';
            }
        };

        scope.transformTimeseriesDataKeyChip = function (chip) {
            return scope.generateDataKey({chip: chip, type: types.dataKeyType.timeseries});
        };

        scope.transformAttributeDataKeyChip = function (chip) {
            return scope.generateDataKey({chip: chip, type: types.dataKeyType.attribute});
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
                    deviceAlias: scope.deviceAlias,
                    deviceAliases: scope.deviceAliases
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
                }
                ngModelCtrl.$setDirty();
            }, function () {
            });
        };

        scope.dataKeysSearch = function (searchText, type) {
            if (scope.deviceAlias) {
                var deferred = $q.defer();
                scope.fetchDeviceKeys({deviceAliasId: scope.deviceAlias.id, query: searchText, type: type})
                    .then(function (dataKeys) {
                        deferred.resolve(dataKeys);
                    }, function (e) {
                        deferred.reject(e);
                    });
                return deferred.promise;
            } else {
                return $q.when([]);
            }
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
            deviceAliases: '=',
            datakeySettingsSchema: '=',
            generateDataKey: '&',
            fetchDeviceKeys: '&',
            onCreateDeviceAlias: '&'
        },
        link: linker
    };
}

/* eslint-enable angular/angularelement */
