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
export default function ManageAssignedCustomersController($mdDialog, $q, types, dashboardService, actionType, dashboardIds, assignedCustomers) {

    var vm = this;

    vm.types = types;
    vm.actionType = actionType;
    vm.dashboardIds = dashboardIds;
    vm.assignedCustomers = assignedCustomers;
    if (actionType != 'manage') {
        vm.assignedCustomers = [];
    }

    if (actionType == 'manage') {
        vm.titleText = 'dashboard.manage-assigned-customers';
        vm.labelText = 'dashboard.assigned-customers';
        vm.actionName = 'action.update';
    } else if (actionType == 'assign') {
        vm.titleText = 'dashboard.assign-to-customers';
        vm.labelText = 'dashboard.assign-to-customers-text';
        vm.actionName = 'action.assign';
    } else if (actionType == 'unassign') {
        vm.titleText = 'dashboard.unassign-from-customers';
        vm.labelText = 'dashboard.unassign-from-customers-text';
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
                promise = dashboardService.updateDashboardCustomers(dashboardId, vm.assignedCustomers);
            } else if (vm.actionType == 'assign') {
                promise = dashboardService.addDashboardCustomers(dashboardId, vm.assignedCustomers);
            } else if (vm.actionType == 'unassign') {
                promise = dashboardService.removeDashboardCustomers(dashboardId, vm.assignedCustomers);
            }
            tasks.push(promise);
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

}
