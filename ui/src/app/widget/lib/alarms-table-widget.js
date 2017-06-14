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

import './alarms-table-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import alarmsTableWidgetTemplate from './alarms-table-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import tinycolor from 'tinycolor2';
import cssjs from '../../../vendor/css.js/css';

export default angular.module('thingsboard.widgets.alarmsTableWidget', [])
    .directive('tbAlarmsTableWidget', AlarmsTableWidget)
    .name;

/*@ngInject*/
function AlarmsTableWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            tableId: '=',
            config: '=',
            subscription: '='
        },
        controller: AlarmsTableWidgetController,
        controllerAs: 'vm',
        templateUrl: alarmsTableWidgetTemplate
    };
}

/*@ngInject*/
function AlarmsTableWidgetController($element, $scope, $filter, $mdMedia, $mdUtil, $translate, utils, types) {
    var vm = this;

    vm.stylesInfo = {};
    vm.contentsInfo = {};

    vm.showData = true;
    vm.hasData = false;

    vm.alarms = [];
    vm.alarmsCount = 0;
    vm.selectedAlarms = []

    vm.alarmSource = null;
    vm.allAlarms = null;

    vm.currentAlarm = null;

    vm.query = {
        order: '-'+types.alarmFields.createdTime.value,
        limit: 10,
        page: 1,
        search: null
    };

    vm.alarmsTitle = $translate.instant('alarm.alarms');

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;

    vm.cellStyle = cellStyle;
    vm.cellContent = cellContent;

    $scope.$watch('vm.config', function() {
        if (vm.config) {
            vm.settings = vm.config.settings;
            vm.widgetConfig = vm.config.widgetConfig;
            initializeConfig();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateAlarms();
        }
    });

    $scope.$watch('vm.subscription', function() {
        if (vm.subscription) {
            vm.alarmSource = vm.subscription.alarmSource;
            updateAlarmSource();
        }
    });

    $scope.$on('alarms-table-data-updated', function(event, tableId) {
        if (vm.tableId == tableId) {
            if (vm.subscription) {
                vm.allAlarms = vm.subscription.alarms;
                updateAlarms(true);
                $scope.$digest();
            }
        }
    });

    $scope.$watch(function() { return $mdMedia('gt-xs'); }, function(isGtXs) {
        vm.isGtXs = isGtXs;
    });

    $scope.$watch(function() { return $mdMedia('gt-md'); }, function(isGtMd) {
        vm.isGtMd = isGtMd;
        if (vm.isGtMd) {
            vm.limitOptions = [5, 10, 15];
        } else {
            vm.limitOptions = null;
        }
    });

    function initializeConfig() {

        if (vm.settings.alarmsTitle && vm.settings.alarmsTitle.length) {
            vm.alarmsTitle = vm.settings.alarmsTitle;
        }
        //TODO:

        var origColor = vm.widgetConfig.color || 'rgba(0, 0, 0, 0.87)';
        var defaultColor = tinycolor(origColor);
        var mdDark = defaultColor.setAlpha(0.87).toRgbString();
        var mdDarkSecondary = defaultColor.setAlpha(0.54).toRgbString();
        var mdDarkDisabled = defaultColor.setAlpha(0.26).toRgbString();
        //var mdDarkIcon = mdDarkSecondary;
        var mdDarkDivider = defaultColor.setAlpha(0.12).toRgbString();

        var cssString = 'table.md-table th.md-column {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
            '}\n'+
            'table.md-table th.md-column md-icon.md-sort-icon {\n'+
            'color: ' + mdDarkDisabled + ';\n'+
            '}\n'+
            'table.md-table th.md-column.md-active, table.md-table th.md-column.md-active md-icon {\n'+
            'color: ' + mdDark + ';\n'+
            '}\n'+
            'table.md-table td.md-cell {\n'+
            'color: ' + mdDark + ';\n'+
            'border-top: 1px '+mdDarkDivider+' solid;\n'+
            '}\n'+
            'table.md-table td.md-cell.md-placeholder {\n'+
            'color: ' + mdDarkDisabled + ';\n'+
            '}\n'+
            'table.md-table td.md-cell md-select > .md-select-value > span.md-select-icon {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
            '}\n'+
            '.md-table-pagination {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
            'border-top: 1px '+mdDarkDivider+' solid;\n'+
            '}\n'+
            '.md-table-pagination .buttons md-icon {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
            '}\n'+
            '.md-table-pagination md-select:not([disabled]):focus .md-select-value {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
            '}';

        var cssParser = new cssjs();
        cssParser.testMode = false;
        var namespace = 'ts-table-' + hashCode(cssString);
        cssParser.cssPreviewNamespace = namespace;
        cssParser.createStyleElement(namespace, cssString);
        $element.addClass(namespace);

        function hashCode(str) {
            var hash = 0;
            var i, char;
            if (str.length === 0) return hash;
            for (i = 0; i < str.length; i++) {
                char = str.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash = hash & hash;
            }
            return hash;
        }
    }

    function enterFilterMode () {
        vm.query.search = '';
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateAlarms();
    }

    function onReorder () {
        updateAlarms();
    }

    function onPaginate () {
        updateAlarms();
    }

    function updateAlarms(preserveSelections) {
        if (!preserveSelections) {
            vm.selectedAlarms = [];
        }
        var result = $filter('orderBy')(vm.allAlarms, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.alarmsCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.alarms = result.slice(startIndex, startIndex + vm.query.limit);
        if (preserveSelections) {
            var newSelectedAlarms = [];
            if (vm.selectedAlarms && vm.selectedAlarms.length) {
                var i = vm.selectedAlarms.length;
                while (i--) {
                    var selectedAlarm = vm.selectedAlarms[i];
                    if (selectedAlarm.id) {
                        result = $filter('filter')(vm.alarms, {id: {id: selectedAlarm.id.id} });
                        if (result && result.length) {
                            newSelectedAlarms.push(result[0]);
                        }
                    }
                }
            }
            vm.selectedAlarms = newSelectedAlarms;
        }
    }

    function cellStyle(alarm, key) {
        var style = {};
        if (alarm && key) {
            var styleInfo = vm.stylesInfo[key.label];
            var value = getAlarmValue(alarm, key);
            if (styleInfo.useCellStyleFunction && styleInfo.cellStyleFunction) {
                try {
                    style = styleInfo.cellStyleFunction(value);
                } catch (e) {
                    style = {};
                }
            } else {
                style = defaultStyle(key, value);
            }
        }
        return style;
    }

    function cellContent(alarm, key) {
        var strContent = '';
        if (alarm && key) {
            var contentInfo = vm.contentsInfo[key.label];
            var value = getAlarmValue(alarm, key);
            if (contentInfo.useCellContentFunction && contentInfo.cellContentFunction) {
                if (angular.isDefined(value)) {
                    strContent = '' + value;
                }
                var content = strContent;
                try {
                    content = contentInfo.cellContentFunction(value, alarm, $filter);
                } catch (e) {
                    content = strContent;
                }
            } else {
                content = defaultContent(key, value);
            }
            return content;
        } else {
            return strContent;
        }
    }

    function defaultContent(key, value) {
        if (angular.isDefined(value)) {
            var alarmField = types.alarmFields[key.name];
            if (alarmField) {
                if (alarmField.time) {
                    return $filter('date')(value, 'yyyy-MM-dd HH:mm:ss');
                } else if (alarmField.value == types.alarmFields.severity.value) {
                    return $translate.instant(types.alarmSeverity[value].name);
                } else if (alarmField.value == types.alarmFields.status.value) {
                    return $translate.instant('alarm.display-status.'+value);
                } else if (alarmField.value == types.alarmFields.originatorType.value) {
                    return $translate.instant(types.entityTypeTranslations[value].type);
                }
                else {
                    return value;
                }
            } else {
                return '';
            }
        } else {
            return '';
        }
    }
    function defaultStyle(key, value) {
        if (angular.isDefined(value)) {
            var alarmField = types.alarmFields[key.name];
            if (alarmField) {
                if (alarmField.value == types.alarmFields.severity.value) {
                    return {
                        fontWeight: 'bold',
                        color: types.alarmSeverity[value].color
                    };
                } else {
                    return {};
                }
            }
        } else {
            return {};
        }
    }

    const getDescendantProp = (obj, path) => (
        path.split('.').reduce((acc, part) => acc && acc[part], obj)
    );

    function getAlarmValue(alarm, key) {
        var alarmField = types.alarmFields[key.name];
        if (alarmField) {
            return getDescendantProp(alarm, alarmField.value);
        } else {
            return getDescendantProp(alarm, key.name);
        }
    }

    function updateAlarmSource() {

        if (vm.settings.alarmsTitle && vm.settings.alarmsTitle.length) {
            vm.alarmsTitle = utils.createLabelFromDatasource(vm.alarmSource, vm.settings.alarmsTitle);
        }

        vm.stylesInfo = {};
        vm.contentsInfo = {};

        for (var d = 0; d < vm.alarmSource.dataKeys.length; d++ ) {
            var dataKey = vm.alarmSource.dataKeys[d];
            var keySettings = dataKey.settings;

            var cellStyleFunction = null;
            var useCellStyleFunction = false;

            if (keySettings.useCellStyleFunction === true) {
                if (angular.isDefined(keySettings.cellStyleFunction) && keySettings.cellStyleFunction.length > 0) {
                    try {
                        cellStyleFunction = new Function('value', keySettings.cellStyleFunction);
                        useCellStyleFunction = true;
                    } catch (e) {
                        cellStyleFunction = null;
                        useCellStyleFunction = false;
                    }
                }
            }

            vm.stylesInfo[dataKey.label] = {
                useCellStyleFunction: useCellStyleFunction,
                cellStyleFunction: cellStyleFunction
            };

            var cellContentFunction = null;
            var useCellContentFunction = false;

            if (keySettings.useCellContentFunction === true) {
                if (angular.isDefined(keySettings.cellContentFunction) && keySettings.cellContentFunction.length > 0) {
                    try {
                        cellContentFunction = new Function('value, alarm, filter', keySettings.cellContentFunction);
                        useCellContentFunction = true;
                    } catch (e) {
                        cellContentFunction = null;
                        useCellContentFunction = false;
                    }
                }
            }

            vm.contentsInfo[dataKey.label] = {
                useCellContentFunction: useCellContentFunction,
                cellContentFunction: cellContentFunction
            };
        }
    }

}