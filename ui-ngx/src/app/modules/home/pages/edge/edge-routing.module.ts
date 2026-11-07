// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
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
import { entityGroupsTitle } from '@shared/models/entity-group.models';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupResolver } from '@home/pages/group/entity-group.shared';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { MenuId } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestEdgeComponent } from '@home/pages/edge/request-edge.component';

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
      edgeRoute(EntityGroupResolver, EdgesTableConfigResolver)
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
    {
      provide: 'emptyEdgeTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class EdgeRoutingModule {
}
