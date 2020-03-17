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
export default function ManageAssignedEdgesToDashboardController($mdDialog, $q, types, dashboardService, actionType, dashboardIds, assignedEdges) {

    var vm = this;

    vm.types = types;
    vm.actionType = actionType;
    vm.dashboardIds = dashboardIds;
    vm.assignedEdges = assignedEdges;
    if (actionType != 'manage') {
        vm.assignedEdges = [];
    }

    if (actionType == 'manage') {
        vm.titleText = 'dashboard.manage-assigned-edges';
        vm.labelText = 'dashboard.assigned-edges';
        vm.actionName = 'action.update';
    } else if (actionType == 'assign') {
        vm.titleText = 'dashboard.assign-to-edges';
        vm.labelText = 'dashboard.assign-to-edges-text';
        vm.actionName = 'action.assign';
    } else if (actionType == 'unassign') {
        vm.titleText = 'dashboard.unassign-from-edges';
        vm.labelText = 'dashboard.unassign-from-edges-text';
        vm.actionName = 'action.unassign';
    }

    vm.submit = submit;
    vm.cancel = cancel;

    function cancel () {
        $mdDialog.cancel();
    }

    function submit () {
        var tasks = [];
        for (var i=0;i<vm.dashboardIds.length;i++) {
            var dashboardId = vm.dashboardIds[i];
            var promise;
            if (vm.actionType == 'manage') {
                promise = dashboardService.updateDashboardEdges(dashboardId, vm.assignedEdges);
            } else if (vm.actionType == 'assign') {
                promise = dashboardService.addDashboardEdges(dashboardId, vm.assignedEdges);
            } else if (vm.actionType == 'unassign') {
                promise = dashboardService.removeDashboardEdges(dashboardId, vm.assignedEdges);
            }
            tasks.push(promise);
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

}
