// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { of } from 'rxjs';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { UsersTableConfigResolver } from '@home/pages/user/users-table-config.resolver';
import { isDefined } from '@core/utils';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

import _ from 'lodash';
import { EntityGroupResolver } from '@home/pages/group/entity-group.shared';

@Injectable()
export class RedirectToEntityGroup  {
  constructor(private router: Router,
              private entityGroupService: EntityGroupService,
              private store: Store<AppState>) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const groupType: EntityType = route.data.groupType;
    const entityId: string = route.params.entityId;
    if (isDefined(groupType) && isDefined(entityId)) {
      const authState = getCurrentAuthState(this.store);
      if (groupType === EntityType.USER && authState.authUser.authority === Authority.SYS_ADMIN) {
        return true;
      }
      const entitiesUrl = _.camelCase(groupType) + 's';
      return of(this.router.parseUrl(`${entitiesUrl}/all/${entityId}`));
    }
    this.router.navigate(['/']);
    return false;
  }

}

const redirectEntityDetailsRoutes: Routes = [
  {
    path: 'entities/devices/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.DEVICE
    }
  },
  {
    path: 'entities/assets/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.ASSET
    }
  },
  {
    path: 'entities/entityViews/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.ENTITY_VIEW
    }
  },
  {
    path: 'customers/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.CUSTOMER
    }
  },
  {
    path: 'edgeManagement/edges/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.EDGE
    }
  },
  {
    path: 'users/:entityId',
    pathMatch: 'full',
    component: EntityDetailsPageComponent,
    canActivate: [RedirectToEntityGroup],
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'account_circle'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'user.user',
      groupType: EntityType.USER
    },
    resolve: {
      entitiesTableConfig: UsersTableConfigResolver
    }
  }
];

const redirectDashboardGroupsRoutes: Routes = [
  {
    path: 'dashboardGroups',
    pathMatch: 'full',
    redirectTo: '/dashboards/groups'
  },
  {
    path: 'dashboardGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/dashboards/groups/:entityGroupId'
  },
  {
    path: 'dashboardGroups/:entityGroupId/:dashboardId',
    redirectTo: 'dashboards/groups/:entityGroupId/:dashboardId'
  }
];

const redirectDeviceGroupsRoutes: Routes = [
  {
    path: 'deviceGroups',
    pathMatch: 'full',
    redirectTo: '/entities/devices/groups'
  },
  {
    path: 'deviceGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/entities/devices/groups/:entityGroupId'
  },
  {
    path: 'deviceGroups/:entityGroupId/:entityId',
    redirectTo: '/entities/devices/groups/:entityGroupId/:entityId'
  }
];

const redirectAssetGroupsRoutes: Routes = [
  {
    path: 'assetGroups',
    pathMatch: 'full',
    redirectTo: '/entities/assets/groups'
  },
  {
    path: 'assetGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/entities/assets/groups/:entityGroupId'
  },
  {
    path: 'assetGroups/:entityGroupId/:entityId',
    redirectTo: '/entities/assets/groups/:entityGroupId/:entityId'
  }
];

const redirectEntityViewGroupsRoutes: Routes = [
  {
    path: 'entityViewGroups',
    pathMatch: 'full',
    redirectTo: '/entities/entityViews/groups'
  },
  {
    path: 'entityViewGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/entities/entityViews/groups/:entityGroupId'
  },
  {
    path: 'entityViewGroups/:entityGroupId/:entityId',
    redirectTo: '/entities/entityViews/groups/:entityGroupId/:entityId'
  }
];

const redirectCustomerGroupsRoutes: Routes = [
  {
    path: 'customerGroups',
    pathMatch: 'full',
    redirectTo: '/customers/groups'
  },
  {
    path: 'customerGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/customers/groups/:entityGroupId'
  },
  {
    path: 'customerGroups/:entityGroupId/:entityId',
    redirectTo: '/customers/groups/:entityGroupId/:entityId'
  }
];

const redirectCustomersHierarchyRoutes: Routes = [
  {
    path: 'customersHierarchy',
    pathMatch: 'full',
    redirectTo: '/customers/hierarchy'
  }
];

const redirectUserGroupsRoutes: Routes = [
  {
    path: 'userGroups',
    pathMatch: 'full',
    redirectTo: '/users/groups'
  },
  {
    path: 'userGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/users/groups/:entityGroupId'
  },
  {
    path: 'userGroups/:entityGroupId/:entityId',
    redirectTo: '/users/groups/:entityGroupId/:entityId'
  }
];

const redirectEdgeGroupsRoutes: Routes = [
  {
    path: 'edgeGroups',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/edges/groups'
  },
  {
    path: 'edgeGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/edges/groups/:entityGroupId'
  },
  {
    path: 'edgeGroups/:entityGroupId/:entityId',
    redirectTo: '/edgeManagement/edges/groups/:entityGroupId/:entityId'
  }
];

const redirectEdgeInstancesRoutes: Routes = [
  {
    path: 'edgeManagement/instances',
    children: [
      {
        path: '**',
        redirectTo: ({ url }) => ['/edgeManagement/edges', ...url.map(segment => segment.path)].join('/')
      }
    ]
  }
];

const routes: Routes = [
  ...redirectDashboardGroupsRoutes,
  ...redirectDeviceGroupsRoutes,
  ...redirectAssetGroupsRoutes,
  ...redirectEntityViewGroupsRoutes,
  ...redirectCustomerGroupsRoutes,
  ...redirectCustomersHierarchyRoutes,
  ...redirectUserGroupsRoutes,
  ...redirectEdgeGroupsRoutes,
  ...redirectEdgeInstancesRoutes,
  ...redirectEntityDetailsRoutes
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EntityGroupResolver,
    RedirectToEntityGroup,
    {
      provide: 'emptyEntityGroupResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class EntityGroupRoutingModule { }
