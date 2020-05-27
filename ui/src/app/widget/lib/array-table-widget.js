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
import './array-table-widget.scss';
import './display-columns-panel.scss';

/* eslint-disable import/no-unresolved, import/default */

import arrayTableWidgetTemplate from './array-table-widget.tpl.html';
//import entityDetailsDialogTemplate from './entitiy-details-dialog.tpl.html';
import displayColumnsPanelTemplate from './display-columns-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import tinycolor from 'tinycolor2';
import cssjs from '../../../vendor/css.js/css';

export default angular.module('thingsboard.widgets.arrayTableWidget', [])
    .directive('tbArrayTableWidget', ArrayTableWidget)
    .name;

/*@ngInject*/
function ArrayTableWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            tableId: '=',
            ctx: '='
        },
        controller: ArrayTableWidgetController,
        controllerAs: 'vm',
        templateUrl: arrayTableWidgetTemplate
    };
}

/*@ngInject*/
function ArrayTableWidgetController($element, $scope, $filter, $mdMedia, $mdPanel, $document, $translate, $timeout, utils, types) {
    var vm = this;

    vm.stylesInfo = {};
    vm.contentsInfo = {};
    vm.columnWidth = {};

    vm.showData = true;
    vm.hasData = false;

    vm.rows = [];
    vm.rowsCount = 0;

    vm.datasources = null;
    vm.allEntities = null;
    vm.allRows = null;

    vm.currentRow = null;

    vm.displayEntityName = true;
    vm.entityNameColumnTitle = '';
    vm.displayEntityLabel = false;
    vm.entityLabelColumnTitle = '';
    vm.displayEntityType = true;
    vm.actionCellDescriptors = [];
    vm.actionRowDescriptors = [];
    vm.displayPagination = true;
    vm.defaultPageSize = 10;
    vm.defaultSortOrder = 'entityName';

    vm.query = {
        order: vm.defaultSortOrder,
        limit: vm.defaultPageSize,
        page: 1,
        search: null
    };

    vm.searchAction = {
        name: 'action.search',
        show: true,
        onAction: function() {
            vm.enterFilterMode();
        },
        icon: 'search'
    };

    let columnDisplayAction = {
        name: 'entity.columns-to-display',
        show: true,
        onAction: function($event) {
            vm.editColumnsToDisplay($event);
        },
        icon: 'view_column'
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.onRowClick = onRowClick;
    vm.onActionButtonClick = onActionButtonClick;
    vm.isCurrent = isCurrent;

    vm.cellStyle = cellStyle;
    vm.cellContent = cellContent;
    vm.headerStyle = headerStyle;

    vm.editColumnsToDisplay = editColumnsToDisplay;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            initializeConfig();
            updateDatasources();
            updateRows();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateRows();
        }
    });

    $scope.$on('array-table-data-updated', function(event, tableId) {
        if (vm.tableId == tableId) {
            if (vm.subscription) {
                updateRowsData(vm.subscription.data);
                updateRows();
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
            vm.limitOptions = [vm.defaultPageSize, vm.defaultPageSize*2, vm.defaultPageSize*3];
        } else {
            vm.limitOptions = null;
        }
    });

    function initializeConfig() {

        vm.ctx.widgetActions = [ vm.searchAction, columnDisplayAction ];

        vm.actionCellDescriptors = vm.ctx.actionsApi.getActionDescriptors('actionCellButton');
        vm.actionRowDescriptors = vm.ctx.actionsApi.getActionDescriptors('rowClick');

        if (vm.settings.tableTitle && vm.settings.tableTitle.length) {
            vm.tableTitle = utils.customTranslation(vm.settings.tableTitle, vm.settings.tableTitle);
        } else {
            vm.tableTitle = $translate.instant('array-table.title');
        }

        vm.ctx.widgetTitle = vm.tableTitle;

        vm.searchAction.show = angular.isDefined(vm.settings.enableSearch) ? vm.settings.enableSearch : true;
        vm.displayEntityName = angular.isDefined(vm.settings.displayEntityName) ? vm.settings.displayEntityName : true;
        vm.displayEntityLabel = angular.isDefined(vm.settings.displayEntityLabel) ? vm.settings.displayEntityLabel : false;
        columnDisplayAction.show = angular.isDefined(vm.settings.enableSelectColumnDisplay) ? vm.settings.enableSelectColumnDisplay : true;

        if (vm.settings.entityNameColumnTitle && vm.settings.entityNameColumnTitle.length) {
            vm.entityNameColumnTitle = utils.customTranslation(vm.settings.entityNameColumnTitle, vm.settings.entityNameColumnTitle);
        } else {
            vm.entityNameColumnTitle = $translate.instant('array-table.entity-name');
        }

        if (vm.settings.entityLabelColumnTitle && vm.settings.entityLabelColumnTitle.length) {
            vm.entityLabelColumnTitle = utils.customTranslation(vm.settings.entityLabelColumnTitle, vm.settings.entityLabelColumnTitle);
        } else {
            vm.entityLabelColumnTitle = $translate.instant('entity.entity-label');
        }

        vm.displayEntityType = angular.isDefined(vm.settings.displayEntityType) ? vm.settings.displayEntityType : true;
        vm.displayPagination = angular.isDefined(vm.settings.displayPagination) ? vm.settings.displayPagination : true;

        var pageSize = vm.settings.defaultPageSize;
        if (angular.isDefined(pageSize) && angular.isNumber(pageSize) && pageSize > 0) {
            vm.defaultPageSize = pageSize;
        }

        if (vm.settings.defaultSortOrder && vm.settings.defaultSortOrder.length) {
            vm.defaultSortOrder = vm.settings.defaultSortOrder;
            if (vm.settings.defaultSortOrder.charAt(0) === "-") {
                vm.defaultSortOrder = '-"' + utils.customTranslation(vm.settings.defaultSortOrder.substring(1), vm.settings.defaultSortOrder.substring(1)) + '"';
            } else {
                vm.defaultSortOrder = '"' + utils.customTranslation(vm.settings.defaultSortOrder, vm.settings.defaultSortOrder) + '"';
            }
        }

        vm.query.order = vm.defaultSortOrder;
        vm.query.limit = vm.defaultPageSize;
        if (vm.isGtMd) {
            vm.limitOptions = [vm.defaultPageSize, vm.defaultPageSize*2, vm.defaultPageSize*3];
        } else {
            vm.limitOptions = null;
        }

        var origColor = vm.widgetConfig.color || 'rgba(0, 0, 0, 0.87)';
        var defaultColor = tinycolor(origColor);
        var mdDark = defaultColor.setAlpha(0.87).toRgbString();
        var mdDarkSecondary = defaultColor.setAlpha(0.54).toRgbString();
        var mdDarkDisabled = defaultColor.setAlpha(0.26).toRgbString();
        //var mdDarkIcon = mdDarkSecondary;
        var mdDarkDivider = defaultColor.setAlpha(0.12).toRgbString();

        //md-icon.md-default-theme, md-icon {

        var cssString = 'table.md-table th.md-column {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
            '}\n'+
            'table.md-table th.md-column.md-checkbox-column md-checkbox:not(.md-checked) .md-icon {\n'+
            'border-color: ' + mdDarkSecondary + ';\n'+
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
            'table.md-table td.md-cell.md-checkbox-cell md-checkbox:not(.md-checked) .md-icon {\n'+
            'border-color: ' + mdDarkSecondary + ';\n'+
            '}\n'+
            'table.md-table td.md-cell.tb-action-cell button.md-icon-button md-icon {\n'+
            'color: ' + mdDarkSecondary + ';\n'+
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
        var namespace = 'entities-table-' + hashCode(cssString);
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
        vm.ctx.hideTitlePanel = true;
        $timeout(()=>{
            angular.element(vm.ctx.$container).find('.searchInput').focus();
        })
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateRows();
        vm.ctx.hideTitlePanel = false;
    }

    function onReorder () {
        updateRows();
    }

    function onPaginate () {
        updateRows();
    }

    function onRowClick($event, row, column) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.currentRow != row) {
            vm.currentRow = row;
        }
        var descriptors = vm.ctx.actionsApi.getActionDescriptors('rowClick');
        if (descriptors.length) {
            var entityId, entityName, entityLabel;
            if (vm.currentRow) {
                entityId = vm.currentRow.id;
                entityName = vm.currentRow.entityName;
                entityLabel = vm.currentRow.entityLabel;
            }
            vm.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, { row: row, column: column }, entityLabel);
        }
    }

    function onActionButtonClick($event, row, actionDescriptor) {
        if ($event) {
            $event.stopPropagation();
        }
        var entityId, entityName, entityLabel;
        if (row) {
            entityId = row.id;
            entityName = row.entityName;
            entityLabel = row.entityLabel;
        }
        vm.ctx.actionsApi.handleWidgetAction($event, actionDescriptor, entityId, entityName, { row: row }, entityLabel);
    }

    function isCurrent(row) {
        return (vm.currentRow && row) && (vm.currentRow.index === row.index);
    }

    function updateRows() {
        var result = $filter('orderBy')(vm.allRows, "\"" + vm.query.order + "\"");
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        if (result != null) {
            vm.rowsCount = result.length;

            if (vm.displayPagination) {
                var startIndex = vm.query.limit * (vm.query.page - 1);
                vm.rows = result.slice(startIndex, startIndex + vm.query.limit);
            } else {
                vm.rows = result;
            }
        }
    }

    function cellStyle(entity, key) {
        var style = {};
        if (entity && key) {
            var styleInfo = vm.stylesInfo[key.label];
            var value = getEntityValue(entity, key);
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
        if (!style.width) {
            var columnWidth = vm.columnWidth[key.label];
            if(columnWidth !== "0px") {
                style.width = columnWidth;
            } else {
                style.width = "auto";
            }
        }
        return style;
    }

    function cellContent(entity, key) {
        var strContent = '';
        if (entity && key) {
            var contentInfo = vm.contentsInfo[key.label];
            var value = getEntityValue(entity, key);
            if (contentInfo.useCellContentFunction && contentInfo.cellContentFunction) {
                if (angular.isDefined(value)) {
                    strContent = '' + value;
                }
                var content = strContent;
                try {
                    content = contentInfo.cellContentFunction(value, entity, $filter);
                } catch (e) {
                    content = strContent;
                }
            } else {
                var decimals = (contentInfo.decimals || contentInfo.decimals === 0) ? contentInfo.decimals : vm.widgetConfig.decimals;
                var units = contentInfo.units || vm.widgetConfig.units;
                content = vm.ctx.utils.formatValue(value, decimals, units, true);
            }
            return content;
        } else {
            return strContent;
        }
    }


    function headerStyle(key) {
        var style = {};
        if (key) {
            var styleInfo = vm.stylesInfo[key.label];
            if (styleInfo.useHeaderStyleFunction && styleInfo.headerStyleFunction) {
                try {
                    style = styleInfo.headerStyleFunction();
                } catch (e) {
                    style = {};
                }
            } else {
                style = defaultStyle(key);
            }
        }
        if (!style.width) {
            var columnWidth = vm.columnWidth[key.label];
            if(columnWidth !== "0px") {
                style.width = columnWidth;
            } else {
                style.width = "auto";
            }
        }
        return style;
    }

    function defaultStyle(/*key, value*/) {
        return {};
    }

    const getDescendantProp = (obj, path) => (
        path.split('.').reduce((acc, part) => acc && acc[part], obj)
    );

    function getEntityValue(entity, key) {
        return getDescendantProp(entity, key.label);
    }

    function updateRowsData(data) {
        var rowIndex = 0;
        vm.allRows = [];
        if (vm.allEntities) {
            for (var i=0;i<vm.allEntities.length;i++) {
                var entity = vm.allEntities[i];
                var indexDataKey = i * vm.dataKeys.length;
                var firstDataKey = data[indexDataKey].data;
                if (firstDataKey && firstDataKey.length && firstDataKey[0].length > 1) {
                    var childRowsList = firstDataKey[0][1];
                    for (var j=0; j<childRowsList.length; j++) {
                        var childRow = angular.copy(entity);
                        for (var a=0;a<vm.dataKeys.length;a++) {
                            var dataKey = vm.dataKeys[a];
                            var index = i * vm.dataKeys.length + a;
                            var keyData = data[index].data;
                            if (keyData && keyData.length && keyData[0].length > 1) {
                                var array = keyData[0][1];
                                childRow[dataKey.label] = array[j];
                            } else {
                                childRow[dataKey.label] = '';
                            }
                        }
                        childRow['index'] = rowIndex++;
                        vm.allRows.push(childRow);
                    }
                }
            }
        }
    }

    function editColumnsToDisplay($event) {
        var element = angular.element($event.target);
        var position = $mdPanel.newPanelPosition()
            .relativeTo(element)
            .addPanelPosition($mdPanel.xPosition.ALIGN_END, $mdPanel.yPosition.BELOW);
        var config = {
            attachTo: angular.element($document[0].body),
            controller: DisplayColumnsPanelController,
            controllerAs: 'vm',
            templateUrl: displayColumnsPanelTemplate,
            panelClass: 'tb-display-columns-panel',
            position: position,
            fullscreen: false,
            locals: {
                'columns': vm.columns
            },
            openFrom: $event,
            clickOutsideToClose: true,
            escapeToClose: true,
            focusOnOpen: false
        };
        $mdPanel.open(config);
    }

    function updateDatasources() {

        vm.stylesInfo = {};
        vm.contentsInfo = {};
        vm.columnWidth = {};
        vm.dataKeys = [];
        vm.columns = [];
        vm.allEntities = [];

        var datasource;
        var dataKey;

        datasource = vm.datasources[0];

        vm.ctx.widgetTitle = utils.createLabelFromDatasource(datasource, vm.tableTitle);

        if (vm.displayEntityName) {
            vm.columns.push(
                {
                    name: 'entityName',
                    label: 'entityName',
                    title: vm.entityNameColumnTitle,
                    display: true
                }
            );
            vm.contentsInfo['entityName'] = {
                useCellContentFunction: false
            };
            vm.stylesInfo['entityName'] = {
                useCellStyleFunction: false,
                useHeaderStyleFunction: false
            };
            vm.columnWidth['entityName'] = '0px';
        }

        if (vm.displayEntityLabel) {
            vm.columns.push(
                {
                    name: 'entityLabel',
                    label: 'entityLabel',
                    title: vm.entityLabelColumnTitle,
                    display: true
                }
            );
            vm.contentsInfo['entityLabel'] = {
                useCellContentFunction: false
            };
            vm.stylesInfo['entityLabel'] = {
                useCellStyleFunction: false,
                useHeaderStyleFunction: false
            };
            vm.columnWidth['entityLabel'] = '0px';
        }

        if (vm.displayEntityType) {
            vm.columns.push(
                {
                    name: 'entityType',
                    label: 'entityType',
                    title: $translate.instant('array-table.entity-type'),
                    display: true
                }
            );
            vm.contentsInfo['entityType'] = {
                useCellContentFunction: false
            };
            vm.stylesInfo['entityType'] = {
                useCellStyleFunction: false,
                useHeaderStyleFunction: false
            };
            vm.columnWidth['entityType'] = '0px';
        }

        for (var d = 0; d < datasource.dataKeys.length; d++ ) {
            dataKey = angular.copy(datasource.dataKeys[d]);
            if (dataKey.type == types.dataKeyType.function) {
                dataKey.name = dataKey.label;
            }
            vm.dataKeys.push(dataKey);

            dataKey.title = utils.customTranslation(dataKey.label, dataKey.label);

            var keySettings = dataKey.settings;

            var cellStyleFunction = null;
            var useCellStyleFunction = null;
            var headerStyleFunction = false;
            var useHeaderStyleFunction = false;

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

            if (keySettings.useHeaderStyleFunction === true) {
                if (angular.isDefined(keySettings.headerStyleFunction) && keySettings.headerStyleFunction.length > 0) {
                    try {
                        headerStyleFunction = new Function(keySettings.headerStyleFunction);
                        useHeaderStyleFunction = true;
                    } catch (e) {
                        headerStyleFunction = null;
                        useHeaderStyleFunction = false;
                    }
                }
            }

            vm.stylesInfo[dataKey.label] = {
                useCellStyleFunction: useCellStyleFunction,
                cellStyleFunction: cellStyleFunction,
                useHeaderStyleFunction: useHeaderStyleFunction,
                headerStyleFunction: headerStyleFunction
            };

            var cellContentFunction = null;
            var useCellContentFunction = false;

            if (keySettings.useCellContentFunction === true) {
                if (angular.isDefined(keySettings.cellContentFunction) && keySettings.cellContentFunction.length > 0) {
                    try {
                        cellContentFunction = new Function('value, entity, filter', keySettings.cellContentFunction);
                        useCellContentFunction = true;
                    } catch (e) {
                        cellContentFunction = null;
                        useCellContentFunction = false;
                    }
                }
            }

            vm.contentsInfo[dataKey.label] = {
                useCellContentFunction: useCellContentFunction,
                cellContentFunction: cellContentFunction,
                units: dataKey.units,
                decimals: dataKey.decimals
            };

            var columnWidth = angular.isDefined(keySettings.columnWidth) ? keySettings.columnWidth : '0px';
            vm.columnWidth[dataKey.label] = columnWidth;

            dataKey.display = true;
            vm.columns.push(dataKey);
        }

        for (var i=0;i<vm.datasources.length;i++) {
            datasource = vm.datasources[i];
            if (datasource.type == types.datasourceType.entity && !datasource.entityId) {
                continue;
            }
            var entity = {
                id: {}
            };
            entity.entityName = datasource.entityName;
            if (datasource.entityLabel) {
                entity.entityLabel = datasource.entityLabel;
            } else {
                entity.entityLabel = datasource.entityName;
            }
            if (datasource.entityId) {
                entity.id.id = datasource.entityId;
            }
            if (datasource.entityType) {
                entity.id.entityType = datasource.entityType;
                entity.entityType = $translate.instant(types.entityTypeTranslations[datasource.entityType].type) + '';
            } else {
                entity.entityType = '';
            }
            for (d = 0; d < vm.dataKeys.length; d++) {
                dataKey = vm.dataKeys[d];
                entity[dataKey.label] = '';
            }
            vm.allEntities.push(entity);
        }

    }

}

/*@ngInject*/
function DisplayColumnsPanelController(columns) {  //eslint-disable-line

    var vm = this;
    vm.columns = columns;
}
