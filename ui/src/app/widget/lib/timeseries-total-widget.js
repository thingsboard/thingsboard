/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import './timeseries-total-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import timeseriesTotalWidgetTemplate from './timeseries-total-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.timeseriesTotalWidget', [])
    .directive('tbTimeseriesTotalWidget', TimeseriesTotalWidget)
    .name;

/*@ngInject*/
function TimeseriesTotalWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            tableId: '=',
            ctx: '='
        },
        controller: TimeseriesTotalWidgetController,
        controllerAs: 'vm',
        templateUrl: timeseriesTotalWidgetTemplate
    };
}

/*@ngInject*/
function TimeseriesTotalWidgetController($element, $scope) {
    var vm = this;

    vm.sources = [];

    vm.periods = [
        { id: 1, name: 'Minute' },
        { id: 2, name: 'Hour' },
        { id: 3, name: 'Day' },
        { id: 4, name: 'Week' }
    ];
    vm.selectedPeriod = { id: 1, name: 'Minute' };

    vm.modes = [
        { id: 1, name: 'Average' },
        { id: 2, name: 'Sum' },
        { id: 3, name: 'Max' },
        { id: 4, name: 'Min' }
    ]
    vm.selectedMode = { id: 1, name: 'Average' };
    vm.value = '';

    vm.changePeriod = changePeriod;
    vm.changeMode = changeMode;

    $scope.$watch('vm.ctx', function() {
       if (vm.ctx) {
           vm.data = vm.ctx.data;
           initialize();
       }
    });

    function initialize() {
        updateDatasources();
    }

    $scope.$on('timeseries-total-data-updated', function(event, tableId) {
        if (vm.tableId == tableId) {
            dataUpdated();
        }
    });

    function dataUpdated() {
        for (var ds = 0; ds < vm.data.length; ds++) {
            var source = vm.sources[ds];
            source.source = vm.data[ds].data;
            updateSourceData(source);
        }
        $scope.$digest();
    }
    
    function updateDatasources() {
        vm.sources = [];
        if (vm.data) {
            var sources = [];
            for (var ds = 0; ds < vm.data.length; ds++) {
                var keyName = vm.data[ds].dataKey.label;

                sources.push({
                    keyName: keyName,
                    source: []
                });
            }
            vm.sources = sources;
        }
    }

    function convertData(data) {
        var period = 0;
        if (vm.selectedPeriod.id === 1) {
            period = 60;
        } else if (vm.selectedPeriod.id === 2) {
            period = 60 * 60;
        } else if (vm.selectedPeriod.id === 3) {
            period = 60 * 60 * 24;
        } else if (vm.selectedPeriod.id === 4) {
            period = 60 * 60 * 24 * 7;
        }

        var rowData = sortDescending(data.source);
        var sum = null, max = null, min = null;
        
        for (var d = 0; d < Math.min(period, rowData.length); d++) {
            sum = sum + rowData[d][1] || 0;
            if (!(max && max > rowData[d][1])) {
                max = rowData[d][1];
            }
            if (!(min && min < rowData[d][1])) {
                min = rowData[d][1];
            }
        }

        if (vm.selectedMode.id === 1) {
            data.value = sum / (Math.min(period, rowData.length)) || 0;
        } else if (vm.selectedMode.id === 2) {
            data.value = sum;
        } else if (vm.selectedMode.id === 3) {
            data.value = max;
        } else if (vm.selectedMode.id === 4) {
            data.value = min;
        }
        data.value = parseInt(data.value);
    }

    function sortDescending(data) {
        return data.sort(function(a, b) { return b[0] - a[0] });
    }

    function updateSourceData(source) {
        convertData(source);
    }

}