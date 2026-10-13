// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { DevicesTableConfigResolver } from '@modules/home/pages/device/devices-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { CustomerTitleResolver } from '@home/pages/customer/customer.shared';
import { entityGroupsTitle } from '@shared/models/entity-group.models';
import { MenuId } from '@core/services/menu.models';

const deviceRoute = (entityGroup: any, entitiesTableConfig: any): Route =>
  ({
    path: ':entityId',
    component: EntityDetailsPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.DEVICE,
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'devices_other'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'device.device',
      hideTabs: true
    },
    resolve: {
      entityGroup,
      entitiesTableConfig
    }
  });

const deviceGroupsChildrenRoutesTemplate = (shared: boolean): Routes => [
  {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: entityGroupsTitle(EntityType.DEVICE, shared),
      groupType: EntityType.DEVICE
    },
    resolve: {
      entityGroup: EntityGroupResolver,
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.DEVICE,
      breadcrumb: {
        icon: 'devices_other',
        labelFunction: groupEntitiesLabelFunction
      } as BreadCrumbConfig<GroupEntitiesTableComponent>
    },
    children: [
      {
        path: '',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.device-group',
          groupType: EntityType.DEVICE,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      deviceRoute(EntityGroupResolver, 'emptyDeviceTableConfigResolver')
    ]
  }
];

export const deviceGroupsRoute: Route = {
  path: 'groups',
  data: {
    groupType: EntityType.DEVICE,
    breadcrumb: {
      menuId: MenuId.device_groups
    }
  },
  children: deviceGroupsChildrenRoutesTemplate(false)
};

const deviceSharedGroupsRoute: Route = {
  path: 'shared',
  data: {
    groupType: EntityType.DEVICE,
    shared: true,
    breadcrumb: {
      menuId: MenuId.device_shared
    }
  },
  children: deviceGroupsChildrenRoutesTemplate(true)
};

export const devicesRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'devices',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        labelFunction: (route, translate) =>
          (route.data.customerTitle ? (route.data.customerTitle + ': ') : '') + translate.instant('device.devices'),
        icon: 'devices_other'
      }
    },
    resolve: {
      customerTitle: CustomerTitleResolver
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: 'all'
        }
      },
      {
        path: 'all',
        data: {
          groupType: EntityType.DEVICE,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            menuId: MenuId.device_all
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'device.devices'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver,
              entityGroup: EntityGroupResolver
            }
          },
          deviceRoute(EntityGroupResolver, DevicesTableConfigResolver)
        ]
      },
      deviceGroupsRoute
    ]
  };
  if (root) {
    routeConfig.children.push(deviceSharedGroupsRoute);
  }
  return routeConfig;
};

const routes: Routes = [
  {
    path: 'devices',
    pathMatch: 'full',
    redirectTo: '/entities/devices'
  },
  {
    path: 'devices/all',
    pathMatch: 'full',
    redirectTo: '/entities/devices/all'
  },
  {
    path: 'devices/:entityId',
    redirectTo: '/entities/devices/all/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DevicesTableConfigResolver,
    {
      provide: 'emptyDeviceTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class DeviceRoutingModule { }
