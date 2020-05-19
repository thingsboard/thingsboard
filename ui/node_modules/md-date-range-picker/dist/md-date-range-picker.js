/*
* Name: md-date-range-picker
* Version: 0.8.4
* Build Date: 3/12/2019
* Author: roel barreto <greatcodeideas@gmail.com>
*/
(function (window, angular) {

    angular
        .module('ngMaterialDateRangePicker', ['ngMaterial'])
        .directive('mdDateRangePicker', mdDateRangePickerDirective)
        .directive('mdDateRange', mdDateRangeDirective)
        .controller('mdDateRangePickerCtrl', mdDateRangePickerCtrl)
        .service('$mdDateRangePicker', mdDateRangePickerService);

    /**
     * scope here is non-bi-directional
     */
    mdDateRangePickerDirective.$inject = ['$mdDateRangePicker'];
    function mdDateRangePickerDirective($mdDateRangePicker) {
        var directive = {
            scope: {
                selectedTemplate: '=',
                selectedTemplateName: '=',
                dateStart: '=?',
                dateEnd: '=?',
                firstDayOfWeek: '=?',
                showTemplate: '=?',
                mdOnSelect: '&',
                localizationMap: '=?',
                customTemplates: '=?',
                disableTemplates: '@',
                maxRange: '=?',
                onePanel: '=?',
                isDisabledDate: '&?',
                format: '=?',
            },
            template: '<div class="md-date-range-picker md-whiteframe-1dp" ng-class="{\'md-date-range-picker__one-panel\':onePanel}"><div layout="column"><div layout="row" layout-margin><div class="md-date-range-picker__calendar-wrapper"><div class="md-date-range-picker__month-year" layout="row" layout-align="center center"><div flex layout="column" layout-align="center center"><span aria-label="Previous Month" class="md-button md-icon-button" event-key="prev"><md-icon md-svg-src="data:image/svg+xml;base64,PHN2ZyB2ZXJzaW9uPSIxLjEiIHg9IjBweCIgeT0iMHB4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxnPjxwb2x5Z29uIHBvaW50cz0iMTUuNCw3LjQgMTQsNiA4LDEyIDE0LDE4IDE1LjQsMTYuNiAxMC44LDEyICIvPjwvZz48L3N2Zz4="></md-icon></span></div><md-select md-container-class="md-date-range-picker__select" md-on-close="updateActiveDate()" ng-model="activeMonth" placeholder="{{::getLocalizationVal(\'Month\')}}" class="md-no-underline"><md-option ng-value="::month.id" ng-repeat="month in months" ng-bind="::month.name"></md-option></md-select><md-select md-container-class="md-date-range-picker__select" md-on-close="updateActiveDate()" ng-model="activeYear" placeholder="{{::getLocalizationVal(\'Year\')}}" class="md-no-underline"><md-option ng-value="::year.id" ng-repeat="year in years" ng-bind="::year.name"></md-option></md-select><div flex layout="column" layout-align="center center" class="hide-gt-sm show-sm show-xs"><span aria-label="Next Month" class="md-icon-button md-button" event-key="next"><md-icon style="transform: rotate(-180deg)" md-svg-src="data:image/svg+xml;base64,PHN2ZyB2ZXJzaW9uPSIxLjEiIHg9IjBweCIgeT0iMHB4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxnPjxwb2x5Z29uIHBvaW50cz0iMTUuNCw3LjQgMTQsNiA4LDEyIDE0LDE4IDE1LjQsMTYuNiAxMC44LDEyICIvPjwvZz48L3N2Zz4="></md-icon></span></div><div flex ng-if="onePanel" layout="column" layout-align="center center" class="hide-sm hide-xs show-gt-sm"><span aria-label="Next Month" class="md-icon-button md-button" event-key="next"><md-icon style="transform: rotate(-180deg)" md-svg-src="data:image/svg+xml;base64,PHN2ZyB2ZXJzaW9uPSIxLjEiIHg9IjBweCIgeT0iMHB4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxnPjxwb2x5Z29uIHBvaW50cz0iMTUuNCw3LjQgMTQsNiA4LDEyIDE0LDE4IDE1LjQsMTYuNiAxMC44LDEyICIvPjwvZz48L3N2Zz4="></md-icon></span></div><div flex ng-if="!onePanel" layout="column" layout-align="center center" class="hide-sm hide-xs show-gt-sm"><span aria-label="Next Month" ng-disabled="true" aria-hidden="true" class="md-icon-button md-button"><md-icon></md-icon></span></div></div><div class="md-date-range-picker__week" style="font-size: 0"><span class="md-date-range-picker__calendar__grid" ng-repeat="day in days">{{::day.name}}</span></div><div class="md-date-range-picker__calendar"><span ng-repeat="date in dates" class="md-date-range-picker__calendar__grid" ng-class="{\'md-date-range-picker__calendar__selected\':inSelectedDateRange(date),\'md-date-range-picker__calendar__start\':isSelectedStartDate(date),\'md-date-range-picker__calendar__end\':isSelectedEndDate(date),\'md-date-range-picker__calendar__not-in-active-month\': !inCurrentMonth(date),\'md-date-range-picker__calendar__today\' : isToday(date),\'md-date-range-picker__calendar__disabled\': !isInMaxRange(date) || isDisabledDate({$date:date})}" event-key="date1" event-param="{{$index}}"><span event-key="date1" event-param="{{$index}}" class="md-date-range-picker__calendar__selection" ng-bind="{{::date.getDate()}}"></span></span></div></div><div ng-show="!onePanel" class="md-date-range-picker__calendar-wrapper hide-sm hide-xs show-gt-sm"><div class="md-date-range-picker__month-year" layout="row" layout-align="center center"><div flex layout="column" layout-align="center center" style="visibility: hidden"><span aria-label="Previous Month" class="md-button md-icon-button" event-key="prev"><md-icon md-svg-src="data:image/svg+xml;base64,PHN2ZyB2ZXJzaW9uPSIxLjEiIHg9IjBweCIgeT0iMHB4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxnPjxwb2x5Z29uIHBvaW50cz0iMTUuNCw3LjQgMTQsNiA4LDEyIDE0LDE4IDE1LjQsMTYuNiAxMC44LDEyICIvPjwvZz48L3N2Zz4="></md-icon></span></div><md-select md-container-class="md-date-range-picker__select" md-on-close="updateActiveDate(true)" ng-model="activeMonth2" placeholder="{{::getLocalizationVal(\'Month\')}}" class="md-no-underline"><md-option ng-value="::month.id" ng-repeat="month in months" ng-bind="::month.name"></md-option></md-select><md-select md-container-class="md-date-range-picker__select" md-on-close="updateActiveDate(true)" ng-model="activeYear2" placeholder="{{::getLocalizationVal(\'Year\')}}" class="md-no-underline"><md-option ng-value="::year.id" ng-repeat="year in years" ng-bind="::year.name"></md-option></md-select><div flex layout="column" layout-align="center center"><span aria-label="Next Month" class="md-icon-button md-button" event-key="next"><md-icon style="transform: rotate(-180deg)" md-svg-src="data:image/svg+xml;base64,PHN2ZyB2ZXJzaW9uPSIxLjEiIHg9IjBweCIgeT0iMHB4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxnPjxwb2x5Z29uIHBvaW50cz0iMTUuNCw3LjQgMTQsNiA4LDEyIDE0LDE4IDE1LjQsMTYuNiAxMC44LDEyICIvPjwvZz48L3N2Zz4="></md-icon></span></div></div><div class="md-date-range-picker__week" style="font-size: 0" ng-if="!onePanel"><span class="md-date-range-picker__calendar__grid" ng-repeat="day in days">{{::day.name}}</span></div><div class="md-date-range-picker__calendar"><span ng-repeat="date in dates2" class="md-date-range-picker__calendar__grid" ng-class="{\'md-date-range-picker__calendar__selected\':inSelectedDateRange(date), \'md-date-range-picker__calendar__start\':isSelectedStartDate(date), \'md-date-range-picker__calendar__end\':isSelectedEndDate(date), \'md-date-range-picker__calendar__not-in-active-month\': !inCurrentMonth(date, true), \'md-date-range-picker__calendar__today\' : isToday(date),\'md-date-range-picker__calendar__disabled\': !isInMaxRange(date) || isDisabledDate({$date:date}) }" event-key="date2" event-param="{{$index}}"><span event-key="date2" event-param="{{$index}}" class="md-date-range-picker__calendar__selection" ng-bind="{{::date.getDate()}}"></span></span></div></div></div><div class="md-date-range-picker__templates" ng-if="showTemplate"><div ng-if="!onePanel && !maxRange" class="hide-xs hide-sm show-gt-sm" layout="row" layout-align="center center" layout-wrap><div ng-repeat="(tmpltKey,tmpltName) in selectionTemplate" class="md-button" aria-label="{{::tmpltName}}" ng-class="selectedTemplate === tmpltKey ? \'md-primary md-raised\' : \'\'" event-key="{{tmpltKey}}" ng-bind="::tmpltName" flex="20"></div><div ng-repeat="tmplt in customTemplates" class="md-button" aria-label="{{::tmplt.name}}" ng-class="selectedTemplate === tmplt.name ? \'md-primary md-raised\' : \'\'" ng-click="selectCustomRange(tmplt.name,tmplt)" event-key="Custom Template" ng-bind="::tmplt.name" flex="20"></div></div><div ng-if="!maxRange" ng-class="{\'hide-gt-sm\':!onePanel}" layout="column" layout-padding><md-input-container><label>{{::getLocalizationVal(\'Date Range Template\')}}</label><md-select md-container-class="md-date-range-picker__select" class="md-block" placeholder="{{::getLocalizationVal(\'Custom Date Range\')}}" ng-model="selectedTemplate"><md-option value=""></md-option><md-option ng-repeat="(tmpltKey,tmpltName) in selectionTemplate track by tmpltKey" aria-label="{{::tmpltName}}" ng-click="actionByKey(tmpltKey,null)" ng-bind="::tmpltName" value="{{tmpltKey}}"></md-option><md-option ng-repeat="tmplt in customTemplates" aria-label="{{::tmplt.name}}" ng-click="selectCustomRange(tmplt.name,tmplt)" ng-bind="::tmplt.name" value="{{::tmplt.name}}"></md-option></md-select></md-input-container></div></div></div></div>',
            controller: 'mdDateRangePickerCtrl',
            link: function (scope, element, attributes, ctrl) {
                scope.actionByKey = function (eventKey, eventParam, e) {
                    switch (eventKey) {
                        case 'prev':
                            scope.handleClickPrevMonth(e);
                            scope.runIfNotInDigest();
                            break;
                        case 'next':
                            scope.handleClickNextMonth(e);
                            scope.runIfNotInDigest();
                            break;
                        case 'date1':
                            if (scope.handleClickDate(e, scope.dates[eventParam])) {
                                scope.runIfNotInDigest(scope.triggerChange);
                            } else {
                                scope.runIfNotInDigest();
                            }
                            break;
                        case 'date2':
                            if (scope.handleClickDate(e, scope.dates2[eventParam])) {
                                scope.runIfNotInDigest(scope.triggerChange);
                            } else {
                                scope.runIfNotInDigest();
                            }
                            break;
                        case 'TD':
                            scope.handleClickSelectToday();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'YD':
                            scope.handleClickSelectYesterday();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'TW':
                            scope.handleClickSelectThisWeek();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'LW':
                            scope.handleClickSelectLastWeek();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'TM':
                            scope.handleClickSelectThisMonth();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'LM':
                            scope.handleClickSelectLastMonth();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'TY':
                            scope.handleClickSelectThisYear();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'LY':
                            scope.handleClickSelectLastYear();
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        case 'Custom Template':
                            scope.runIfNotInDigest(scope.triggerChange);
                            break;
                        default:
                            break;
                    }
                }

                scope.runIfNotInDigest = function (operation) {
                    if (scope.$root != null && !scope.$root.$$phase) { // check if digest already in progress
                        scope.$apply(); // launch digest;
                        if (operation && typeof operation === 'function') {
                            operation();
                        }
                    }
                };
                element.on('click', function (e) {
                    var eventKey = e.target.getAttribute('event-key'),
                        eventParam = e.target.getAttribute('event-param');
                        scope.actionByKey(eventKey, eventParam, e);
                });

                scope.triggerChange = function triggerChange(e) {
                    var $dates = $mdDateRangePicker.getSelectedDate(scope.dateStart, scope.dateEnd, scope.isDisabledDate, scope.maxRange); // All selected enabled dates
                    if (scope.mdOnSelect) {
                        scope.mdOnSelect({ $dates: $dates });
                    }
                };
            }
        };
        return directive
    }

    mdDateRangePickerCtrl.$inject = ['$scope', '$filter'];
    function mdDateRangePickerCtrl($scope, $filter) {
        var ctrl = $scope, NUMBER_OF_MONTH_TO_DISPLAY = 2,
            SELECTION_TEMPLATES = {
                'TD': getLocalizationVal('Today'),
                'YD': getLocalizationVal('Yesterday'),
                'TW': getLocalizationVal('This Week'),
                'LW': getLocalizationVal('Last Week'),
                'TM': getLocalizationVal('This Month'),
                'LM': getLocalizationVal('Last Month'),
                'TY': getLocalizationVal('This Year'),
                'LY': getLocalizationVal('Last Year')
            }, START_OF_WEEK = 1
        SELECTION_TEMPLATES_CUSTOM = {}
            ;
        $scope.isMenuContainer = false;
        $scope.days = [];
        $scope.label = 'Date range picker';
        $scope.dates = [];
        $scope.dates2 = [];
        $scope.numberOfMonthToDisplay = 2;
        $scope.today = new Date();
        $scope.dateStart && $scope.dateStart.setHours(0, 0, 0, 0);
        $scope.dateEnd && $scope.dateEnd.setHours(23, 59, 59, 999);
        $scope.firstDayOfMonth = $scope.dateStart ? new Date($scope.dateStart.getFullYear(), $scope.dateStart.getMonth(), 1) : Date($scope.today.getFullYear(), $scope.today.getMonth(), 1);
        $scope.lastDayOfMonth = $scope.dateStart ? new Date($scope.dateStart.getFullYear(), $scope.dateStart.getMonth() + 1, 0) : Date($scope.today.getFullYear(), $scope.today.getMonth() + 1, 0);
        $scope.activeDate = $scope.dateStart || $scope.today;
        $scope.activeDate2 = new Date($scope.activeDate.getFullYear(), $scope.activeDate.getMonth() + 1, 1);
        $scope.activeMonth = $scope.activeDate.getMonth();
        $scope.activeYear = $scope.activeDate.getFullYear();
        $scope.activeMonth2 = $scope.activeDate2.getMonth();
        $scope.activeYear2 = $scope.activeDate2.getFullYear();
        $scope.months = [];
        $scope.years = [];

        $scope.inCurrentMonth = inCurrentMonth;
        $scope.isToday = isToday;
        $scope.handleClickDate = handleClickDate;
        $scope.inSelectedDateRange = inSelectedDateRange;
        $scope.isSelectedStartDate = isSelectedStartDate;
        $scope.isSelectedEndDate = isSelectedEndDate;

        $scope.updateActiveDate = updateActiveDate;
        $scope.selectedDateText = selectedDateText;
        $scope.focusToDate = focusToDate;

        $scope.handleClickNextMonth = handleClickNextMonth;
        $scope.handleClickPrevMonth = handleClickPrevMonth;

        $scope.handleClickSelectToday = handleClickSelectToday;
        $scope.handleClickSelectYesterday = handleClickSelectYesterday;
        $scope.handleClickSelectThisWeek = handleClickSelectThisWeek;
        $scope.handleClickSelectLastWeek = handleClickSelectLastWeek;
        $scope.handleClickSelectThisMonth = handleClickSelectThisMonth;
        $scope.handleClickSelectLastMonth = handleClickSelectLastMonth;
        $scope.handleClickSelectThisYear = handleClickSelectThisYear;
        $scope.handleClickSelectLastYear = handleClickSelectLastYear;

        $scope.getLocalizationVal = getLocalizationVal;
        $scope.selectCustomRange = selectCustomRange;
        $scope.isInMaxRange = isInMaxRange;
        $scope.selectionTemplate = {};

        init();

        function init() {
            var mctr = 0;
            var currTmpl;

            /** 
             * add custom template to local custom template array 
            */
            if ($scope.customTemplates != null) {
                for (var i = 0; i < $scope.customTemplates.length; i++) {
                    currTmpl = $scope.customTemplates[i];
                    SELECTION_TEMPLATES_CUSTOM[currTmpl.name] = currTmpl;
                }
            }
            if ($scope.selectedTemplate) {
                switch ($scope.selectedTemplate) {
                    case 'TD':
                        $scope.handleClickSelectToday();
                        break;
                    case 'YD':
                        $scope.handleClickSelectYesterday();
                        break;
                    case 'TW':
                        $scope.handleClickSelectThisWeek();
                        break;
                    case 'LW':
                        $scope.handleClickSelectLastWeek();
                        break;
                    case 'TM':
                        $scope.handleClickSelectThisMonth();
                        break;
                    case 'LM':
                        $scope.handleClickSelectLastMonth();
                        break;
                    case 'TY':
                        $scope.handleClickSelectThisYear();
                        break;
                    case 'LY':
                        $scope.handleClickSelectLastYear();
                        break;
                    default:
                        if (SELECTION_TEMPLATES_CUSTOM && SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate] && SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate].dateStart && SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate].dateEnd) {
                            $scope.dateStart = SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate].dateStart;
                            $scope.dateEnd = SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate].dateEnd;
                        }
                        $scope.selectedTemplateName = $scope.selectedDateText();
                        break;
                }
                $scope.updateActiveDate();
            } else {
                $scope.selectedTemplate = '';
                $scope.selectedTemplateName = $scope.selectedDateText();
                $scope.updateActiveDate();
            }

            $scope.$watch('selectedTemplate', function (next, prev) {
                if (next !== prev && $scope.dateStart && !$scope.inCurrentMonth($scope.dateStart) && !$scope.inCurrentMonth($scope.dateStart, true)) {
                    $scope.focusToDate($scope.dateStart);
                }
            });
            $scope.$watch('dateStart', function (next, prev) {
                if (next !== prev && $scope.dateStart && !$scope.inCurrentMonth($scope.dateStart) && !$scope.inCurrentMonth($scope.dateStart, true)) {
                    $scope.focusToDate($scope.dateStart);
                }
            });

            /**
             * Generate Days of Week Names
             * Fact: January 1st of 2017 is Sunday
             */
            var w = new Date(2017, 0, 1);
            $scope.days = [];
            for (mctr = 0; mctr < 7; mctr++) {
                //add $scope.firstDayOfWeek to set the first Day of week e.g. -1 = Sunday, 0 = Monday 
                w.setDate(mctr + 1 + getFirstDayOfWeek());
                $scope.days.push({ id: mctr, name: getLocalizationVal($filter('date')(w, 'EEE')) });
            }
            /**
             * Generate Month Names, Might depend on localization
            */
            var m = null;
            $scope.months = [];
            for (mctr = 0; mctr < 12; mctr++) {
                m = new Date(2017, mctr, 1);
                $scope.months.push({ id: mctr, name: getLocalizationVal($filter('date')(m, 'MMMM')) });
            }
            /**
             * Generate Year Selection
            */
            var y = $scope.activeYear, yctr = 0;
            $scope.years = [];
            for (yctr = y - 10; yctr < y + 10; yctr++) {
                $scope.years.push({ id: yctr, name: getLocalizationVal(yctr) })
            }

            /**
             * get the templates to use 
            */
            for (var tmplKey in SELECTION_TEMPLATES) {
                if (SELECTION_TEMPLATES.hasOwnProperty(tmplKey)) {
                    //check if we have disable templates property 
                    if ($scope.disableTemplates != null && $scope.disableTemplates != '') {
                        //if key is not exist in disableTemplates property add it
                        if ($scope.disableTemplates.indexOf(tmplKey) < 0) {
                            $scope.selectionTemplate[tmplKey] = SELECTION_TEMPLATES[tmplKey];
                        }
                    } else {
                        $scope.selectionTemplate[tmplKey] = SELECTION_TEMPLATES[tmplKey];
                    }

                }
            }


        }

        function selectCustomRange(tmpltKey, tmpltObj) {
            $scope.dateStart = tmpltObj.dateStart;
            $scope.dateEnd = tmpltObj.dateEnd;
            $scope.selectedTemplate = tmpltKey;
            $scope.selectedTemplateName = $scope.selectedDateText();
        }
        function getLocalizationVal(val) {
            var ret = null;
            if ($scope.localizationMap != null && $scope.localizationMap[val] != null) {
                ret = $scope.localizationMap[val];
            } else {
                ret = val;
            }
            return ret;
        }

        function getFirstDayOfWeek() {
            if ([undefined, null, '', NaN].indexOf($scope.firstDayOfWeek) !== -1 || [0, 1, 2, 3, 4, 5, 6].indexOf($scope.firstDayOfWeek) === -1) {
                return START_OF_WEEK;
            }
            return $scope.firstDayOfWeek;
        }
        /**
         * Fill the Calendar Dates
         */
        function fillDateGrid(currentDate) {

            var dates = [],
                monthStartDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1),
                monthEndDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0),
                firstDay = getFirstDayOfWeek(),
                ctr, day;

            for (ctr = 1; ctr <= monthEndDate.getDate(); ctr++) {
                dates.push(new Date(currentDate.getFullYear(), currentDate.getMonth(), ctr));
            }

            day = dates[0].getDay();
            ctr = 0;
            while (day !== firstDay) {
                dates.unshift(new Date(currentDate.getFullYear(), currentDate.getMonth(), ctr));
                day = day <= 0 ? 6 : day - 1;
                ctr--;
            }

            day = (dates[dates.length - 1].getDay() + 1) % 7;
            ctr = 1;
            while (day !== firstDay) {
                dates.push(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, ctr));
                day = (day + 1) % 7;
                ctr++
            }
            return dates;
        }

        /**
         * Diff 2 Dates by Day Differences
         * date1 < date2 return positive integer
         * date1 = date2 return 0
         * date1 > date2 return negative integer
         */
        function getDateDiff(date1, date2) {
            if (!date1 || !date2) return;
            var _d1 = new Date(date1.getFullYear(), date1.getMonth(), date1.getDate()),
                _d2 = new Date(date2.getFullYear(), date2.getMonth(), date2.getDate());
            return _d2 - _d1;
        }

        /**
         * return Day Name in a week
         */
        function getDayName(day) {
            var weekday = new Array(7), div = getFirstDayOfWeek();
            weekday[0] = "Sun";
            weekday[1] = "Mon";
            weekday[2] = "Tue";
            weekday[3] = "Wed";
            weekday[4] = "Thu";
            weekday[5] = "Fri";
            weekday[6] = "Sat";
            return weekday[day + div % 7];
        }

        /**
         * Events
         */

        function inCurrentMonth(date, isSecondMonth) {
            return !isSecondMonth ?
                date.getMonth() === $scope.activeMonth && date.getFullYear() === $scope.activeYear :
                date.getMonth() === $scope.activeMonth2 && date.getFullYear() === $scope.activeYear2;
        }

        function isInMaxRange(date) {
            if (!$scope.dateStart) return true;
            if (getDateDiff($scope.dateStart, $scope.dateEnd) !== 0) return true;
            var diff = getDateDiff($scope.dateStart, date);
            return ($scope.maxRange && Math.abs(Math.ceil(diff / (1000 * 3600 * 24))) + 1 <= $scope.maxRange || !$scope.maxRange);
        }

        function handleClickDate($event, date) {
            var changed = false;
            var shouldConfirm = false;
            if (getDateDiff($scope.dateStart, $scope.dateEnd) === 0) {
                if (getDateDiff($scope.dateStart, date) === 0) {
                    shouldConfirm = true;
                    changed = true;
                } else if (!$scope.isDisabledDate || !$scope.isDisabledDate({ $date: date })) {
                    var diff = getDateDiff($scope.dateStart, date);
                    if (diff > 0) {
                        // Check if maxRange
                        if ($scope.maxRange && Math.abs(Math.ceil(diff / (1000 * 3600 * 24))) + 1 <= $scope.maxRange || !$scope.maxRange) {
                            $scope.dateEnd = date;
                            shouldConfirm = true;
                            changed = true;
                        }
                    } else {
                        // Check if maxRange
                        if ($scope.maxRange && Math.abs(Math.ceil(diff / (1000 * 3600 * 24))) + 1 <= $scope.maxRange || !$scope.maxRange) {
                            $scope.dateStart = date;
                            shouldConfirm = true;
                            changed = true;
                        }
                    }
                }
            } else {
                if (!$scope.isDisabledDate || !$scope.isDisabledDate({ $date: date })) {
                    $scope.dateStart = date;
                    $scope.dateEnd = date;
                    changed = true;
                }
            }
            if (changed) {
                $scope.selectedTemplate = false;
                $scope.selectedTemplateName = $scope.selectedDateText();
            }
            return shouldConfirm;
        }

        function inSelectedDateRange(date) {
            return $scope.dateStart && $scope.dateEnd
                ? getDateDiff($scope.dateStart, date) >= 0 && 0 <= getDateDiff(date, $scope.dateEnd)
                : false;
        }

        function updateActiveDate(isSecondMonth) {
            var d = new Date($scope.activeYear, $scope.activeMonth, 1),
                d2 = new Date($scope.activeYear2, $scope.activeMonth2, 1);
            if (isSecondMonth) {
                d = new Date($scope.activeYear2, $scope.activeMonth2 - 1, 1);
                $scope.activeYear = d.getFullYear();
                $scope.activeMonth = d.getMonth();
            } else {
                d2 = new Date($scope.activeYear, $scope.activeMonth + 1, 1);
                $scope.activeYear2 = d2.getFullYear();
                $scope.activeMonth2 = d2.getMonth();
            }
            $scope.focusToDate(d);
        }

        function handleClickNextMonth($event) {
            var d = new Date($scope.activeDate.getFullYear(), $scope.activeDate.getMonth() + 1, 1);
            $scope.focusToDate(d);
        }

        function handleClickPrevMonth($event) {
            var d = new Date($scope.activeDate.getFullYear(), $scope.activeDate.getMonth() - 1, 1);
            $scope.focusToDate(d);
        }

        function handleClickSelectToday() {
            var d = new Date(), d1 = new Date(d.getFullYear(), d.getMonth(), d.getDate());

            $scope.dateStart = d1;
            $scope.dateEnd = d1;
            $scope.selectedTemplate = 'TD';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d);
        }

        function handleClickSelectYesterday() {
            var d = new Date(), d1 = new Date(d.getFullYear(), d.getMonth(), d.getDate() - 1);

            $scope.dateStart = d1;
            $scope.dateEnd = d1;
            $scope.selectedTemplate = 'YD';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d);
        }


        function handleClickSelectThisWeek() {
            var p = new Date(),
                d = new Date(p.getFullYear(), p.getMonth(), p.getDate()),
                d1 = new Date(d.getFullYear(), d.getMonth(), d.getDate() - (d.getDay() - getFirstDayOfWeek())),
                d2 = new Date(d.getFullYear(), d.getMonth(), d.getDate() + (6 - d.getDay() + getFirstDayOfWeek()));

            $scope.dateStart = d1;
            $scope.dateEnd = d2;
            $scope.selectedTemplate = 'TW';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d);
        }

        function handleClickSelectLastWeek() {
            var p = new Date(),
                d = new Date(p.getFullYear(), p.getMonth(), p.getDate() - 7),
                d1 = new Date(d.getFullYear(), d.getMonth(), d.getDate() - (d.getDay() - getFirstDayOfWeek())),
                d2 = new Date(d.getFullYear(), d.getMonth(), d.getDate() + (6 - d.getDay() + getFirstDayOfWeek()));

            $scope.dateStart = d1;
            $scope.dateEnd = d2;
            $scope.selectedTemplate = 'LW';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d);
        }


        function handleClickSelectThisMonth() {
            var d = new Date(),
                d1 = new Date(d.getFullYear(), d.getMonth(), 1),
                d2 = new Date(d.getFullYear(), d.getMonth() + 1, 0);

            $scope.dateStart = d1;
            $scope.dateEnd = d2;
            $scope.selectedTemplate = 'TM';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d);
        }

        function handleClickSelectLastMonth() {
            var p = new Date(),
                d = new Date(p.getFullYear(), p.getMonth(), 0),
                d1 = new Date(d.getFullYear(), d.getMonth(), 1),
                d2 = new Date(d.getFullYear(), d.getMonth() + 1, 0);

            $scope.dateStart = d1;
            $scope.dateEnd = d2;
            $scope.selectedTemplate = 'LM';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d);
        }

        function handleClickSelectThisYear() {
            var d = new Date(),
                d1 = new Date(d.getFullYear(), 0, 1),
                d2 = new Date(d.getFullYear(), 11, 31);

            $scope.dateStart = d1;
            $scope.dateEnd = d2;
            $scope.selectedTemplate = 'TY';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d1);
        }

        function handleClickSelectLastYear() {
            var d = new Date(),
                d1 = new Date(d.getFullYear() - 1, 0, 1),
                d2 = new Date(d.getFullYear() - 1, 11, 31);

            $scope.dateStart = d1;
            $scope.dateEnd = d2;
            $scope.selectedTemplate = 'LY';
            $scope.selectedTemplateName = $scope.selectedDateText();
            //$scope.focusToDate(d1);
        }

        function isSelectedStartDate(date) {
            return getDateDiff($scope.dateStart, date) === 0;
        }

        function isSelectedEndDate(date) {
            return getDateDiff($scope.dateEnd, date) === 0;
        }

        function isToday(date) {
            return getDateDiff(date, new Date()) === 0;
        }

        function selectedDateText() {
            if ($scope.format && typeof $scope.format === 'function') {
                return $scope.format($scope.dateStart, $scope.dateEnd, $scope.selectedTemplate, $scope.selectedTemplateName);
            } else if (!$scope.dateStart || !$scope.dateEnd) {
                return '';
            } else if (!$scope.selectedTemplate) {
                if (getDateDiff($scope.dateStart, $scope.dateEnd) === 0) {
                    return $filter('date')($scope.dateStart, 'dd MMM yyyy');
                } else {
                    return $filter('date')(
                        $scope.dateStart,
                        'dd' + ($scope.dateStart.getMonth() !== $scope.dateEnd.getMonth() || $scope.dateStart.getFullYear() !== $scope.dateEnd.getFullYear() ? ' MMM' : '') + ($scope.dateStart.getFullYear() !== $scope.dateEnd.getFullYear() ? ' yyyy' : '')
                    ) + ' - ' +
                        $filter('date')(
                            $scope.dateEnd,
                            'dd MMM yyyy'
                        );
                }
            } else if (SELECTION_TEMPLATES_CUSTOM != null && SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate] != null) {
                return SELECTION_TEMPLATES_CUSTOM[$scope.selectedTemplate].name;
            } else {
                return SELECTION_TEMPLATES[$scope.selectedTemplate];
            }
        }

        function focusToDate(d) {
            var d2 = new Date(d.getFullYear(), d.getMonth() + 1, 1);
            $scope.activeDate = d;
            $scope.activeMonth = d.getMonth();
            $scope.activeYear = d.getFullYear();

            $scope.activeDate2 = d2;
            $scope.activeMonth2 = d2.getMonth();
            $scope.activeYear2 = d2.getFullYear();

            $scope.dates = fillDateGrid(d);
            $scope.dates2 = fillDateGrid(d2);
        }
    }

    function mdDateRangeDirective() {
        return {
            scope: {
                ngModel: '=ngModel',
                autoConfirm: '=autoConfirm',
                ngDisabled: '=ngDisabled',
                showTemplate: '=',
                placeholder: '@',
                isDisabledDate: '&',
                localizationMap: '=?',
                customTemplates: '=?',
                disableTemplates: '@',
                mdOnSelect: '&',
                onePanel: '=?',
                format: '=?',
                maxRange: '=?',
                firstDayOfWeek: '=?'
            },
            template: ['<md-menu ng-disabled="ngDisabled">',
                '<span class="md-select-value" ng-click="!ngDisabled && (($mdMenu && $mdMenu.open) ? $mdMenu.open($event) : $mdOpenMenu($event))">',
                '  <span>{{ngModel.selectedTemplateName || placeholder}}</span>',
                '  <span class="md-select-icon" aria-hidden="true"></span>',
                '</span>',
                '<md-menu-content class="md-custom-menu-content" style="max-height: none!important; height: auto!important; padding: 0!important;">',
                '    <span style="text-align: left; padding: 12px 20px 0 20px" disabled>{{ngModel.selectedTemplateName || placeholder}}</span>',
                '    <md-date-range-picker first-day-of-week="firstDayOfWeek" ',
                '     md-on-select="autoConfirm && ok($dates)" ',
                '     date-start="ngModel.dateStart" ',
                '     date-end="ngModel.dateEnd" ',
                '     selected-template="ngModel.selectedTemplate" ',
                '     show-template="showTemplate" ',
                '     localization-map="localizationMap" ',
                '     custom-templates="customTemplates" ',
                '     disable-templates="{{disableTemplates}}" ',
                '     is-disabled-date="isDisabledDate({ $date: $date })" ',
                '     max-range="maxRange" ',
                '     one-panel="onePanel" ',
                '     format="format" ',
                '     selected-template-name="ngModel.selectedTemplateName"></md-date-range-picker>',
                '<p ng-if="!autoConfirm" layout="row" layout-align="end center">',
                '<md-button ng-if="ngModel.showClear" class="md-raised" ng-click="clear()">{{getLocalizationVal("Clear")}}</md-button>',
                '<md-button class="md-raised md-primary" ng-click="ok()">{{getLocalizationVal("Ok")}}</md-button>',
                '</p>',
                '</md-menu-content>',
                '</md-menu>'].join(''),
            controller: ['$scope', '$mdMenu', function ($scope, $mdMenu) {
                $scope.ok = function ok($dates) {
                    $scope.mdOnSelect({ $dates: $dates });
                    $mdMenu.hide();
                }
                $scope.clear = function clear() {
                    $scope.ngModel.selectedTemplateName = '';
                    $scope.ngModel.selectedTemplate = null;
                    $scope.ngModel.dateStart = null;
                    $scope.ngModel.dateEnd = null;
                }
                $scope.getLocalizationVal = function getLocalizationVal(val) {
                    var ret = null;
                    if ($scope.ngModel && $scope.ngModel.localizationMap != null && $scope.ngModel.localizationMap[val] != null) {
                        ret = $scope.ngModel.localizationMap[val];
                    } else {
                        ret = val;
                    }
                    return ret;
                }
            }]
        };
    }

    mdDateRangePickerService.$inject = ['$mdDialog'];
    function mdDateRangePickerService($mdDialog) {
        var service = this;

        service.show = show;
        service.getSelectedDate = getSelectedDate;

        /**
         * @description returns all seleced date based on mmodel, filters and max range
         * @param {*} model 
         * @param {*} isDisabledDateCallback 
         * @param {*} maxRange 
         */
        function getSelectedDate(dateStart, dateEnd, isDisabledDateCallback, maxRange) {
            var dates = [];
            var limit = dateEnd.getTime();
            var date = dateStart;
            var ctr = 0;
            var y = dateStart.getFullYear();
            var m = dateStart.getMonth();
            var d = dateStart.getDate();
            while (date.getTime() <= limit) {
                if (isDisabledDateCallback && !isDisabledDateCallback({ $date: date })) {
                    dates.push(date);
                }
                ctr++;
                date = new Date(y, m, d + ctr);
                if (ctr > (maxRange || 10000)) break; // break on 10,000
            }
            return dates;
        }

        function show(config) {
            return $mdDialog.show({
                locals: {
                    mdDateRangePickerServiceModel: angular.copy(config.model),
                    mdDateRangePickerServiceConfig: angular.copy(config),
                },
                controller: ['$scope', 'mdDateRangePickerServiceModel', 'mdDateRangePickerServiceConfig', function ($scope, mdDateRangePickerServiceModel, mdDateRangePickerServiceConfig) {
                    $scope.model = mdDateRangePickerServiceModel || {};
                    $scope.config = mdDateRangePickerServiceConfig || {};
                    $scope.model.selectedTemplateName = $scope.model.selectedTemplateName || '';
                    $scope.ok = function () {
                        $scope.model.dateStart && $scope.model.dateStart.setHours(0, 0, 0, 0);
                        $scope.model.dateEnd && $scope.model.dateEnd.setHours(23, 59, 59, 999);
                        $mdDialog.hide($scope.model);
                    }
                    $scope.cancel = function () {
                        $mdDialog.cancel();
                    }
                    $scope.clear = function clear() {
                        $scope.model.selectedTemplateName = '';
                        $scope.model.selectedTemplate = null;
                        $scope.model.dateStart = null;
                        $scope.model.dateEnd = null;
                    }
                    $scope.handleOnSelect = function ($dates) {
                        if (typeof $scope.config.mdOnSelect === 'function') {
                            $scope.config.mdOnSelect($dates);
                        }
                        if ($scope.config.autoConfirm) {
                            $scope.ok();
                        }
                    }
                    $scope.getLocalizationVal = function getLocalizationVal(val) {
                        var ret = null;
                        if ($scope.model && $scope.model.localizationMap != null && $scope.model.localizationMap[val] != null) {
                            ret = $scope.model.localizationMap[val];
                        } else {
                            ret = val;
                        }
                        return ret;
                    }
                    if ($scope.model.customTemplates) console.warn('model.customTemplates will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({customTemplates}) instead');
                    if ($scope.model.localizationMap) console.warn('model.localizationMap will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({localizationMap}) instead');
                    if ($scope.model.firstDayOfWeek) console.warn('model.firstDayOfWeek will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({firstDayOfWeek}) instead');
                    if ($scope.model.showTemplate) console.warn('model.showTemplate will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({showTemplate}) instead');
                    if ($scope.model.maxRange) console.warn('model.maxRange will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({maxRange}) instead');
                    if ($scope.model.onePanel) console.warn('model.onePanel will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({onePanel}) instead');
                    if ($scope.model.isDisabledDate) console.warn('model.isDisabledDate({ $date: $date }) will be removed from model on next rlease, please use root config e.g. $mdDateRangePicker.show({isDisabledDate:($date)=>{}}) instead');

                }],
                template: ['<md-dialog aria-label="Date Range Picker">',
                    '<md-toolbar class="md-primary" layout="row" layout-align="start center">',
                    '<md-button aria-label="Date Range Picker" class="md-icon-button" aria-hidden="true" ng-disabled="true">',
                    '<md-icon md-svg-icon="data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjRkZGRkZGIiBoZWlnaHQ9IjI0IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICAgIDxwYXRoIGQ9Ik05IDExSDd2Mmgydi0yem00IDBoLTJ2Mmgydi0yem00IDBoLTJ2Mmgydi0yem0yLTdoLTFWMmgtMnYySDhWMkg2djJINWMtMS4xMSAwLTEuOTkuOS0xLjk5IDJMMyAyMGMwIDEuMS44OSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMlY2YzAtMS4xLS45LTItMi0yem0wIDE2SDVWOWgxNHYxMXoiLz4KICAgIDxwYXRoIGQ9Ik0wIDBoMjR2MjRIMHoiIGZpbGw9Im5vbmUiLz4KPC9zdmc+"></md-icon>',
                    '</md-button>',
                    '<span md-truncate class="md-toolbar-tools">{{model.selectedTemplateName}}</span>',
                    '</md-toolbar>',
                    '<md-dialog-content>',
                    '<md-date-range-picker ',
                    'date-start="model.dateStart" ',
                    'date-end="model.dateEnd" ',
                    'show-template="config.showTemplate || model.showTemplate" ',
                    'selected-template="model.selectedTemplate" ',
                    'selected-template-name="model.selectedTemplateName" ',
                    'first-day-of-week="config.firstDayOfWeek || model.firstDayOfWeek" ',
                    'localization-map="config.localizationMap || model.localizationMap" ',
                    'custom-templates="config.customTemplates || model.customTemplates" ',
                    'format="config.format" ',
                    'disable-templates="{{model.disableTemplates}}" ',
                    'md-on-select="handleOnSelect($dates)" ',
                    'is-disabled-date="config.isDisabledDate ? config.isDisabledDate($date) : model.isDisabledDate({ $date: $date })" ',
                    'max-range="config.maxRange || model.maxRange" ',
                    'one-panel="config.onePanel || model.onePanel" ',
                    '>',
                    '</md-date-range-picker>',
                    '</md-dialog-content>',
                    '<md-dialog-actions layout="row" layout-align="end center">',
                    '<md-button ng-click="cancel()">{{getLocalizationVal("Cancel")}}</md-button>',
                    '<md-button ng-if="!config.autoConfirm" class="md-raised" ng-click="clear()">{{getLocalizationVal("Clear")}}</md-button>',
                    '<md-button ng-if="!config.autoConfirm" class="md-raised md-primary" ng-click="ok()">{{getLocalizationVal("Ok")}}</md-button>',
                    '</md-dialog-actions>',
                    '</md-dialog>'].join(''),
                parent: angular.element(document.body),
                multiple: config.multiple,
                targetEvent: config.targetEvent || document.body,
                clickOutsideToClose: true,
                fullscreen: config.model.fullscreen
            });
        }
    }

}(window, angular));
