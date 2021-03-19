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
/* eslint-disable import/no-unresolved, import/default */

import edgeDownlinksContentTemplate from './edge-downlinks-content-dialog.tpl.html';
import edgeDownlinlsRowTemplate from './edge-downlinks-row.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeDownlinksRowDirective($compile, $templateCache, $mdDialog, $document, $translate, types, utils, toast, entityService) {

    var linker = function (scope, element, attrs) {

        var template = edgeDownlinlsRowTemplate;

        element.html($templateCache.get(template));
        $compile(element.contents())(scope);

        scope.types = types;
        scope.downlink = attrs.downlink;

        scope.showEdgeEntityContent = function($event) {
            var onShowingCallback = {
                onShowing: function(){}
            }
            var content = entityService.getEdgeEventContentByEntityType(scope.downlink).then(
                function success(content) {
                    showDialog();
                    return angular.toJson(content);
                }, function fail() {
                    showError();
                });
            function showDialog() {
                $mdDialog.show({
                    controller: 'EdgeDownlinksContentDialogController',
                    controllerAs: 'vm',
                    templateUrl: edgeDownlinksContentTemplate,
                    locals: {
                        content,
                        title: $translate.instant('event.data'),
                        contentType: types.contentType.JSON.value,
                        showingCallback: onShowingCallback
                    },
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event,
                    multiple: true,
                    onShowing: function(scope, element) {
                        onShowingCallback.onShowing(scope, element);
                    }
                });
            }
            function showError() {
                toast.showError($translate.instant('edge.load-entity-error'));
            }
        }

        scope.isEdgeEventHasData = function(type) {
            return !(
                type === types.edgeEventType.adminSettings
            );
        }

        $compile(element.contents())(scope);

        scope.updateStatus = function(downlinkCreatedTime) {
            var status;
            if (downlinkCreatedTime < scope.queueStartTs) {
                status = $translate.instant(types.edgeEventStatus.DEPLOYED.name);
                scope.statusColor = types.edgeEventStatus.DEPLOYED.color;
            } else {
                status = $translate.instant(types.edgeEventStatus.PENDING.name);
                scope.statusColor = types.edgeEventStatus.PENDING.color;
            }
            return status;
        }
    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
