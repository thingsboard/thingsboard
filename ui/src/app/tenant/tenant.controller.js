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

import addTenantTemplate from './add-tenant.tpl.html';
import tenantCard from './tenant-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function TenantController(tenantService, $state, $stateParams, $translate, types) {

    var tenantActionsList = [
        {
            onAction: function ($event, item) {
                openTenantUsers($event, item);
            },
            name: function() { return $translate.instant('tenant.admins') },
            details: function() { return $translate.instant('tenant.manage-tenant-admins') },
            icon: "account_circle"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('tenant.delete') },
            icon: "delete"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.tenantGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteTenantTitle,
        deleteItemContentFunc: deleteTenantText,
        deleteItemsTitleFunc: deleteTenantsTitle,
        deleteItemsActionTitleFunc: deleteTenantsActionTitle,
        deleteItemsContentFunc: deleteTenantsText,

        fetchItemsFunc: fetchTenants,
        saveItemFunc: saveTenant,
        deleteItemFunc: deleteTenant,

        getItemTitleFunc: getTenantTitle,

        itemCardTemplateUrl: tenantCard,

        actionsList: tenantActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addTenantTemplate,

        addItemText: function() { return $translate.instant('tenant.add-tenant-text') },
        noItemsText: function() { return $translate.instant('tenant.no-tenants-text') },
        itemDetailsText: function() { return $translate.instant('tenant.tenant-details') }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.tenantGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.tenantGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.openTenantUsers = openTenantUsers;

    function deleteTenantTitle(tenant) {
        return $translate.instant('tenant.delete-tenant-title', {tenantTitle: tenant.title});
    }

    function deleteTenantText() {
        return $translate.instant('tenant.delete-tenant-text');
    }

    function deleteTenantsTitle(selectedCount) {
        return $translate.instant('tenant.delete-tenants-title', {count: selectedCount}, 'messageformat');
    }

    function deleteTenantsActionTitle(selectedCount) {
        return $translate.instant('tenant.delete-tenants-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteTenantsText() {
        return $translate.instant('tenant.delete-tenants-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchTenants(pageLink) {
        return tenantService.getTenants(pageLink);
    }

    function saveTenant(tenant) {
        return tenantService.saveTenant(tenant);
    }

    function deleteTenant(tenantId) {
        return tenantService.deleteTenant(tenantId);
    }

    function getTenantTitle(tenant) {
        return tenant ? tenant.title : '';
    }

    function openTenantUsers($event, tenant) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.tenants.users', {tenantId: tenant.id.id});
    }
}
