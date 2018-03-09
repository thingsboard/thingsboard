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
/* eslint-disable import/no-unresolved, import/default */

import addWidgetsBundleTemplate from './add-widgets-bundle.tpl.html';
import widgetsBundleCard from './widgets-bundle-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function WidgetsBundleController(widgetService, userService, importExport, $state, $stateParams, $filter, $translate, types) {

    var widgetsBundleActionsList = [
        {
            onAction: function ($event, item) {
                exportWidgetsBundle($event, item);
            },
            name: function() { $translate.instant('action.export') },
            details: function() { return $translate.instant('widgets-bundle.export') },
            icon: "file_download"
        },
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('widgets-bundle.details') },
            details: function() { return $translate.instant('widgets-bundle.widgets-bundle-details') },
            icon: "edit"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('widgets-bundle.delete') },
            icon: "delete",
            isEnabled: isWidgetsBundleEditable
        }
   ];

    var widgetsBundleAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.create') },
            details: function() { return $translate.instant('widgets-bundle.create-new-widgets-bundle') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importWidgetsBundle($event).then(
                    function() {
                        vm.grid.refreshList();
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('widgets-bundle.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.widgetsBundleGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteWidgetsBundleTitle,
        deleteItemContentFunc: deleteWidgetsBundleText,
        deleteItemsTitleFunc: deleteWidgetsBundlesTitle,
        deleteItemsActionTitleFunc: deleteWidgetsBundlesActionTitle,
        deleteItemsContentFunc: deleteWidgetsBundlesText,

        fetchItemsFunc: fetchWidgetsBundles,
        saveItemFunc: saveWidgetsBundle,
        clickItemFunc: openWidgetsBundle,
        deleteItemFunc: deleteWidgetsBundle,

        getItemTitleFunc: getWidgetsBundleTitle,
        itemCardTemplateUrl: widgetsBundleCard,
        parentCtl: vm,

        actionsList: widgetsBundleActionsList,
        addItemActions: widgetsBundleAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addWidgetsBundleTemplate,

        addItemText: function() { return $translate.instant('widgets-bundle.add-widgets-bundle-text') },
        noItemsText: function() { return $translate.instant('widgets-bundle.no-widgets-bundles-text') },
        itemDetailsText: function() { return $translate.instant('widgets-bundle.widgets-bundle-details') },
        isSelectionEnabled: isWidgetsBundleEditable,
        isDetailsReadOnly: function(widgetsBundle) {
             return !isWidgetsBundleEditable(widgetsBundle);
        }

    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.widgetsBundleGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.widgetsBundleGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.exportWidgetsBundle = exportWidgetsBundle;

    function deleteWidgetsBundleTitle(widgetsBundle) {
        return $translate.instant('widgets-bundle.delete-widgets-bundle-title', {widgetsBundleTitle: widgetsBundle.title});
    }

    function deleteWidgetsBundleText() {
        return $translate.instant('widgets-bundle.delete-widgets-bundle-text');
    }

    function deleteWidgetsBundlesTitle(selectedCount) {
        return $translate.instant('widgets-bundle.delete-widgets-bundles-title', {count: selectedCount}, 'messageformat');
    }

    function deleteWidgetsBundlesActionTitle(selectedCount) {
        return $translate.instant('widgets-bundle.delete-widgets-bundles-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteWidgetsBundlesText() {
        return $translate.instant('widgets-bundle.delete-widgets-bundles-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchWidgetsBundles(pageLink) {
        return widgetService.getAllWidgetsBundlesByPageLink(pageLink);
    }

    function saveWidgetsBundle(widgetsBundle) {
        return widgetService.saveWidgetsBundle(widgetsBundle);
    }

    function deleteWidgetsBundle(widgetsBundleId) {
        return widgetService.deleteWidgetsBundle(widgetsBundleId);
    }

    function getWidgetsBundleTitle(widgetsBundle) {
        return widgetsBundle ? widgetsBundle.title : '';
    }

    function isWidgetsBundleEditable(widgetsBundle) {
        if (userService.getAuthority() === 'TENANT_ADMIN') {
            return widgetsBundle && widgetsBundle.tenantId.id != types.id.nullUid;
        } else {
            return userService.getAuthority() === 'SYS_ADMIN';
        }
    }

    function exportWidgetsBundle($event, widgetsBundle) {
        $event.stopPropagation();
        importExport.exportWidgetsBundle(widgetsBundle.id.id);
    }

    function openWidgetsBundle($event, widgetsBundle) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.widgets-bundles.widget-types', {widgetsBundleId: widgetsBundle.id.id});
    }

}
