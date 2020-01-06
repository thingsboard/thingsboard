/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import './timeseries-table-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import timeseriesTableWidgetTemplate from './timeseries-table-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import tinycolor from 'tinycolor2';
import cssjs from '../../../vendor/css.js/css';

export default angular.module('thingsboard.widgets.timeseriesTableWidget', [])
    .directive('tbTimeseriesTableWidget', TimeseriesTableWidget)
    .name;

/*@ngInject*/
function TimeseriesTableWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            tableId: '=',
            ctx: '='
        },
        controller: TimeseriesTableWidgetController,
        controllerAs: 'vm',
        templateUrl: timeseriesTableWidgetTemplate
    };
}

/*@ngInject*/
function TimeseriesTableWidgetController($element, $scope, $filter, $timeout, types) {
    var vm = this;
    let dateFormatFilter;

    vm.sources = [];
    vm.sourceIndex = 0;
    vm.defaultPageSize = 10;
    vm.defaultSortOrder = '-0';
    vm.query = {
        "search": null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onRowClick = onRowClick;
    vm.onActionButtonClick = onActionButtonClick;
    vm.actionCellDescriptors = [];

    function enterFilterMode () {
        vm.query.search = '';
        vm.ctx.hideTitlePanel = true;
        $timeout(()=>{
            angular.element(vm.ctx.$container).find('.searchInput').focus();
        })
    }

    function exitFilterMode () {
        vm.query.search = null;
        vm.ctx.hideTitlePanel = false;
    }

    vm.searchAction = {
        name: 'action.search',
        show: true,
        onAction: function() {
            vm.enterFilterMode();
        },
        icon: 'search'
    };

    $scope.$watch('vm.ctx', function() {
       if (vm.ctx) {
           vm.settings = vm.ctx.settings;
           vm.widgetConfig = vm.ctx.widgetConfig;
           vm.data = vm.ctx.data;
           vm.datasources = vm.ctx.datasources;
           initialize();
       }
    });

    function initialize() {
        vm.ctx.widgetActions = [ vm.searchAction ];
        vm.actionCellDescriptors = vm.ctx.actionsApi.getActionDescriptors('actionCellButton');
        vm.showTimestamp = vm.settings.showTimestamp !== false;
        dateFormatFilter = (vm.settings.showMilliseconds !== true) ? 'yyyy-MM-dd HH:mm:ss' :  'yyyy-MM-dd HH:mm:ss.sss';
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

        vm.displayPagination = angular.isDefined(vm.settings.displayPagination) ? vm.settings.displayPagination : true;

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
        updateDatasources();
    }

    $scope.$on('timeseries-table-data-updated', function(event, tableId) {
        if (vm.tableId == tableId) {
            dataUpdated();
        }
    });

    function dataUpdated() {
        for (var s=0; s < vm.sources.length; s++) {
            var source = vm.sources[s];
            source.rawData = vm.data.slice(source.keyStartIndex, source.keyEndIndex);
        }
        updateSourceData(vm.sources[vm.sourceIndex]);
        $scope.$digest();
    }

    vm.onPaginate = function(source) {
        updatePage(source);
    }

    vm.onReorder = function(source) {
        reorder(source);
        updatePage(source);
    }

    function onRowClick($event, row) {
        if ($event) {
            $event.stopPropagation();
        }
        var descriptors = vm.ctx.actionsApi.getActionDescriptors('rowClick');
        if (descriptors.length) {
            var entityId = vm.ctx.activeEntityInfo.entityId;
            var entityName = vm.ctx.activeEntityInfo.entityName;
            var entityLabel = vm.ctx.activeEntityInfo.entityLabel;
            vm.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, row, entityLabel);
        }
    }

    function onActionButtonClick($event, row, actionDescriptor) {
        if ($event) {
            $event.stopPropagation();
        }
        var entityId = vm.ctx.activeEntityInfo.entityId;
        var entityName = vm.ctx.activeEntityInfo.entityName;
        var entityLabel = vm.ctx.activeEntityInfo.entityLabel;
        vm.ctx.actionsApi.handleWidgetAction($event, actionDescriptor, entityId, entityName, row, entityLabel);
    }


    vm.cellStyle = function(source, index, value) {
        var style = {};
        if (index > 0) {
            var styleInfo = source.ts.stylesInfo[index-1];
            if (styleInfo.useCellStyleFunction && styleInfo.cellStyleFunction) {
                try {
                    style = styleInfo.cellStyleFunction(value);
                } catch (e) {
                    style = {};
                }
            }
        }
        return style;
    }

    vm.cellContent = function(source, index, row, value) {
        if (index === 0) {
            return $filter('date')(value, dateFormatFilter);
        } else {
            var strContent = '';
            if (angular.isDefined(value)) {
                strContent = ''+value;
            }
            var content = strContent;
            var contentInfo = source.ts.contentsInfo[index-1];
            if (contentInfo.useCellContentFunction && contentInfo.cellContentFunction) {
                try {
                    var rowData = source.ts.rowDataTemplate;
                    rowData['Timestamp'] = row[0];
                    for (var h=0; h < source.ts.header.length; h++) {
                        var headerInfo = source.ts.header[h];
                        rowData[headerInfo.dataKey.name] = row[headerInfo.index];
                    }
                    content = contentInfo.cellContentFunction(value, rowData, $filter);
                } catch (e) {
                    content = strContent;
                }
            } else {
                var decimals = (contentInfo.decimals || contentInfo.decimals === 0) ? contentInfo.decimals : vm.widgetConfig.decimals;
                var units = contentInfo.units || vm.widgetConfig.units;
                content = vm.ctx.utils.formatValue(value, decimals, units, true);
            }
            return content;
        }
    }

    $scope.$watch('vm.sourceIndex', function(newIndex, oldIndex) {
        if (newIndex != oldIndex) {
            updateSourceData(vm.sources[vm.sourceIndex]);
            updateActiveEntityInfo();
        }
    });

    function updateActiveEntityInfo() {
        var source = vm.sources[vm.sourceIndex];
        var activeEntityInfo = null;
        if (source) {
            var datasource = source.datasource;
            if (datasource.type === types.datasourceType.entity &&
                datasource.entityType && datasource.entityId) {
                activeEntityInfo = {
                    entityId: {
                        entityType: datasource.entityType,
                        id: datasource.entityId
                    },
                    entityName: datasource.entityName
                };
            }
        }
        vm.ctx.activeEntityInfo = activeEntityInfo;
    }

    function updateDatasources() {
        vm.sources = [];
        vm.sourceIndex = 0;
        var keyOffset = 0;
        if (vm.datasources) {
            for (var ds = 0; ds < vm.datasources.length; ds++) {
                var source = {};
                var datasource = vm.datasources[ds];
                source.keyStartIndex = keyOffset;
                keyOffset += datasource.dataKeys.length;
                source.keyEndIndex = keyOffset;
                source.datasource = datasource;
                source.data = [];
                source.rawData = [];
                source.query = {
                    limit: vm.settings.defaultPageSize || 10,
                    page: 1,
                    order: '-0'
                }
                source.ts = {
                    header: [],
                    count: 0,
                    data: [],
                    stylesInfo: [],
                    contentsInfo: [],
                    rowDataTemplate: {}
                }
                source.ts.rowDataTemplate['Timestamp'] = null;
                for (var a = 0; a < datasource.dataKeys.length; a++ ) {
                    var dataKey = datasource.dataKeys[a];
                    var keySettings = dataKey.settings;
                    source.ts.header.push({
                        index: a+1,
                        dataKey: dataKey
                    });
                    source.ts.rowDataTemplate[dataKey.label] = null;

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

                    source.ts.stylesInfo.push({
                        useCellStyleFunction: useCellStyleFunction,
                        cellStyleFunction: cellStyleFunction
                    });

                    var cellContentFunction = null;
                    var useCellContentFunction = false;

                    if (keySettings.useCellContentFunction === true) {
                        if (angular.isDefined(keySettings.cellContentFunction) && keySettings.cellContentFunction.length > 0) {
                            try {
                                cellContentFunction = new Function('value, rowData, filter', keySettings.cellContentFunction);
                                useCellContentFunction = true;
                            } catch (e) {
                                cellContentFunction = null;
                                useCellContentFunction = false;
                            }
                        }
                    }

                    source.ts.contentsInfo.push({
                        useCellContentFunction: useCellContentFunction,
                        cellContentFunction: cellContentFunction,
                        units: dataKey.units,
                        decimals: dataKey.decimals
                    });

                }
                vm.sources.push(source);
            }
        }
        updateActiveEntityInfo();
    }

    function updatePage(source) {
        var startIndex = source.query.limit * (source.query.page - 1);
        source.ts.data = source.data.slice(startIndex, startIndex + source.query.limit);
    }

    function reorder(source) {
        let searchRegExp = new RegExp(vm.query.search);

        source.data = $filter('orderBy')(source.data, source.query.order);
        if (vm.query.search !== null) {
            source.data = source.data.filter(function(item){
                for (let i = 0; i < item.length; i++) {
                    if (vm.showTimestamp) {
                        if (i === 0) {
                            if (searchRegExp.test($filter('date')(item[i], dateFormatFilter))) {
                                return true;
                            }
                        } else {
                            if (searchRegExp.test(item[i])) {
                                return true;
                            }
                        }
                    } else {
                        if (searchRegExp.test(item[i])) {
                            return true;
                        }
                    }
                }
            });
        }
    }

    function convertData(data) {
        var rowsMap = {};
        for (var d = 0; d < data.length; d++) {
            var columnData = data[d].data;
            for (var i = 0; i < columnData.length; i++) {
                var cellData = columnData[i];
                var timestamp = cellData[0];
                var row = rowsMap[timestamp];
                if (!row) {
                    row = [];
                    row[0] = timestamp;
                    for (var c = 0; c < data.length; c++) {
                        row[c+1] = undefined;
                    }
                    rowsMap[timestamp] = row;
                }
                row[d+1] = cellData[1];
            }
        }
        var rows = [];
        for (var t in rowsMap) {
            if (vm.settings.hideEmptyLines)
			{
                var hideLine = true;
                for (var _c = 0; (_c < data.length) && hideLine; _c++) {
                    if (rowsMap[t][_c+1])
                        hideLine = false;
                }
                if (!hideLine)
                   rows.push(rowsMap[t]);
            }
            else
                rows.push(rowsMap[t]);
        }
        return rows;
    }

    function updateSourceData(source) {
        source.data = convertData(source.rawData);
        source.ts.count = source.data.length;
        reorder(source);
        updatePage(source);
    }

}
