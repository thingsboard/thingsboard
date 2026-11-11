// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { CustomersTableConfigResolver } from './customers-table-config.resolver';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { entitiesRoute } from '@home/pages/entities/entities-routing.module';
import { dashboardsRoute } from '@home/pages/dashboard/dashboard-routing.module';
import { CustomersHierarchyComponent } from '@home/pages/customer/customers-hierarchy.component';
import { CustomerTitleResolver } from '@home/pages/customer/customer.shared';
import { usersRoute } from '@home/pages/user/user-routing.module';
import { entityGroupsTitle } from '@shared/models/entity-group.models';
import { edgesRoute } from '@home/pages/edge/edge-routing.module';
import { agentsRoute } from '@home/pages/agent/agent-routing.module';
import { MenuId } from '@core/services/menu.models';

const customerRoute = (entityGroup: any, entitiesTableConfig: any): Route =>
  ({
    path: ':entityId',
    component: EntityDetailsPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.CUSTOMER,
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'supervisor_account'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'customer.customer',
      hideTabs: true
    },
    resolve: {
      entityGroup,
      entitiesTableConfig
    }
  });

const customerChildrenRoutes = (): Routes =>
  ([
    { ...customersRoute(), ...{
        path: ':customerId/customers',
        data: {
          breadcrumb: {
            labelFunction: (route, translate) =>
              route.data.customerTitle + ': ' + translate.instant('customer.customers'),
            icon: 'supervisor_account'
          },
          backNavigationCommands: ['../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    },
    { ...entitiesRoute(), ...{
        path: ':customerId/entities',
        data: {
          backNavigationCommands: ['../../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    },
    { ...dashboardsRoute(), ...{
        path: ':customerId/dashboards',
        data: {
          breadcrumb: {
            labelFunction: (route, translate) =>
              route.data.customerTitle + ': ' + translate.instant('dashboard.dashboards'),
            icon: 'dashboards'
          },
          backNavigationCommands: ['../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    },
    { ...usersRoute(), ...{
        path: ':customerId/users',
        data: {
          breadcrumb: {
            labelFunction: (route, translate) =>
              route.data.customerTitle + ': ' + translate.instant('user.users'),
            icon: 'account_circle'
          },
          backNavigationCommands: ['../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    },
    (() => {
        const edges = edgesRoute();
        return {
          ...edges,
          path: ':customerId/edgeManagement',
          data: {
            backNavigationCommands: ['../../../..']
          },
          resolve: {
            customerTitle: CustomerTitleResolver
          },
          children: [...edges.children, agentsRoute()]
        };
    })(),
]);

const customerGroupsChildrenRoutesTemplate = (root: boolean, shared: boolean): Routes => {
  const routes: Routes = [];
  const groupsRoute: Route = {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: entityGroupsTitle(EntityType.CUSTOMER, shared),
      groupType: EntityType.CUSTOMER
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  };
  if (!root) {
    groupsRoute.resolve.entityGroup = EntityGroupResolver;
  }
  routes.push(groupsRoute);

  const customerEntitiesRoute: Route = {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.CUSTOMER,
      breadcrumb: {
        icon: 'supervisor_account'
      } as BreadCrumbConfig<GroupEntitiesTableComponent>
    },
    children: [
      {
        path: '',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.customer-group',
          groupType: EntityType.CUSTOMER,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      customerRoute(EntityGroupResolver, 'emptyCustomerTableConfigResolver')
    ]
  };
  if (root) {
    customerEntitiesRoute.data.breadcrumb.labelFunction = (route, translate, component, data) => data.entityGroup.parentEntityGroup ?
        data.entityGroup.parentEntityGroup.name :
        (component && component.entityGroup ? component.entityGroup.name : data.entityGroup.name);
  } else {
    customerEntitiesRoute.data.breadcrumb.labelFunction = groupEntitiesLabelFunction;
  }
  if (root) {
    customerEntitiesRoute.children.push(
      ...customerChildrenRoutes()
    );
  }
  routes.push(customerEntitiesRoute);
  return routes;
};

const customerGroupsRoute = (root: boolean): Route => ({
  path: 'groups',
  data: {
    groupType: EntityType.CUSTOMER,
    breadcrumb: {
      menuId: MenuId.customer_groups
    }
  },
  children: customerGroupsChildrenRoutesTemplate(root, false)
});

const customerSharedGroupsRoute = (root: boolean): Route => ({
  path: 'shared',
  data: {
    groupType: EntityType.CUSTOMER,
    shared: true,
    breadcrumb: {
      menuId: MenuId.customer_shared
    }
  },
  children: customerGroupsChildrenRoutesTemplate(root, true)
});

const customersHierarchyRoute: Route = {
  path: 'hierarchy',
  component: CustomersHierarchyComponent,
  data: {
    breadcrumb: {
      menuId: MenuId.customers_hierarchy
    },
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    title: 'customers-hierarchy.customers-hierarchy'
  }
};

export const customersRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'customers',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.customers
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: 'all'
        }
      }
    ]
  };
  const allCustomersRoute: Route = {
    path: 'all',
    data: {
      groupType: EntityType.CUSTOMER,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.customer_all
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver,
          entityGroup: EntityGroupResolver
        }
      },
      customerRoute(EntityGroupResolver, CustomersTableConfigResolver)
    ]
  };
  if (root) {
    allCustomersRoute.children.push(
      ...customerChildrenRoutes()
    );
  }
  routeConfig.children.push(allCustomersRoute);
  routeConfig.children.push(customerGroupsRoute(root));
  if (root) {
    routeConfig.children.push(customerSharedGroupsRoute(root));
    routeConfig.children.push(customersHierarchyRoute);
  }
  return routeConfig;
};

@NgModule({
  imports: [RouterModule.forChild([customersRoute(true)])],
  exports: [RouterModule],
  providers: [
    CustomersTableConfigResolver,
    {
      provide: 'emptyCustomerTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    },
    CustomerTitleResolver
  ]
})
export class CustomerRoutingModule { }
