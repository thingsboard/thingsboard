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
import './dashboard-settings.scss';

/*@ngInject*/
export default function DashboardSettingsController($scope, $mdDialog, $q, $filter, statesControllerService, settings,
    gridSettings, stateSettings, entityAliasesList) {
        
    var vm = this;

    vm.cancel = cancel;
    vm.save = save;
    vm.imageAdded = imageAdded;
    vm.clearImage = clearImage;
    vm.querySearch = querySearch;

    vm.stateControllerIdChanged = stateControllerIdChanged;

    vm.settings = settings;
    vm.gridSettings = gridSettings;
    vm.stateSettings = stateSettings;
    vm.entityAliasesList = entityAliasesList;
    vm.stateControllers = statesControllerService.getStateControllers();

    vm.selectedItem = null;
    vm.searchText = '';

    if (vm.settings) {
        if (angular.isUndefined(vm.settings.stateControllerId)) {
            vm.settings.stateControllerId = 'entity';
        }

        if (angular.isUndefined(vm.settings.showTitle)) {
            vm.settings.showTitle = false;
        }

        if (angular.isUndefined(vm.settings.titleColor)) {
            vm.settings.titleColor = 'rgba(0,0,0,0.870588)';
        }

        if (angular.isUndefined(vm.settings.showDashboardExport)) {
            vm.settings.showDashboardExport = true;
        }

        if (angular.isUndefined(vm.settings.toolbarAlwaysOpen)) {
            vm.settings.toolbarAlwaysOpen = true;
        }
    }

    if (vm.gridSettings) {
        vm.gridSettings.backgroundColor = vm.gridSettings.backgroundColor || 'rgba(0,0,0,0)';
        vm.gridSettings.color = vm.gridSettings.color || 'rgba(0,0,0,0.870588)';
        vm.gridSettings.columns = vm.gridSettings.columns || 24;
        vm.gridSettings.margins = vm.gridSettings.margins || [10, 10];
        vm.gridSettings.autoFillHeight = angular.isDefined(vm.gridSettings.autoFillHeight) ? vm.gridSettings.autoFillHeight : false;
        vm.gridSettings.mobileAutoFillHeight = angular.isDefined(vm.gridSettings.mobileAutoFillHeight) ? vm.gridSettings.mobileAutoFillHeight : false;
        vm.gridSettings.mobileRowHeight = angular.isDefined(vm.gridSettings.mobileRowHeight) ? vm.gridSettings.mobileRowHeight : 70;
        vm.hMargin = vm.gridSettings.margins[0];
        vm.vMargin = vm.gridSettings.margins[1];
        vm.gridSettings.backgroundSizeMode = vm.gridSettings.backgroundSizeMode || '100%';
    }

    if (vm.stateSettings) {
        if (angular.isUndefined(vm.stateSettings.showDashboardsSelect)) {
            vm.stateSettings.showDashboardsSelect = false;
        }

        if (angular.isUndefined(vm.stateSettings.showEntitiesSelect)) {
            vm.stateSettings.showEntitiesSelect = true;
        }

        if (angular.isUndefined(vm.stateSettings.showDashboardTimewindow)) {
            vm.stateSettings.showDashboardTimewindow = false;
        }

        if (angular.isUndefined(vm.stateSettings.entityAliasesIcon)) {
            vm.stateSettings.entityAliasesIcon = 'devices_other';
        }

        if (angular.isUndefined(vm.stateSettings.entityAliasesLabel)) {
            vm.stateSettings.entityAliasesLabel = '';
        }

        if (angular.isUndefined(vm.stateSettings.entityAliasesList)) {
            vm.stateSettings.entityAliasesList = [];
        }

        if (angular.isUndefined(vm.stateSettings.minEntitiesToShowSelect)) {
            vm.stateSettings.minEntitiesToShowSelect = 1;
        }
    }

    function querySearch(searchText) {
      var deferred = $q.defer();
      var entityAliasesList = $filter('filter')(vm.entityAliasesList, {alias: searchText});
      deferred.resolve(entityAliasesList);
      return deferred.promise;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function imageAdded($file) {
        var reader = new FileReader();
        reader.onload = function(event) {
            $scope.$apply(function() {
                if (event.target.result && event.target.result.startsWith('data:image/')) {
                    $scope.theForm.$setDirty();
                    vm.gridSettings.backgroundImageUrl = event.target.result;
                }
            });
        };
        reader.readAsDataURL($file.file);
    }

    function clearImage() {
        $scope.theForm.$setDirty();
        vm.gridSettings.backgroundImageUrl = null;
    }

    function stateControllerIdChanged() {
        if (vm.settings.stateControllerId != 'default') {
            vm.settings.toolbarAlwaysOpen = true;
        }
    }

    function save() {
        $scope.theForm.$setPristine();
        if (vm.gridSettings) {
            vm.gridSettings.margins = [vm.hMargin, vm.vMargin];
        }
        $mdDialog.hide(
            {
                settings: vm.settings,
                gridSettings: vm.gridSettings,
                stateSettings: vm.stateSettings
            }
        );
    }
}
