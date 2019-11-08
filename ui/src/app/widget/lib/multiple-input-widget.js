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
import './multiple-input-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import multipleInputWidgetTemplate from './multiple-input-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.multipleInputWidget', [])
    .directive('tbMultipleInputWidget', MultipleInputWidget)
    .name;

/*@ngInject*/
function MultipleInputWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            formId: '=',
            ctx: '='
        },
        controller: MultipleInputWidgetController,
        controllerAs: 'vm',
        templateUrl: multipleInputWidgetTemplate
    };
}

/*@ngInject*/
function MultipleInputWidgetController($q, $scope, $translate, attributeService, toast, types, utils) {
    var vm = this;

    vm.entityDetected = false;
    vm.isAllParametersValid = true;

    vm.data = [];
    vm.datasources = null;

    vm.discardAll = discardAll;
    vm.inputChanged = inputChanged;
    vm.save = save;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            initializeConfig();
            updateDatasources();
        }
    });

    $scope.$on('multiple-input-data-updated', function(event, formId) {
        if (vm.formId == formId) {
            updateWidgetData(vm.subscription.data);
            $scope.$digest();
        }
    });

    $scope.$on('multiple-input-resize', function(event, formId) {
        if (vm.formId == formId) {
            updateWidgetDisplaying();
        }
    });

    function discardAll() {
        for (var i = 0; i < vm.data.length; i++) {
            vm.data[i].data.currentValue = vm.data[i].data.originalValue;
        }
        $scope.multipleInputForm.$setPristine();
    }

    function inputChanged(key) {
        if (!vm.settings.showActionButtons) {
            if (!key.settings.required || (key.settings.required && key.data && angular.isDefined(key.data.currentValue))) {
                vm.save(key);
            }
        }
    }

    function save(key) {
        var tasks = [];
        var serverAttributes = [], sharedAttributes = [], telemetry = [];
        var config = {
            ignoreLoading: !vm.settings.showActionButtons
        };
        var data;
        if (key) {
            data = [key];
        } else {
            data = vm.data;
        }
        for (let i = 0; i < data.length; i++) {
            var item = data[i];
            if (item.data.currentValue !== item.data.originalValue) {
                var attribute = {
                    key: item.name
                };
                switch (item.settings.dataKeyValueType) {
                    case 'dateTime':
                    case 'date':
                        attribute.value = item.data.currentValue.getTime();
                        break;
                    case 'time':
                        attribute.value = item.data.currentValue.getTime() - moment().startOf('day').valueOf();//eslint-disable-line
                        break;
                    default:
                        attribute.value = item.data.currentValue;
                }

                switch (item.settings.dataKeyType) {
                    case 'shared':
                        sharedAttributes.push(attribute);
                        break;
                    case 'timeseries':
                        telemetry.push(attribute);
                        break;
                    default:
                        serverAttributes.push(attribute);
                }
            }
        }
        for (let i = 0; i < serverAttributes.length; i++) {
            tasks.push(attributeService.saveEntityAttributes(
                vm.datasources[0].entityType,
                vm.datasources[0].entityId,
                types.attributesScope.server.value,
                serverAttributes,
                config));
        }
        for (let i = 0; i < sharedAttributes.length; i++) {
            tasks.push(attributeService.saveEntityAttributes(
                vm.datasources[0].entityType,
                vm.datasources[0].entityId,
                types.attributesScope.shared.value,
                sharedAttributes,
                config));
        }
        for (let i = 0; i < telemetry.length; i++) {
            tasks.push(attributeService.saveEntityTimeseries(
                vm.datasources[0].entityType,
                vm.datasources[0].entityId,
                types.latestTelemetry.value,
                telemetry,
                config));
        }
        if (tasks.length) {
            $q.all(tasks).then(
                function success() {
                    $scope.multipleInputForm.$setPristine();
                    if (vm.settings.showResultMessage) {
                        toast.showSuccess($translate.instant('widgets.input-widgets.update-successful'), 1000, angular.element(vm.ctx.$container), 'bottom left');
                    }
                },
                function fail() {
                    if (vm.settings.showResultMessage) {
                        toast.showError($translate.instant('widgets.input-widgets.update-failed'), angular.element(vm.ctx.$container), 'bottom left');
                    }
                }
            );
        }
    }

    function initializeConfig() {

        if (vm.settings.widgetTitle && vm.settings.widgetTitle.length) {
            vm.widgetTitle = utils.customTranslation(vm.settings.widgetTitle, vm.settings.widgetTitle);
        } else {
            vm.widgetTitle = vm.ctx.widgetConfig.title;
        }

        vm.ctx.widgetTitle = vm.widgetTitle;

        vm.isVerticalAlignment = !(vm.settings.fieldsAlignment === 'row');

        if (!vm.isVerticalAlignment && vm.settings.fieldsInRow) {
            vm.inputWidthSettings = 100 / vm.settings.fieldsInRow + '%';
        }
    }

    function updateDatasources() {
        if (vm.datasources && vm.datasources.length) {
            var datasource = vm.datasources[0];
            if (datasource.type === types.datasourceType.entity) {
                for (var i = 0; i < datasource.dataKeys.length; i++) {
                    if ((datasource.entityType !== types.entityType.device) && (datasource.dataKeys[i].settings.dataKeyType !== 'server')) {
                        vm.isAllParametersValid = false;
                    }
                    vm.data.push(datasource.dataKeys[i]);
                    vm.data[i].data = {};
                }
                vm.entityDetected = true;
            }
        }
    }

    function updateWidgetData(data) {
        for (var i = 0; i < vm.data.length; i++) {
            var keyData = data[i].data;
            if (keyData && keyData.length) {
                var value;
                switch (vm.data[i].settings.dataKeyValueType) {
                    case 'dateTime':
                    case 'date':
                        value = moment(keyData[0][1]).toDate(); // eslint-disable-line
                        break;
                    case 'time':
                        value = moment().startOf('day').add(keyData[0][1], 'ms').toDate(); // eslint-disable-line
                        break;
                    case 'booleanCheckbox':
                    case 'booleanSwitch':
                        value = (keyData[0][1] === 'true');
                        break;
                    default:
                        value = keyData[0][1];
                }

                vm.data[i].data = {
                    currentValue: value,
                    originalValue: value
                };

                if (vm.data[i].settings.isEditable === 'editable' && vm.data[i].settings.disabledOnDataKey) {
                    var conditions = data.filter((item) => {
                        return item.dataKey.name === vm.data[i].settings.disabledOnDataKey;
                    });
                    if (conditions && conditions.length) {
                        if (conditions[0].data.length) {
                            if (conditions[0].data[0][1] === 'false') {
                                vm.data[i].settings.disabledOnCondition = true;
                            } else {
                                vm.data[i].settings.disabledOnCondition = !conditions[0].data[0][1];
                            }
                        }
                    }
                }
            }
        }
    }

    function updateWidgetDisplaying() {
        vm.changeAlignment = (vm.ctx.$container[0].offsetWidth < 620);
        vm.smallWidthContainer = (vm.ctx.$container[0].offsetWidth < 420);
    }
}
