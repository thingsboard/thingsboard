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
import './material-icons-dialog.scss';

/*@ngInject*/
export default function MaterialIconsDialogController($scope, $mdDialog, $timeout, utils, icon) {

    var vm = this;

    vm.selectedIcon = icon;

    vm.showAll = false;
    vm.loadingIcons = false;

    $scope.$watch('vm.showAll', function(showAll) {
        if (showAll) {
            vm.loadingIcons = true;
            $timeout(function() {
                utils.getMaterialIcons().then(
                    function success(icons) {
                        vm.icons = icons;
                    }
                );
            });
        } else {
            vm.icons = utils.getCommonMaterialIcons();
        }
    });

    $scope.$on('iconsLoadFinished', function() {
        vm.loadingIcons = false;
    });

    vm.cancel = cancel;
    vm.selectIcon = selectIcon;

    function cancel() {
        $mdDialog.cancel();
    }

    function selectIcon($event, icon) {
        vm.selectedIcon = icon;
        $mdDialog.hide(vm.selectedIcon);
    }
}
