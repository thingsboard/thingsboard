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
import './datasource-func.scss';

import 'md-color-picker';
import tinycolor from 'tinycolor2';
import $ from 'jquery';
import thingsboardTypes from '../common/types.constant';
import thingsboardUtils from '../common/utils.service';
import thingsboardDatakeyConfigDialog from './datakey-config-dialog.controller';

/* eslint-disable import/no-unresolved, import/default */

import datasourceFuncTemplate from './datasource-func.tpl.html';
import datakeyConfigDialogTemplate from './datakey-config-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.datasourceFunc', [thingsboardTypes, thingsboardUtils, thingsboardDatakeyConfigDialog])
    .directive('tbDatasourceFunc', DatasourceFunc)
    .name;

/*@ngInject*/
function DatasourceFunc($compile, $templateCache, $mdDialog, $window, $document, $mdColorPicker, types, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(datasourceFuncTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.functionTypes = utils.getPredefinedFunctionsList();

        scope.selectedDataKey = null;
        scope.dataKeySearchText = null;

        scope.updateValidity = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var dataValid = angular.isDefined(value) && value != null;
                ngModelCtrl.$setValidity('deviceData', dataValid);
                if (dataValid) {
                    ngModelCtrl.$setValidity('funcTypes',
                        angular.isDefined(value.dataKeys) &&
                        value.dataKeys != null &&
                        value.dataKeys.length > 0);
                }
            }
        };

        scope.$watch('funcDataKeys', function () {
            if (ngModelCtrl.$viewValue) {
                var dataKeys = [];
                dataKeys = dataKeys.concat(scope.funcDataKeys);
                ngModelCtrl.$viewValue.dataKeys = dataKeys;
                scope.updateValidity();
            }
        }, true);

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var funcDataKeys = [];
                if (ngModelCtrl.$viewValue.dataKeys) {
                    funcDataKeys = funcDataKeys.concat(ngModelCtrl.$viewValue.dataKeys);
                }
                scope.funcDataKeys = funcDataKeys;
            }
        };

        scope.transformDataKeyChip = function (chip) {
            return scope.generateDataKey({chip: chip, type: types.dataKeyType.function});
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
                    deviceAlias: null,
                    deviceAliases: null
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
                scope.funcDataKeys[index] = dataKey;
                ngModelCtrl.$setDirty();
            }, function () {
            });
        };

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.dataKeysSearch = function (dataKeySearchText) {
            var dataKeys = dataKeySearchText ? scope.functionTypes.filter(
                scope.createFilterForDataKey(dataKeySearchText)) : scope.functionTypes;
            return dataKeys;
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
            generateDataKey: '&',
            datakeySettingsSchema: '='
        },
        link: linker
    };
}

/* eslint-enable angular/angularelement */
