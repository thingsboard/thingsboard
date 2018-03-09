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
import './dashboard-toolbar.scss';

import 'javascript-detect-element-resize/detect-element-resize';

/* eslint-disable import/no-unresolved, import/default */

import dashboardToolbarTemplate from './dashboard-toolbar.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DashboardToolbar() {
    return {
        restrict: "E",
        scope: true,
        transclude: true,
        bindToController: {
            toolbarOpened: '=',
            forceFullscreen: '=',
            onTriggerClick: '&'
        },
        controller: DashboardToolbarController,
        controllerAs: 'vm',
        templateUrl: dashboardToolbarTemplate
    };
}

/* eslint-disable angular/angularelement */


/*@ngInject*/
function DashboardToolbarController($scope, $element, $timeout, mdFabToolbarAnimation) {

    let vm = this;

    vm.mdFabToolbarElement = angular.element($element[0].querySelector('md-fab-toolbar'));

    function initElements() {
        $timeout(function() {
            vm.mdFabBackgroundElement = angular.element(vm.mdFabToolbarElement[0].querySelector('.md-fab-toolbar-background'));
            vm.mdFabTriggerElement = angular.element(vm.mdFabToolbarElement[0].querySelector('md-fab-trigger button'));
            if (!vm.mdFabBackgroundElement || !vm.mdFabBackgroundElement[0]) {
                initElements();
            } else {
                triggerFabResize();
            }
        });
    }

    addResizeListener(vm.mdFabToolbarElement[0], triggerFabResize); // eslint-disable-line no-undef

    $scope.$on("$destroy", function () {
        removeResizeListener(vm.mdFabToolbarElement[0], triggerFabResize); // eslint-disable-line no-undef
    });

    initElements();

    function triggerFabResize() {
        if (!vm.mdFabBackgroundElement || !vm.mdFabBackgroundElement[0]) {
            return;
        }
        var ctrl = vm.mdFabToolbarElement.controller('mdFabToolbar');
        if (ctrl.isOpen) {
            if (!vm.mdFabBackgroundElement[0].offsetWidth) {
                mdFabToolbarAnimation.addClass(vm.mdFabToolbarElement, 'md-is-open', function () {
                });
            } else {
                var color = window.getComputedStyle(vm.mdFabTriggerElement[0]).getPropertyValue('background-color'); //eslint-disable-line

                var width = vm.mdFabToolbarElement[0].offsetWidth;
                var scale = 2 * (width / vm.mdFabTriggerElement[0].offsetWidth);
                vm.mdFabBackgroundElement[0].style.backgroundColor = color;
                vm.mdFabBackgroundElement[0].style.borderRadius = width + 'px';

                var transform = vm.mdFabBackgroundElement[0].style.transform;
                var targetTransform = 'scale(' + scale + ')';
                if (!transform || !angular.equals(transform, targetTransform)) {
                    vm.mdFabBackgroundElement[0].style.transform = targetTransform;
                }
            }
        }
    }
}

