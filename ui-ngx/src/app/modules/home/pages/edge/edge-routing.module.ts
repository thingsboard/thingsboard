// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route, RouterModule, Routes } from '@angular/router';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { RuleChainType } from '@shared/models/rule-chain.models';
import {
  importRuleChainBreadcumbLabelFunction,
  ruleChainBreadcumbLabelFunction,
  RuleChainImportGuard,
  RuleChainMetaDataResolver,
  RuleChainResolver,
  RuleNodeComponentsResolver,
  TooltipsterResolver
} from '@home/pages/rulechain/rulechain-routing.module';
import { ConvertersTableConfigResolver } from '@home/pages/converter/converters-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { IntegrationsTableConfigResolver } from '@home/pages/integration/integrations-table-config.resolver';
import { EntityType } from '@shared/models/entity-type.models';
import { dashboardGroupsRoute } from '@home/pages/dashboard/dashboard-routing.module';
import { userGroupsRoute } from '@home/pages/user/user-routing.module';
import { assetGroupsRoute } from '@home/pages/asset/asset-routing.module';
import { deviceGroupsRoute } from '@home/pages/device/device-routing.module';
import { entityViewGroupsRoute } from '@home/pages/entity-view/entity-view-routing.module';
import { edgeEntitiesTitle, entityGroupsTitle, resolveGroupParams } from '@shared/models/entity-group.models';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupResolver } from '@home/pages/group/entity-group.shared';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { SchedulerEventsComponent } from '@home/components/scheduler/scheduler-events.component';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { EdgeService } from '@core/http/edge.service';
import { MenuId } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestEdgeComponent } from '@home/pages/edge/request-edge.component';

@Injectable()
export class EdgeTitleResolver  {

  constructor(private edgeService: EdgeService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<string> {
    const params = resolveGroupParams(route);
    if (params.edgeId) {
      return this.edgeService.getEdge(params.edgeId).pipe(
        map((edge) => edge.name)
      );
    } else {
      return of(null);
    }
  }
}

const disabledEdgeReplaceComponentFunction = (store: Store<AppState>) => {
  const authState = getCurrentAuthState(store);
  if (authState.licenseVersion > 1 && !authState.edgeEnabled) {
    return RequestEdgeComponent;
  } else {
    return null;
  }
}

const edgeRoute = (entityGroup: any, entitiesTableConfig: any): Route =>
  ({
    path: ':entityId',
    component: EntityDetailsPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.EDGE,
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'router'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'edge.edge',
      hideTabs: true
    },
    resolve: {
      entityGroup,
      entitiesTableConfig
    }
  });

const edgeSchedulerRoute: Route = {
  path: ':edgeId/scheduler',
  component: SchedulerEventsComponent,
  data: {
    edgeEntitiesType: EntityType.SCHEDULER_EVENT,
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    breadcrumb: {
      labelFunction: (route, translate, component, data) =>
        route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.SCHEDULER_EVENT)),
      icon: 'schedule'
    },
    backNavigationCommands: ['../..'],
    hideTabs: true
  },
  resolve: {
    entityGroup: EntityGroupResolver,
    edgeTitle: EdgeTitleResolver
  }
};

const edgeRuleChainsRoute: Route = {
  path: ':edgeId/ruleChains',
  data: {
    edgeEntitiesType: EntityType.RULE_CHAIN,
    breadcrumb: {
      labelFunction: (route, translate, component, data) =>
        route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.RULE_CHAIN)),
      icon: 'settings_ethernet'
    },
    backNavigationCommands: ['../..'],
    hideTabs: true
  },
  resolve: {
    edgeTitle: EdgeTitleResolver
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        title: 'edge.rulechains',
        auth: [Authority.TENANT_ADMIN],
        ruleChainsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: RuleChainsTableConfigResolver
      }
    },
    {
      path: ':ruleChainId',
      component: RuleChainPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: ruleChainBreadcumbLabelFunction,
          icon: 'settings_ethernet'
        } as BreadCrumbConfig<RuleChainPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'rulechain.edge-rulechain',
        import: false,
        ruleChainType: RuleChainType.EDGE,
        ruleChainsType: 'edge'
      },
      loadChildren: () => import('../rulechain/rulechain-page.module').then(m => m.RuleChainPageModule),
      resolve: {
        entityGroup: EntityGroupResolver,
        ruleChain: RuleChainResolver,
        ruleChainMetaData: RuleChainMetaDataResolver,
        ruleNodeComponents: RuleNodeComponentsResolver,
        tooltipster: TooltipsterResolver
      }
    }
  ]
};

