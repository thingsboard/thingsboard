///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from "@angular/router";
import { EntitiesTableComponent } from "@home/components/entity/entities-table.component";
import { Authority } from "@shared/models/authority.enum";
import { EdgesTableConfigResolver } from "@home/pages/edge/edges-table-config.resolver"
import { AssetsTableConfigResolver } from "@home/pages/asset/assets-table-config.resolver";
import { DevicesTableConfigResolver } from "@home/pages/device/devices-table-config.resolver";
import { EntityViewsTableConfigResolver } from "@home/pages/entity-view/entity-views-table-config.resolver";
import { DashboardsTableConfigResolver } from "@home/pages/dashboard/dashboards-table-config.resolver";
import { RuleChainsTableConfigResolver } from "@home/pages/rulechain/rulechains-table-config.resolver";

const routes: Routes = [
  {
    path: 'edges',
    data: {
      breadcrumb: {
        label: 'edge.edges',
        icon: 'router'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          edgesType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: EdgesTableConfigResolver
        }
      },
      {
        path: ':edgeId/ruleChains',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          ruleChainsType: 'edge',
          breadcrumb: {
            label: 'rulechain.edge-rulechains',
            icon: 'settings_ethernet'
          },
        },
        resolve: {
          entitiesTableConfig: RuleChainsTableConfigResolver
        }
      },
      {
        path: ':edgeId/assets',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          assetsType: 'edge',
          breadcrumb: {
            label: 'edge.assets',
            icon: 'domain'
          }
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      },
      {
        path: ':edgeId/devices',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          devicesType: 'edge',
          breadcrumb: {
            label: 'edge.devices',
            icon: 'devices_other'
          }
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      },
      {
        path: ':edgeId/entityViews',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          entityViewsType: 'edge',
          breadcrumb: {
            label: 'edge.entity-views',
            icon: 'view_quilt'
          },
        },
        resolve: {
          entitiesTableConfig: EntityViewsTableConfigResolver
        }
      },
      {
        path: ':edgeId/dashboards',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          dashboardsType: 'edge',
          breadcrumb: {
            label: 'edge.dashboards',
            icon: 'dashboard'
          }
        },
        resolve: {
          entitiesTableConfig: DashboardsTableConfigResolver
        }
      }]
  }]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EdgesTableConfigResolver
  ]
})
export class EdgeRoutingModule { }
