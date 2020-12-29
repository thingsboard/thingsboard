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

import addCustomerTemplate from './add-customer.tpl.html';
import customerCard from './customer-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CustomerController(customerService, $state, $stateParams, $translate, types) {

    var customerActionsList = [
        {
            onAction: function ($event, item) {
                openCustomerUsers($event, item);
            },
            name: function() { return $translate.instant('user.users') },
            details: function() { return $translate.instant('customer.manage-customer-users') },
            icon: "account_circle",
            isEnabled: function(customer) {
                return customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
            }
        },
        {
            onAction: function ($event, item) {
                openCustomerAssets($event, item);
            },
            name: function() { return $translate.instant('asset.assets') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-assets')
                } else {
                    return $translate.instant('customer.manage-customer-assets')
                }
            },
            icon: "domain"
        },
        {
            onAction: function ($event, item) {
                openCustomerDevices($event, item);
            },
            name: function() { return $translate.instant('device.devices') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-devices')
                } else {
                    return $translate.instant('customer.manage-customer-devices')
                }
            },
            icon: "devices_other"
        },
        {
            onAction: function ($event, item) {
                openCustomerDashboards($event, item);
            },
            name: function() { return $translate.instant('dashboard.dashboards') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-dashboards')
                } else {
                    return $translate.instant('customer.manage-customer-dashboards')
                }
            },
            icon: "dashboard"
        },
        {
            onAction: function ($event, item) {
                openCustomerEdges($event, item);
            },
            name: function() { return $translate.instant('edge.edge-instances') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-edges')
                } else {
                    return $translate.instant('customer.manage-customer-edges')
                }
            },
            icon: "router"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('customer.delete') },
            icon: "delete",
            isEnabled: function(customer) {
                return customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
            }
        }
    ];

    var vm = this;

    vm.types = types;

    vm.customerGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteCustomerTitle,
        deleteItemContentFunc: deleteCustomerText,
        deleteItemsTitleFunc: deleteCustomersTitle,
        deleteItemsActionTitleFunc: deleteCustomersActionTitle,
        deleteItemsContentFunc: deleteCustomersText,

        fetchItemsFunc: fetchCustomers,
        saveItemFunc: saveCustomer,
        deleteItemFunc: deleteCustomer,

        getItemTitleFunc: getCustomerTitle,

        itemCardTemplateUrl: customerCard,

        actionsList: customerActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addCustomerTemplate,

        addItemText: function() { return $translate.instant('customer.add-customer-text') },
        noItemsText: function() { return $translate.instant('customer.no-customers-text') },
        itemDetailsText: function(customer) {
            if (customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic)) {
                return $translate.instant('customer.customer-details')
            } else {
                return '';
            }
        },
        isSelectionEnabled: function (customer) {
            return customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
        },
        isDetailsReadOnly: function (customer) {
            return customer && customer.additionalInfo && customer.additionalInfo.isPublic;
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.customerGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.customerGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.openCustomerUsers = openCustomerUsers;
    vm.openCustomerAssets = openCustomerAssets;
    vm.openCustomerDevices = openCustomerDevices;
    vm.openCustomerDashboards = openCustomerDashboards;
    vm.openCustomerEdges = openCustomerEdges;

    function deleteCustomerTitle(customer) {
        return $translate.instant('customer.delete-customer-title', {customerTitle: customer.title});
    }

    function deleteCustomerText() {
        return $translate.instant('customer.delete-customer-text');
    }

    function deleteCustomersTitle(selectedCount) {
        return $translate.instant('customer.delete-customers-title', {count: selectedCount}, 'messageformat');
    }

    function deleteCustomersActionTitle(selectedCount) {
        return $translate.instant('customer.delete-customers-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteCustomersText() {
        return $translate.instant('customer.delete-customers-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchCustomers(pageLink) {
        return customerService.getCustomers(pageLink);
    }

    function saveCustomer(customer) {
        return customerService.saveCustomer(customer);
    }

    function deleteCustomer(customerId) {
        return customerService.deleteCustomer(customerId);
    }

    function getCustomerTitle(customer) {
        return customer ? customer.title : '';
    }

    function openCustomerUsers($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.users', {customerId: customer.id.id});
    }

    function openCustomerAssets($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.assets', {customerId: customer.id.id});
    }

    function openCustomerDevices($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.devices', {customerId: customer.id.id});
    }

    function openCustomerDashboards($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.dashboards', {customerId: customer.id.id});
    }

    function openCustomerEdges($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.edges', {customerId: customer.id.id});
    }

}
