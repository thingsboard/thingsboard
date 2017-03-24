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
import './details-sidenav.scss';

/* eslint-disable import/no-unresolved, import/default */

import detailsSidenavTemplate from './details-sidenav.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.detailsSidenav', [])
    .directive('tbDetailsSidenav', DetailsSidenav)
    .name;

/*@ngInject*/
function DetailsSidenav($timeout) {

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