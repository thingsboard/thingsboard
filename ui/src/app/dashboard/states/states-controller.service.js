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

import defaultStateControllerTemplate from './default-state-controller.tpl.html';
import entityStateControllerTemplate from './entity-state-controller.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import DefaultStateController from './default-state-controller';
import EntityStateController from './entity-state-controller';

/*@ngInject*/
export default function StatesControllerService() {

    var statesControllers = {};
    statesControllers['default'] = {
        controller: DefaultStateController,
        templateUrl: defaultStateControllerTemplate
    };
    statesControllers['entity'] = {
        controller: EntityStateController,
        templateUrl: entityStateControllerTemplate
    };

    var service = {
        registerStatesController: registerStatesController,
        getStateControllers: getStateControllers,
        getStateController: getStateController,
        preserveStateControllerState: preserveStateControllerState,
        withdrawStateControllerState: withdrawStateControllerState,
        cleanupPreservedStates: cleanupPreservedStates
    };

    return service;

    function registerStatesController(id, stateControllerInfo) {
        statesControllers[id] = stateControllerInfo;
    }

    function getStateControllers() {
        return statesControllers;
    }

    function getStateController(id) {
        return statesControllers[id];
    }

    function preserveStateControllerState(id, state) {
        statesControllers[id].state = angular.copy(state);
    }

    function withdrawStateControllerState(id) {
        var state = statesControllers[id].state;
        statesControllers[id].state = null;
        return state;
    }

    function cleanupPreservedStates() {
        for (var id in statesControllers) {
            statesControllers[id].state = null;
        }
    }

}