const edgeIntegrationsRoute: Route = {
  path: ':edgeId/integrations',
  data: {
    edgeEntitiesType: EntityType.INTEGRATION,
    breadcrumb: {
      labelFunction: (route, translate, component, data) =>
        route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.INTEGRATION)),
      icon: 'input'
    },
    backNavigationCommands: ['../..'],
    hideTabs: true
  },
  resolve: {
    edgeTitle: EdgeTitleResolver
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        title: 'edge.integrations',
        auth: [Authority.TENANT_ADMIN],
        integrationsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
    },
    {
      path: ':entityId',
      component: EntityDetailsPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: entityDetailsPageBreadcrumbLabelFunction,
          icon: 'input'
        } as BreadCrumbConfig<EntityDetailsPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.integration-templates',
        integrationsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
    }
  ]
};

const edgeChildrenRoutes = (): Routes =>
  ([
    { ...userGroupsRoute, ...{
        path: ':edgeId/userGroups',
        data: {
          edgeEntitiesType: EntityType.USER,
          groupType: EntityType.USER,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.USER)),
            icon: 'account_circle'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...assetGroupsRoute, ...{
        path: ':edgeId/assetGroups',
        data: {
          edgeEntitiesType: EntityType.ASSET,
          groupType: EntityType.ASSET,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.ASSET)),
            icon: 'domain'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...deviceGroupsRoute, ...{
        path: ':edgeId/deviceGroups',
        data: {
          edgeEntitiesType: EntityType.DEVICE,
          groupType: EntityType.DEVICE,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.DEVICE)),
            icon: 'devices_other'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...entityViewGroupsRoute, ...{
        path: ':edgeId/entityViewGroups',
        data: {
          edgeEntitiesType: EntityType.ENTITY_VIEW,
          groupType: EntityType.ENTITY_VIEW,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.ENTITY_VIEW)),
            icon: 'view_quilt'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...dashboardGroupsRoute, ...{
        path: ':edgeId/dashboardGroups',
        data: {
          edgeEntitiesType: EntityType.DASHBOARD,
          groupType: EntityType.DASHBOARD,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.DASHBOARD)),
            icon: 'dashboard'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    edgeSchedulerRoute,
    edgeRuleChainsRoute,
    edgeIntegrationsRoute
  ]);

const edgeGroupsChildrenRoutesTemplate = (root: boolean, shared: boolean): Routes => {
  const routes: Routes = [];
  const groupsRoute: Route = {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: entityGroupsTitle(EntityType.EDGE, shared),
      groupType: EntityType.EDGE
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  };
  if (!root) {
    groupsRoute.resolve.entityGroup = EntityGroupResolver;
  }
  routes.push(groupsRoute);

  const edgeEntitiesRoute: Route = {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.EDGE,
      breadcrumb: {
        labelFunction: root ?
          (route, translate, component, data) =>
            data.entityGroup.parentEntityGroup ? data.entityGroup.parentEntityGroup.name :
            (component && component.entityGroup ? component.entityGroup.name : data.entityGroup.name) :
          (route, translate, component, data) =>
            data.entityGroup.edgeGroupName ? data.entityGroup.edgeGroupName : data.entityGroup.name,
        icon: 'router'
      } as BreadCrumbConfig<GroupEntitiesTableComponent>
    },
    children: [
      {
        path: '',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.edge-group',
          groupType: EntityType.EDGE,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      ...edgeChildrenRoutes(),
      edgeRoute(EntityGroupResolver, 'emptyEdgeTableConfigResolver')
    ]
  };
  routes.push(edgeEntitiesRoute);
  return routes;
};

const edgeGroupsRoute = (root: boolean): Route => ({
  path: 'groups',
  data: {
    groupType: EntityType.EDGE,
    breadcrumb: {
      menuId: MenuId.edge_groups
    }
  },
  children: edgeGroupsChildrenRoutesTemplate(root, false)
});

const edgeSharedGroupsRoute = (root: boolean): Route => ({
  path: 'shared',
  data: {
    groupType: EntityType.EDGE,
    shared: true,
    breadcrumb: {
      menuId: MenuId.edge_shared
    }
  },
  children: edgeGroupsChildrenRoutesTemplate(root, true)
});

const edgeRuleChainTemplatesRoute: Route = {
  path: 'ruleChains',
  data: {
    auth: [Authority.TENANT_ADMIN],
    breadcrumb: {
      label: 'edge.rule-chain',
      icon: 'settings_ethernet'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.rulechain-templates',
        ruleChainsType: 'edges',
        replaceComponent: disabledEdgeReplaceComponentFunction
      },
      resolve: {
        entitiesTableConfig: RuleChainsTableConfigResolver
      }
    },
    {
      path: ':ruleChainId',
      component: RuleChainPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: ruleChainBreadcumbLabelFunction,
          icon: 'settings_ethernet'
        } as BreadCrumbConfig<RuleChainPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'rulechain.edge-rulechain',
        import: false,
        ruleChainType: RuleChainType.EDGE
      },
      loadChildren: () => import('../rulechain/rulechain-page.module').then(m => m.RuleChainPageModule),
      resolve: {
        ruleChain: RuleChainResolver,
        ruleChainMetaData: RuleChainMetaDataResolver,
        ruleNodeComponents: RuleNodeComponentsResolver,
        tooltipster: TooltipsterResolver
      }
    },
    {
      path: 'ruleChain/import',
      component: RuleChainPageComponent,
      canActivate: [RuleChainImportGuard],
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: importRuleChainBreadcumbLabelFunction,
          icon: 'settings_ethernet'
        } as BreadCrumbConfig<RuleChainPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'rulechain.edge-rulechain',
        import: true,
        ruleChainType: RuleChainType.EDGE
      },
      loadChildren: () => import('../rulechain/rulechain-page.module').then(m => m.RuleChainPageModule),
      resolve: {
        ruleNodeComponents: RuleNodeComponentsResolver,
        tooltipster: TooltipsterResolver
      }
    }
  ]
};

