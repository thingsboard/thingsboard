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
import './dashboard-settings.scss';

/*@ngInject*/
export default function DashboardSettingsController($scope, $mdDialog, gridSettings) {

    var vm = this;

    vm.cancel = cancel;
    vm.save = save;
    vm.imageAdded = imageAdded;
    vm.clearImage = clearImage;

    vm.gridSettings = gridSettings || {};

    if (angular.isUndefined(vm.gridSettings.showTitle)) {
        vm.gridSettings.showTitle = true;
    }

    if (angular.isUndefined(vm.gridSettings.showDevicesSelect)) {
        vm.gridSettings.showDevicesSelect = true;
    }

    if (angular.isUndefined(vm.gridSettings.showDashboardTimewindow)) {
        vm.gridSettings.showDashboardTimewindow = true;
    }

    if (angular.isUndefined(vm.gridSettings.showDashboardExport)) {
        vm.gridSettings.showDashboardExport = true;
    }

    vm.gridSettings.backgroundColor = vm.gridSettings.backgroundColor || 'rgba(0,0,0,0)';
    vm.gridSettings.titleColor = vm.gridSettings.titleColor || 'rgba(0,0,0,0.870588)';
    vm.gridSettings.columns = vm.gridSettings.columns || 24;
    vm.gridSettings.margins = vm.gridSettings.margins || [10, 10];
    vm.hMargin = vm.gridSettings.margins[0];
    vm.vMargin = vm.gridSettings.margins[1];

    vm.gridSettings.backgroundSizeMode = vm.gridSettings.backgroundSizeMode || '100%';

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

    function save() {
        $scope.theForm.$setPristine();
        vm.gridSettings.margins = [vm.hMargin, vm.vMargin];
        $mdDialog.hide(vm.gridSettings);
    }
}
