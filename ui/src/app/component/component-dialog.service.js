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

import componentDialogTemplate from './component-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ComponentDialogService($mdDialog, $document, $q) {

    var service = {
        openComponentDialog: openComponentDialog
    }

    return service;

    function openComponentDialog($event, isAdd, readOnly, title, type, pluginClazz, component) {
        var deferred = $q.defer();
        var componentInfo = {
            title: title,
            type: type,
            pluginClazz: pluginClazz
        };
        if (component) {
            componentInfo.component = angular.copy(component);
        }
        $mdDialog.show({
            controller: 'ComponentDialogController',
            controllerAs: 'vm',
            templateUrl: componentDialogTemplate,
            locals: {isAdd: isAdd,
                isReadOnly: readOnly,
                componentInfo: componentInfo},
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event,
            skipHide: true
        }).then(function (component) {
            deferred.resolve(component);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

}