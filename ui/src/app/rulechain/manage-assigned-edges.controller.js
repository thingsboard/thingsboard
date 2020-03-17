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
/*@ngInject*/
export default function ManageAssignedEdgesToRuleChainController($mdDialog, $q, types, ruleChainService, actionType, ruleChainIds, assignedEdges) {

    var vm = this;

    vm.types = types;
    vm.actionType = actionType;
    vm.ruleChainIds = ruleChainIds;
    vm.assignedEdges = assignedEdges;
    if (actionType != 'manage') {
        vm.assignedEdges = [];
    }

    if (actionType == 'manage') {
        vm.titleText = 'rulechain.manage-assigned-edges';
        vm.labelText = 'rulechain.assigned-edges';
        vm.actionName = 'action.update';
    } else if (actionType == 'assign') {
        vm.titleText = 'rulechain.assign-to-edges';
        vm.labelText = 'rulechain.assign-to-edges-text';
        vm.actionName = 'action.assign';
    } else if (actionType == 'unassign') {
        vm.titleText = 'rulechain.unassign-from-edges';
        vm.labelText = 'rulechain.unassign-from-edges-text';
        vm.actionName = 'action.unassign';
    }

    vm.submit = submit;
    vm.cancel = cancel;

    function cancel () {
        $mdDialog.cancel();
    }

    function submit () {
        var tasks = [];
        for (var i=0;i<vm.ruleChainIds.length;i++) {
            var ruleChainId = vm.ruleChainIds[i];
            var promise;
            if (vm.actionType == 'manage') {
                promise = ruleChainService.updateRuleChainEdges(ruleChainId, vm.assignedEdges);
            } else if (vm.actionType == 'assign') {
                promise = ruleChainService.addRuleChainEdges(ruleChainId, vm.assignedEdges);
            } else if (vm.actionType == 'unassign') {
                promise = ruleChainService.removeRuleChainEdges(ruleChainId, vm.assignedEdges);
            }
            tasks.push(promise);
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

}
