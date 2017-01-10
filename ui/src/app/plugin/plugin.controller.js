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
/* eslint-disable import/no-unresolved, import/default */

import addPluginTemplate from './add-plugin.tpl.html';
import pluginCard from './plugin-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function PluginController(pluginService, userService, $state, $stateParams, $filter, $translate, types, helpLinks) {

    var pluginActionsList = [
        {
            onAction: function ($event, item) {
                activatePlugin($event, item);
            },
            name: function() { return $translate.instant('action.activate') },
            details: function() { return $translate.instant('plugin.activate') },
            icon: "play_arrow",
            isEnabled: function(plugin) {
                return isPluginEditable(plugin) && plugin && plugin.state === 'SUSPENDED';
            }
        },
        {
            onAction: function ($event, item) {
                suspendPlugin($event, item);
            },
            name: function() { return $translate.instant('action.suspend') },
            details: function() { return $translate.instant('plugin.suspend') },
            icon: "pause",
            isEnabled: function(plugin) {
                return isPluginEditable(plugin) && plugin && plugin.state === 'ACTIVE';
            }
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('plugin.delete') },
            icon: "delete",
            isEnabled: isPluginEditable
        }
    ];

    var vm = this;

    vm.types = types;

    vm.helpLinkIdForPlugin = helpLinkIdForPlugin;

    vm.pluginGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deletePluginTitle,
        deleteItemContentFunc: deletePluginText,
        deleteItemsTitleFunc: deletePluginsTitle,
        deleteItemsActionTitleFunc: deletePluginsActionTitle,
        deleteItemsContentFunc: deletePluginsText,

        fetchItemsFunc: fetchPlugins,
        saveItemFunc: savePlugin,
        deleteItemFunc: deletePlugin,

        getItemTitleFunc: getPluginTitle,
        itemCardTemplateUrl: pluginCard,
        parentCtl: vm,

        actionsList: pluginActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addPluginTemplate,

        addItemText: function() { return $translate.instant('plugin.add-plugin-text') },
        noItemsText: function() { return $translate.instant('plugin.no-plugins-text') },
        itemDetailsText: function() { return $translate.instant('plugin.plugin-details') },
        isSelectionEnabled: isPluginEditable,
        isDetailsReadOnly: function(plugin) {
            return !isPluginEditable(plugin);
        }

    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.pluginGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.pluginGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.activatePlugin = activatePlugin;
    vm.suspendPlugin = suspendPlugin;

    function helpLinkIdForPlugin() {
        return helpLinks.getPluginLink(vm.grid.operatingItem());
    }

    function deletePluginTitle(plugin) {
        return $translate.instant('plugin.delete-plugin-title', {pluginName: plugin.name});
    }

    function deletePluginText() {
        return $translate.instant('plugin.delete-plugin-text');
    }

    function deletePluginsTitle(selectedCount) {
        return $translate.instant('plugin.delete-plugins-title', {count: selectedCount}, 'messageformat');
    }

    function deletePluginsActionTitle(selectedCount) {
        return $translate.instant('plugin.delete-plugins-action-title', {count: selectedCount}, 'messageformat');
    }

    function deletePluginsText() {
        return $translate.instant('plugin.delete-plugins-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchPlugins(pageLink) {
        return pluginService.getAllPlugins(pageLink);
    }

    function savePlugin(plugin) {
        return pluginService.savePlugin(plugin);
    }

    function deletePlugin(pluginId) {
        return pluginService.deletePlugin(pluginId);
    }

    function getPluginTitle(plugin) {
        return plugin ? plugin.name : '';
    }

    function isPluginEditable(plugin) {
        if (userService.getAuthority() === 'TENANT_ADMIN') {
            return plugin && plugin.tenantId.id != types.id.nullUid;
        } else {
            return userService.getAuthority() === 'SYS_ADMIN';
        }
    }

    function activatePlugin(event, plugin) {
        pluginService.activatePlugin(plugin.id.id).then(function () {
            vm.grid.refreshList();
        }, function () {
        });
    }

    function suspendPlugin(event, plugin) {
        pluginService.suspendPlugin(plugin.id.id).then(function () {
            vm.grid.refreshList();
        }, function () {
        });
    }

}
