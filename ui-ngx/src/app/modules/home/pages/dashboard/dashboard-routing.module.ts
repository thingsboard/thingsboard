// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { DashboardsTableConfigResolver } from './dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityType } from '@shared/models/entity-type.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { mergeMap, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { entityGroupsTitle } from '@shared/models/entity-group.models';
import { UserSettingsService } from '@core/http/user-settings.service';
import { UserDashboardAction } from '@shared/models/user-settings.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Operation, Resource } from '@shared/models/security.models';
import { MenuId } from '@core/services/menu.models';

@Injectable()
export class DashboardResolver  {

  constructor(private store: Store<AppState>,
              private dashboardService: DashboardService,
              private userSettingService: UserSettingsService,
              private dashboardUtils: DashboardUtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Dashboard> {
    const dashboardId = route.params.dashboardId;
    return this.dashboardService.getDashboard(dashboardId).pipe(
      mergeMap((dashboard) =>
        (getCurrentAuthUser(this.store).isPublic ? of(null) :
          this.userSettingService.reportUserDashboardAction(dashboardId, UserDashboardAction.VISIT,
            {ignoreLoading: true, ignoreErrors: true})).pipe(
          catchError(() => of(dashboard)),
          map(() => dashboard)
        )),
      map((dashboard) => this.dashboardUtils.validateAndUpdateDashboard(dashboard))
    );
  }
}

const dashboardRoute = (entityGroup: any, singlePageMode = false, isAllPage = false): Route =>
  ({
    path: ':dashboardId',
    component: DashboardPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.DASHBOARD,
      breadcrumb: {
        icon: 'dashboard'
      } as BreadCrumbConfig<DashboardPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      canActivate$: (userPermissionsService: UserPermissionsService, params: any): Observable<boolean> => {
        if (isAllPage) {
          return of(userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD));
        } else if (entityGroup === 'emptyEntityGroupResolver') {
          if (userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)) {
            return of(true);
          }
          return userPermissionsService.hasEntityPermission({entityType: EntityType.DASHBOARD, id: params.dashboardId}, Operation.READ);
        } else {
          return of(userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD));
        }
      },
      title: 'dashboard.dashboard',
      hideTabs: true,
      widgetEditMode: false,
      singlePageMode
    },
    resolve: {
      dashboard: DashboardResolver,
      entityGroup
    }
  });

const dashboardGroupsChildrenRoutesTemplate = (shared: boolean): Routes => [
  {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: entityGroupsTitle(EntityType.DASHBOARD, shared),
      groupType: EntityType.DASHBOARD
    },
    resolve: {
      entityGroup: EntityGroupResolver,
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.DASHBOARD,
      breadcrumb: {
        icon: 'dashboard',
        labelFunction: groupEntitiesLabelFunction
      } as BreadCrumbConfig<GroupEntitiesTableComponent>
    },
    children: [
      {
        path: '',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.dashboard-group',
          groupType: EntityType.DASHBOARD,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      dashboardRoute(EntityGroupResolver, false)
    ]
  }
];

export const dashboardGroupsRoute: Route = {
  path: 'groups',
  data: {
    groupType: EntityType.DASHBOARD,
    breadcrumb: {
      menuId: MenuId.dashboard_groups
    }
  },
  children: dashboardGroupsChildrenRoutesTemplate(false)
};

const dashboardSharedGroupsRoute: Route = {
  path: 'shared',
  data: {
    groupType: EntityType.DASHBOARD,
    shared: true,
    breadcrumb: {
      menuId: MenuId.dashboard_shared
    }
  },
  children: dashboardGroupsChildrenRoutesTemplate(true)
};

export const dashboardsRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'dashboards',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.dashboards
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
          groupType: EntityType.DASHBOARD,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            menuId: MenuId.dashboard_all
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'dashboard.dashboards'
            },
            resolve: {
              entitiesTableConfig: DashboardsTableConfigResolver,
              entityGroup: EntityGroupResolver
            }
          },
          dashboardRoute(EntityGroupResolver, false, true)
        ]
      },
      dashboardGroupsRoute
    ]
  };
  if (root) {
    routeConfig.children.push(dashboardSharedGroupsRoute);
  }
  routeConfig.children.push(dashboardRoute('emptyEntityGroupResolver', true));
  return routeConfig;
};

// @dynamic
@NgModule({
  imports: [RouterModule.forChild([dashboardsRoute(true)])],
  exports: [RouterModule],
  providers: [
    DashboardsTableConfigResolver,
    DashboardResolver
  ]
})
export class DashboardRoutingModule { }
