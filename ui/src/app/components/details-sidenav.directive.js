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
import './details-sidenav.scss';

/* eslint-disable import/no-unresolved, import/default */

import detailsSidenavTemplate from './details-sidenav.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.detailsSidenav', [])
    .directive('tbDetailsSidenav', DetailsSidenav)
    .name;

/*@ngInject*/
function DetailsSidenav($timeout, $mdUtil, $q, $animate) {

    var linker = function (scope, element, attrs) {

        if (angular.isUndefined(attrs.isReadOnly)) {
            attrs.isReadOnly = false;
        }

        if (angular.isUndefined(scope.headerHeightPx)) {
            scope.headerHeightPx = 100;
        }

        if (angular.isDefined(attrs.isAlwaysEdit) && attrs.isAlwaysEdit) {
            scope.isEdit = true;
        }

        var backdrop;
        var previousContainerStyles;

        if (attrs.hasOwnProperty('tbEnableBackdrop')) {
            backdrop = $mdUtil.createBackdrop(scope, "md-sidenav-backdrop md-opaque ng-enter");
            element.on('$destroy', function() {
                backdrop && backdrop.remove();
            });
            scope.$on('$destroy', function(){
                backdrop && backdrop.remove();
            });
            scope.$watch('isOpen', updateIsOpen);
        }

        function updateIsOpen(isOpen) {
            backdrop[isOpen ? 'on' : 'off']('click', (ev)=>{
                ev.preventDefault();
                scope.isOpen = false;
                scope.$apply();
            });
            var parent = element.parent();
            var restorePositioning = updateContainerPositions(parent, isOpen);

            return $q.all([
                isOpen && backdrop ? $animate.enter(backdrop, parent) : backdrop ?
                    $animate.leave(backdrop) : $q.when(true)
            ]).then(function() {
                restorePositioning && restorePositioning();
            });
        }

        function updateContainerPositions(parent, willOpen) {
            var drawerEl = element[0];
            var scrollTop = parent[0].scrollTop;
            if (willOpen && scrollTop) {
                previousContainerStyles = {
                    top: drawerEl.style.top,
                    bottom: drawerEl.style.bottom,
                    height: drawerEl.style.height
                };
                var positionStyle = {
                    top: scrollTop + 'px',
                    bottom: 'auto',
                    height: parent[0].clientHeight + 'px'
                };
                backdrop.css(positionStyle);
            }
            if (!willOpen && previousContainerStyles) {
                return function() {
                    backdrop[0].style.top = null;
                    backdrop[0].style.bottom = null;
                    backdrop[0].style.height = null;
                    previousContainerStyles = null;
                };
            }
        }

        scope.toggleDetailsEditMode = function () {
            if (!scope.isAlwaysEdit) {
                if (!scope.isEdit) {
                    scope.isEdit = true;
                } else {
                    scope.isEdit = false;
                }
            }
            $timeout(function () {
                scope.onToggleDetailsEditMode();
            });
        };

        scope.detailsApply = function () {
            $timeout(function () {
                scope.onApplyDetails();
            });
        }

        scope.closeDetails = function () {
            scope.isOpen = false;
            $timeout(function () {
                scope.onCloseDetails();
            });
        };
    }

    return {
        restrict: "E",
        transclude: {
            headerPane: '?headerPane',
            detailsButtons: '?detailsButtons'
        },
        scope: {
            headerTitle: '@',
            headerSubtitle: '@',
            headerHeightPx: '@',
            isReadOnly: '=',
            isOpen: '=',
            isEdit: '=?',
            isAlwaysEdit: '=?',
            theForm: '=',
            onCloseDetails: '&',
            onToggleDetailsEditMode: '&',
            onApplyDetails: '&'
        },
        link: linker,
        templateUrl: detailsSidenavTemplate
    };
}