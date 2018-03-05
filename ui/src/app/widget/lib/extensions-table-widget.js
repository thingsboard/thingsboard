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
import './extensions-table-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import extensionsTableWidgetTemplate from './extensions-table-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.extensionsTableWidget', [])
    .directive('tbExtensionsTableWidget', ExtensionsTableWidget)
    .name;

/*@ngInject*/
function ExtensionsTableWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: ExtensionsTableWidgetController,
        controllerAs: 'vm',
        templateUrl: extensionsTableWidgetTemplate
    };
}

/*@ngInject*/
function ExtensionsTableWidgetController($scope, $translate, utils) {
    var vm = this;

    vm.datasources = null;
    vm.tabsHidden = false;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            initializeConfig();
            updateDatasources();
        }
    });

    function initializeConfig() {

        if (vm.settings.extensionsTitle && vm.settings.extensionsTitle.length) {
            vm.extensionsTitle = utils.customTranslation(vm.settings.extensionsTitle, vm.settings.extensionsTitle);
        } else {
            vm.extensionsTitle = $translate.instant('extension.extensions');
        }
        vm.ctx.widgetTitle = vm.extensionsTitle;

        vm.ctx.widgetActions = [vm.importExtensionsAction, vm.exportExtensionsAction, vm.addAction, vm.searchAction, vm.refreshAction];
    }

    function updateDatasources() {

        var datasource = vm.datasources[0];
        vm.selectedSource = vm.datasources[0];
        vm.ctx.widgetTitle = utils.createLabelFromDatasource(datasource, vm.extensionsTitle);
    }

    vm.changeSelectedSource = function(source) {
        vm.selectedSource = source;
    };

    vm.searchAction = {
        name: "action.search",
        show: true,
        onAction: function() {
            $scope.$broadcast("showSearch", vm.selectedSource);
        },
        icon: "search"
    };

    vm.refreshAction = {
        name: "action.refresh",
        show: true,
        onAction: function() {
            $scope.$broadcast("refreshExtensions", vm.selectedSource);
        },
        icon: "refresh"
    };

    vm.addAction = {
        name: "action.add",
        show: true,
        onAction: function() {
            $scope.$broadcast("addExtension", vm.selectedSource);
        },
        icon: "add"
    };

    vm.exportExtensionsAction = {
        name: "extension.export-extensions-configuration",
        show: true,
        onAction: function() {
            $scope.$broadcast("exportExtensions", vm.selectedSource);
        },
        icon: "file_download"
    };

    vm.importExtensionsAction = {
        name: "extension.import-extensions-configuration",
        show: true,
        onAction: function() {
            $scope.$broadcast("importExtensions", vm.selectedSource);
        },
        icon: "file_upload"
    };

    $scope.$on("filterMode", function($event, mode) {
        vm.tabsHidden = mode;
    });

    $scope.$on("selectedExtensions", function($event, mode) {
        vm.tabsHidden = mode;
    });
}