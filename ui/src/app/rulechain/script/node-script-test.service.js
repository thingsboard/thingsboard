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

import nodeScriptTestTemplate from './node-script-test.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function NodeScriptTest($q, $mdDialog, $document) {

    var service = {
        testNodeScript: testNodeScript
    };

    return service;

    function testNodeScript($event, script, scriptType, functionTitle, functionName, argNames, msg, metadata, msgType) {
        var deferred = $q.defer();

        if ($event) {
            $event.stopPropagation();
        }
        var onShowingCallback = {
            onShowed: () => {
            }
        };

        var inputParams = {
            script: script,
            scriptType: scriptType,
            functionName: functionName,
            argNames: argNames
        };

        $mdDialog.show({
            controller: 'NodeScriptTestController',
            controllerAs: 'vm',
            templateUrl: nodeScriptTestTemplate,
            parent: angular.element($document[0].body),
            locals: {
                msg: msg,
                metadata: metadata,
                msgType: msgType,
                functionTitle: functionTitle,
                inputParams: inputParams,
                onShowingCallback: onShowingCallback
            },
            fullscreen: true,
            skipHide: true,
            targetEvent: $event,
            onComplete: () => {
                onShowingCallback.onShowed();
            }
        }).then(
            (script) => {
                deferred.resolve(script);
            },
            () => {
                deferred.reject();
            }
        );

        return deferred.promise;
    }

}