const edgeConverterTemplatesRoute: Route = {
  path: 'converters',
  data: {
    auth: [Authority.TENANT_ADMIN],
    breadcrumb: {
      label: 'edge.converter',
      icon: 'transform'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.converter-templates',
        convertersType: 'edges',
        replaceComponent: disabledEdgeReplaceComponentFunction
      },
      resolve: {
        entitiesTableConfig: ConvertersTableConfigResolver
      }
    },
    {
      path: ':entityId',
      component: EntityDetailsPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: entityDetailsPageBreadcrumbLabelFunction,
          icon: 'transform'
        } as BreadCrumbConfig<EntityDetailsPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.converter-templates'
      },
      resolve: {
        entitiesTableConfig: ConvertersTableConfigResolver
      }
    }
  ]
};

const edgeIntegrationTemplatesRoute: Route = {
  path: 'integrations',
  data: {
    auth: [Authority.TENANT_ADMIN],
    breadcrumb: {
      label: 'edge.integration',
      icon: 'input'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.integration-templates',
        integrationsType: 'edges',
        replaceComponent: disabledEdgeReplaceComponentFunction
      },
      resolve: {
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
    },
    {
      path: ':entityId',
      component: EntityDetailsPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: entityDetailsPageBreadcrumbLabelFunction,
          icon: 'input'
        } as BreadCrumbConfig<EntityDetailsPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.integration-templates',
        integrationsType: 'edges'
      },
      resolve: {
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
    }
  ]
};

const edgeTemplatesRoute: Route = {
  path: 'templates',
  component: RouterTabsComponent,
  data: {
    auth: [Authority.TENANT_ADMIN],
    useChildrenRoutesForTabs: true,
    breadcrumb: {
      menuId: MenuId.edge_templates
    }
  },
  children: [
    {
      path: '',
      pathMatch: 'full',
      redirectTo: 'ruleChains'
    },
    edgeRuleChainTemplatesRoute,
    edgeIntegrationTemplatesRoute,
    edgeConverterTemplatesRoute
  ]
};

export const edgesRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'edgeManagement',
    data: {
      breadcrumb: root ? {
        menuId: MenuId.edge_management
      } : { skip: true }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: 'edges'
        }
      }
    ]
  };
  const edgesRouteSection: Route = {
    path: 'edges',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        labelFunction: (route, translate) =>
          (route.data.customerTitle ? (route.data.customerTitle + ': ') : '') +
          translate.instant(root ? 'edge.edges' : 'edge.edge-instances'),
        icon: 'router'
      },
      replaceComponent: disabledEdgeReplaceComponentFunction
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
  const allEdgesRoute: Route = {
    path: 'all',
    data: {
      groupType: EntityType.EDGE,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.edge_all
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'edge.edge-instances'
        },
        resolve: {
          entitiesTableConfig: EdgesTableConfigResolver,
          entityGroup: EntityGroupResolver
        }
      },
      edgeRoute(EntityGroupResolver, EdgesTableConfigResolver),
      ...edgeChildrenRoutes()
    ]
  };
  routeConfig.children.push(edgesRouteSection);
  edgesRouteSection.children.push(allEdgesRoute);
  edgesRouteSection.children.push(edgeGroupsRoute(root));
  if (root) {
    edgesRouteSection.children.push(edgeSharedGroupsRoute(root));
    routeConfig.children.push(edgeTemplatesRoute);
  }
  return routeConfig;
};


@NgModule({
  imports: [RouterModule.forChild([edgesRoute(true)])],
  exports: [RouterModule],
  providers: [
    EdgesTableConfigResolver,
    EdgeTitleResolver,
    {
      provide: 'emptyEdgeTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class EdgeRoutingModule {
}
