// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Route, RouterModule, Routes } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { AgentService } from '@core/http/agent.service';
import { AgentInfo } from '@shared/models/agent.models';
import { resolveAgentIdParam } from '@home/pages/agent/util/agent-route-params';
import { MenuId } from '@core/services/menu.models';
import { AgentsTableConfigResolver } from '@home/pages/agent/agents-table-config.resolver';
import { AgentProfilesTableConfigResolver } from '@home/pages/agent/agent-profiles-table-config.resolver';
import { AgentAppProfilesTableConfigResolver } from '@home/pages/agent/agent-app-profiles-table-config.resolver';
import { AgentApplicationsTableConfigResolver } from '@home/pages/agent/agent-applications-table-config.resolver';
import { AgentEventsPageComponent } from '@home/pages/agent/agent-events-page.component';
import { AgentBulkActionEventsPageComponent } from '@home/pages/agent/agent-bulk-action-events-page.component';
import {
  AgentAppUnitLogViewerPageComponent
} from '@home/pages/agent/log-viewer/agent-app-unit-log-viewer-page.component';
import {
  AgentApplicationsPageComponent
} from '@home/pages/agent/metrics/agent-applications-page.component';
import {
  AgentApplicationDetailsPageComponent
} from '@home/pages/agent/metrics/agent-application-details-page.component';
import { AgentAppProfilesPageComponent } from '@home/pages/agent/agent-app-profiles-page.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupResolver } from '@home/pages/group/entity-group.shared';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { entityGroupsTitle } from '@shared/models/entity-group.models';

// Resolves the agent for the `:agentId` tabs breadcrumb so its name survives
// across the applications/events/units tabs (none of those page components
// expose the agent themselves).
const agentInfoBreadcrumbResolver: ResolveFn<AgentInfo | null> = (route) => {
  const agentId = resolveAgentIdParam(route);
  return agentId
    ? inject(AgentService).getAgentInfoById(agentId).pipe(catchError(() => of(null)))
    : of(null);
};

const agentTabsBreadcrumbLabelFunction: BreadCrumbLabelFunction<RouterTabsComponent> =
  (route, translate, component, data, utils) => {
    const name = (route?.data?.agent as AgentInfo)?.name ?? '';
    return utils ? utils.customTranslation(name, name) : name;
  };

// Group-name crumb for the agent group/shared hierarchy. Unlike the shared
// groupEntitiesLabelFunction (which reads the active component's entityGroup),
// this reads the entityGroup resolved on the :entityGroupId route, so it also
// works when the active component is an agent tab page that has no entityGroup.
const agentGroupBreadcrumbLabelFunction: BreadCrumbLabelFunction<GroupEntitiesTableComponent> =
  (route, translate, component, data, utils) => {
    const name = component?.entityGroup?.name ?? route?.data?.entityGroup?.name ?? '';
    return utils ? utils.customTranslation(name, name) : name;
  };

// Per-agent detail route used under /agents/all/:agentId — opens the existing
// tabbed view (applications, events). Split out so both the flat /all branch
// and group-scoped /groups/:entityGroupId branch can mount it.
const agentTabsChildren = (entitiesTableConfig: any): Route => ({
  path: ':agentId',
  component: RouterTabsComponent,
  data: {
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    useChildrenRoutesForTabs: true,
    breadcrumb: {
      labelFunction: agentTabsBreadcrumbLabelFunction,
      icon: 'memory'
    } as BreadCrumbConfig<RouterTabsComponent>
  },
  resolve: {
    agent: agentInfoBreadcrumbResolver
  },
  children: [
    {
      path: '',
      pathMatch: 'full',
      redirectTo: 'applications'
    },
    {
      path: 'applications',
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        breadcrumb: {
          label: 'agent.applications',
          icon: 'apps'
        }
      },
      children: [
        {
          path: '',
          component: AgentApplicationsPageComponent,
          data: {
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'agent.applications',
            isPage: true
          },
          resolve: {
            entitiesTableConfig: AgentApplicationsTableConfigResolver
          }
        },
        {
          path: ':entityId',
          component: AgentApplicationDetailsPageComponent,
          canDeactivate: [ConfirmOnExitGuard],
          data: {
            breadcrumb: {
              labelFunction: entityDetailsPageBreadcrumbLabelFunction,
              icon: 'apps'
            } as BreadCrumbConfig<EntityDetailsPageComponent>,
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'agent.applications'
          },
          resolve: {
            entitiesTableConfig: AgentApplicationsTableConfigResolver
          }
        },
        {
          path: ':applicationId/units/:unitId/logs',
          component: AgentAppUnitLogViewerPageComponent,
          data: {
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'agent.app-unit-logs',
            isPage: true,
            breadcrumb: {
              label: 'agent.app-unit-logs',
              icon: 'description'
            }
          }
        }
      ]
    },
    {
      path: 'events',
      component: AgentEventsPageComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'agent.app-events',
        isPage: true,
        breadcrumb: {
          label: 'agent.app-events',
          icon: 'history'
        }
      }
    }
  ]
});

