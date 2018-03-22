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

import auditLogDetailsDialogTemplate from './audit-log-details-dialog.tpl.html';

import auditLogRowTemplate from './audit-log-row.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AuditLogRowDirective($compile, $templateCache, types, $mdDialog, $document) {

    var linker = function (scope, element, attrs) {

        var template = $templateCache.get(auditLogRowTemplate);
        element.html(template);

        scope.auditLog = attrs.auditLog;
        scope.auditLogMode = attrs.auditLogMode;
        scope.types = types;

        scope.showAuditLogDetails = function($event) {
            var onShowingCallback = {
                onShowing: function(){}
            }
            $mdDialog.show({
                controller: 'AuditLogDetailsDialogController',
                controllerAs: 'vm',
                templateUrl: auditLogDetailsDialogTemplate,
                locals: {
                    auditLog: scope.auditLog,
                    showingCallback: onShowingCallback
                },
                parent: angular.element($document[0].body),
                targetEvent: $event,
                fullscreen: true,
                skipHide: true,
                onShowing: function(scope, element) {
                    onShowingCallback.onShowing(scope, element);
                }
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
