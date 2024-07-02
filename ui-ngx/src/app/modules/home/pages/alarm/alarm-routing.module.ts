///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { AlarmTableComponent } from '@home/components/alarm/alarm-table.component';
import { AlarmsMode } from '@shared/models/alarm.models';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { AlarmRulesTableConfigResolver } from '@home/pages/alarm/alarm-rules-table-config.resolver';

const routes: Routes = [
  {
    path: 'alarms',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'alarm.alarms',
        icon: 'mdi:alert-outline'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: '/alarms/alarms'
        }
      },
      {
        path: 'alarms',
        component: AlarmTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'alarm.alarms',
          breadcrumb: {
            label: 'alarm.alarms',
            icon: 'mdi:alert-outline'
          },
          isPage: true,
          alarmsMode: AlarmsMode.ALL
        }
      },
      {
        path: 'alarm-rules',
        data: {
          breadcrumb: {
            label: 'alarm-rule.alarm-rules',
            icon: 'mdi:list-status'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'alarm-rules.alarm-rules'
            },
            resolve: {
              entitiesTableConfig: AlarmRulesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'mdi:list-status'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN]
            },
            resolve: {
              entitiesTableConfig: null
            }
          }
        ]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AlarmRulesTableConfigResolver
  ]
})
export class AlarmRoutingModule { }
