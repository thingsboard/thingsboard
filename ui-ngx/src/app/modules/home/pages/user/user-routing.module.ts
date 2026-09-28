// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route, RouterModule } from '@angular/router';
import { UsersTableConfigResolver } from '@modules/home/pages/user/users-table-config.resolver';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { EntityType } from '@shared/models/entity-type.models';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { MenuId } from '@core/services/menu.models';

const userRoute = (entityGroup: any, entitiesTableConfig: any): Route =>
  ({
    path: ':entityId',
    component: EntityDetailsPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.USER,
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'domain'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'user.user',
      hideTabs: true
    },
    resolve: {
      entityGroup,
      entitiesTableConfig
    }
  });

const userGroupsChildrenRoutes: Route[] = [
  {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.user-groups',
      groupType: EntityType.USER
    },
    resolve: {
      entityGroup: EntityGroupResolver,
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.USER,
      breadcrumb: {
        icon: 'account_circle',
        labelFunction: groupEntitiesLabelFunction
      } as BreadCrumbConfig<GroupEntitiesTableComponent>
    },
    children: [
      {
        path: '',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.user-group',
          groupType: EntityType.USER,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      userRoute(EntityGroupResolver, 'emptyUserTableConfigResolver')
    ]
  }
];

export const userGroupsRoute: Route = {
  path: 'groups',
  data: {
    groupType: EntityType.USER,
    breadcrumb: {
      menuId: MenuId.user_groups
    }
  },
  children: userGroupsChildrenRoutes
};

export const usersRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'users',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.users
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
      },
      {
        path: 'all',
        data: {
          groupType: EntityType.USER,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            menuId: MenuId.user_all
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'user.users'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver,
              entityGroup: EntityGroupResolver
            }
          },
          userRoute(EntityGroupResolver, UsersTableConfigResolver)
        ]
      },
      userGroupsRoute
    ]
  };
  return routeConfig;
};

@NgModule({
  imports: [RouterModule.forChild([usersRoute(true)])],
  exports: [RouterModule],
  providers: [
    UsersTableConfigResolver,
    {
      provide: 'emptyUserTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class UserRoutingModule { }
