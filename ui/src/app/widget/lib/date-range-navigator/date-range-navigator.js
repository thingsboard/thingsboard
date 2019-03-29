/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */

/* eslint-disable import/no-unresolved, import/default */
import widgetTpl from './date-range-navigator.tpl.html';
import './date-range-navigator.scss';
/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.dateRangeNavigator', [])
    .directive('dateRangeNavigatorWidget', DateRangeNavigatorWidget)
    .name;

/*@ngInject*/
function DateRangeNavigatorWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            tableId: '=',
            ctx: '='
        },
        controller: DateRangeNavigatorWidgetController,
        controllerAs: 'vm',
        templateUrl: widgetTpl
    };
}

/*@ngInject*/
function DateRangeNavigatorWidgetController($scope, $window, $filter) {

    let vm = this,
        hour = 3600000,
        day = 86400000,
        week = 604800000,
        month = 2629743000,
        words = [
            "Mon",
            "Tue",
            "Wed",
            "Thu",
            "Fri",
            "Sat",
            "Sun",
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
            "Ok",
            'Custom Date Range',
            'Date Range Template',
            'Today',
            'Yesterday',
            'This Week',
            'Last Week',
            'Month',
            'This Month',
            'Last Month',
            'Year',
            'This Year',
            'Last Year'
        ],
        firstUpdate = true;

    $scope.datesMap = {
        hour: {
            ts: hour,
            label: "Hour"
        },
        day: {
            ts: day,
            label: "Day"
        },
        week: {
            ts: week,
            label: "Week"
        },
        twoWeeks: {
            ts: week * 2,
            label: "2 weeks"
        },
        month: {
            ts: month,
            label: "Month"
        },
        threeMonths: {
            ts: month * 3,
            label: "3 months"
        },
        sixMonths: {
            ts: month * 6,
            label: "6 months"
        }
    };
    $scope.advancedModel = {};
    $scope.endRestrictionDate = Date.now();
    $scope.localizationMap = getLocalizationMap();

    $scope.changeInterval = changeInterval;
    $scope.goForth = goForth;
    $scope.goBack = goBack;
    $scope.triggerChange = triggerChange;

    $scope.$watch('vm.ctx', function () {
        if (vm.ctx && vm.ctx.dashboard.dashboardTimewindow) {
            $scope.settings = vm.ctx.widgetConfig.settings;
            let selection;
            if ($scope.settings.useSessionStorage) {
                selection = readFromStorage('date-range');
            }
            if (selection) {
                $scope.advancedModel = {
                    selectedTemplateName: selection.name,
                    dateStart: selection.start,
                    dateEnd: selection.end
                };
            } else {
                let end = new Date();
                end.setHours(23,59,59,999);

                let formattedDate = getFormattedDate(
                    (end.getTime() + 1) - $scope.datesMap[$scope.settings.initialInterval || "week"].ts,
                    end.getTime()
                );
                $scope.advancedModel = formattedDate;
            }
            $scope.selectedStepSize = $scope.datesMap[$scope.settings.stepSize || "day"].ts;

            widgetContextTimewindowSync();
        }
    });

    $scope.$on('dashboardTimewindowChanged', function () {
        $scope.dashboardTimewindowChanged = true;
        widgetContextTimewindowSync();
    });

    function getLocalizationMap() {
        let result = {};

        words.forEach(function (key) {
            result[key] = $filter('translate')('widgets.date-range-navigator.localizationMap.' + key);
        });

        return result;
    }

    function triggerChange() {
        updateTimewindow($scope.advancedModel.dateStart.getTime(), $scope.advancedModel.dateEnd.getTime() + day - 1);
    }

    function widgetContextTimewindowSync() {
        if (vm.ctx && vm.ctx.dashboardTimewindow && $scope.dashboardTimewindowChanged &&
            vm.ctx.dashboard.dashboardTimewindow.history &&
            vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow) {


            if (!firstUpdate) {
                updateAdvancedModel();
            }
            updateDateInterval();
            if ($scope.settings.useSessionStorage) {
                updateStorageDate();
            }
            if (firstUpdate) {
                updateTimewindow($scope.advancedModel.dateStart.getTime(), $scope.advancedModel.dateEnd.getTime());
                firstUpdate = false;
            }
        }
    }

    function getFormattedDate(startTime, endTime) {
        var template;

        let startDate = new Date(startTime);
        let endDate = new Date(endTime);

        if (getDateDiff(startDate, endDate) === 0) {
            template = $filter('date')(startDate, 'dd MMM yyyy');
        } else {
            template = $filter('date')(
                startDate,
                'dd' + (startDate.getMonth() !== endDate.getMonth() || startDate.getFullYear() !== endDate.getFullYear() ? ' MMM' : '') + (startDate.getFullYear() !== endDate.getFullYear() ? ' yyyy' : '')
                )
                + ' - ' +
                $filter('date')(
                    endDate,
                    'dd MMM yyyy'
                );
        }

        return {
            selectedTemplateName: template,
            dateStart: startDate,
            dateEnd: endDate
        };
    }


    function readFromStorage(itemKey) {
        if ($window.sessionStorage.getItem(itemKey)) {
            let selection = angular.fromJson($window.sessionStorage.getItem(itemKey));
            selection.start = new Date(parseInt(selection.start));
            selection.end = new Date(parseInt(selection.end));
            return selection;
        }

        return undefined;
    }

    function goForth() {
        updateTimewindow(vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.startTimeMs + $scope.selectedStepSize, vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.endTimeMs + $scope.selectedStepSize);
    }

    function goBack() {
        updateTimewindow(vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.startTimeMs - $scope.selectedStepSize, vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.endTimeMs - $scope.selectedStepSize);
    }

    function changeInterval() {
        updateTimewindow(vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.endTimeMs - $scope.selectedDateInterval / 2, vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.endTimeMs + $scope.selectedDateInterval / 2);
    }

    function getDateDiff(date1, date2) {
        if (!date1 || !date2) return;
        var _d1 = new Date(date1.getFullYear(), date1.getMonth(), date1.getDate()),
            _d2 = new Date(date2.getFullYear(), date2.getMonth(), date2.getDate());
        return _d2 - _d1;
    }

    function updateTimewindow(startTime, endTime) {
        vm.ctx.dashboard.dashboardTimewindowApi.onUpdateTimewindow(startTime, endTime, 10);
    }

    function updateDateInterval() {
        let interval = $scope.advancedModel.dateEnd.getTime() - $scope.advancedModel.dateStart.getTime();

        for (let i in $scope.datesMap) {
            if ($scope.datesMap.hasOwnProperty(i)) {
                if ($scope.datesMap[i].ts === interval || $scope.datesMap[i].ts === interval + 1 || $scope.datesMap[i].ts === interval - 1) {
                    $scope.selectedDateInterval = $scope.datesMap[i].ts;
                    $scope.customInterval = false;
                    return;
                }
            }
        }

        $scope.selectedDateInterval = interval;
        $scope.customInterval = {ts: interval, label: "Custom interval"};
    }

    function updateAdvancedModel() {
        $scope.advancedModel = getFormattedDate(vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.startTimeMs, vm.ctx.dashboard.dashboardTimewindow.history.fixedTimewindow.endTimeMs);
    }

    function updateStorageDate() {
        saveIntoStorage('date-range', {
            start: $scope.advancedModel.dateStart.getTime(),
            end: $scope.advancedModel.dateEnd.getTime(),
            name: $scope.advancedModel.selectedTemplateName
        });
    }

    function saveIntoStorage(keyName, selection) {
        if (selection) {
            $window.sessionStorage.setItem(keyName, angular.toJson(selection));
        }
    }
}