// Leaf route for an individual agent's detail (name/key/secret) inside an
// entity-group scope. `pathMatch: 'full'` so only the bare agent detail matches
// here; the applications/events/units hierarchy is served by agentTabsChildren
// mounted as a sibling so the group scope is preserved in the URL.
const agentGroupLeaf = (entityGroup: any, entitiesTableConfig: any): Route => ({
  path: ':entityId',
  pathMatch: 'full',
  component: EntityDetailsPageComponent,
  canDeactivate: [ConfirmOnExitGuard],
  data: {
    groupType: EntityType.AGENT,
    breadcrumb: {
      labelFunction: entityDetailsPageBreadcrumbLabelFunction,
      icon: 'memory'
    } as BreadCrumbConfig<EntityDetailsPageComponent>,
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    title: 'agent.agents',
    hideTabs: true
  },
  resolve: {
    entityGroup,
    entitiesTableConfig
  }
});

const agentGroupsChildrenRoutesTemplate = (shared: boolean): Routes => [
  {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: entityGroupsTitle(EntityType.AGENT, shared),
      groupType: EntityType.AGENT
    },
    resolve: {
      entityGroup: EntityGroupResolver,
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.AGENT,
      breadcrumb: {
        icon: 'memory',
        labelFunction: agentGroupBreadcrumbLabelFunction
      } as BreadCrumbConfig<GroupEntitiesTableComponent>
    },
    resolve: {
      entityGroup: EntityGroupResolver
    },
    children: [
      {
        path: '',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'agent.group',
          groupType: EntityType.AGENT,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      agentGroupLeaf(EntityGroupResolver, 'emptyAgentTableConfigResolver'),
      agentTabsChildren(AgentsTableConfigResolver)
    ]
  }
];

export const agentsRoute = (): Route => ({
  path: 'agents',
  component: RouterTabsComponent,
  data: {
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    breadcrumb: {
      menuId: MenuId.agents
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
        groupType: EntityType.AGENT,
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        breadcrumb: {
          menuId: MenuId.agent_all
        }
      },
      children: [
        {
          path: '',
          component: EntitiesTableComponent,
          data: {
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'agent.agents',
            agentsType: 'tenant'
          },
          resolve: {
            entitiesTableConfig: AgentsTableConfigResolver,
            entityGroup: EntityGroupResolver
          }
        },
        {
          path: ':entityId',
          pathMatch: 'full',
          component: EntityDetailsPageComponent,
          canDeactivate: [ConfirmOnExitGuard],
          data: {
            groupType: EntityType.AGENT,
            breadcrumb: {
              labelFunction: entityDetailsPageBreadcrumbLabelFunction,
              icon: 'memory'
            } as BreadCrumbConfig<EntityDetailsPageComponent>,
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'agent.agents',
            agentsType: 'tenant'
          },
          resolve: {
            entitiesTableConfig: AgentsTableConfigResolver,
            entityGroup: EntityGroupResolver
          }
        },
        agentTabsChildren(AgentsTableConfigResolver)
      ]
    },
    {
      path: 'groups',
      data: {
        groupType: EntityType.AGENT,
        breadcrumb: {
          menuId: MenuId.agent_groups
        }
      },
      children: agentGroupsChildrenRoutesTemplate(false)
    },
    {
      path: 'shared',
      data: {
        groupType: EntityType.AGENT,
        shared: true,
        breadcrumb: {
          menuId: MenuId.agent_shared
        }
      },
      children: agentGroupsChildrenRoutesTemplate(true)
    }
  ]
});

const routes: Routes = [
  {
    path: 'edgeManagement',
    data: {
      breadcrumb: {
        menuId: MenuId.edge_management
      }
    },
    children: [
      agentsRoute(),
      {
        path: 'profiles',
        component: RouterTabsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          useChildrenRoutesForTabs: true,
          breadcrumb: {
            menuId: MenuId.edge_profiles
          }
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'agent'
          },
          {
            path: 'agent',
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              breadcrumb: {
                label: 'agent.agent-profiles',
                icon: 'group_work'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'agent.agent-profiles'
                },
                resolve: {
                  entitiesTableConfig: AgentProfilesTableConfigResolver
                }
              },
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'group_work'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'agent.agent-profiles'
                },
                resolve: {
                  entitiesTableConfig: AgentProfilesTableConfigResolver
                }
              },
              {
                path: 'bulk/:bulkActionId',
                component: AgentBulkActionEventsPageComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'agent.bulk-action-details',
                  isPage: true,
                  breadcrumb: {
                    label: 'agent.bulk-action-details',
                    icon: 'playlist_play'
                  }
                }
              }
            ]
          },
          {
            path: 'application',
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              breadcrumb: {
                label: 'agent.app-profiles',
                icon: 'description'
              }
            },
            children: [
              {
                path: '',
                component: AgentAppProfilesPageComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'agent.app-profiles'
                },
                resolve: {
                  entitiesTableConfig: AgentAppProfilesTableConfigResolver
                }
              },
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'description'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'agent.app-profiles'
                },
                resolve: {
                  entitiesTableConfig: AgentAppProfilesTableConfigResolver
                }
              }
            ]
          }
        ]
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AgentsTableConfigResolver,
    AgentProfilesTableConfigResolver,
    AgentAppProfilesTableConfigResolver,
    AgentApplicationsTableConfigResolver,
    {
      provide: 'emptyAgentTableConfigResolver',
      useValue: (_route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class AgentRoutingModule {
}
