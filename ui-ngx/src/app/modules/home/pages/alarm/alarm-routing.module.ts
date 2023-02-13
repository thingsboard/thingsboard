///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { Observable } from 'rxjs';
import { OAuth2Service } from '@core/http/oauth2.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { AlarmTableComponent } from '@home/components/alarm/alarm-table.component';
import { AlarmRulesTableConfigResolver } from '@home/pages/alarm/alarm-rules-table-config.resolver';
import { AlarmsMode } from '@shared/models/alarm.models';

@Injectable()
export class OAuth2LoginProcessingUrlResolver implements Resolve<string> {

  constructor(private oauth2Service: OAuth2Service) {
  }

  resolve(): Observable<string> {
    return this.oauth2Service.getLoginProcessingUrl();
  }
}

const routes: Routes = [
  {
    path: 'alarm',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'alarm.alarms',
        icon: 'notifications'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/alarm/alarms'
        }
      },
      {
        path: 'alarms',
        component: AlarmTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'alarm.all-alarms',
          breadcrumb: {
            label: 'alarm.all-alarms',
            icon: 'notifications'
          },
          isPage: true,
          alarmsMode: AlarmsMode.ALL
        }
      },
      {
        path: 'rules',
        data: {
          breadcrumb: {
            label: 'alarm-rule.rules',
            icon: 'edit_notifications'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'alarm-rule.alarm-rules'
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
                icon: 'domain'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'alarm-rule.alarm-rules'
            },
            resolve: {
              entitiesTableConfig: AlarmRulesTableConfigResolver
            }
          }
        ]
      }
    ]
  },
  {
    path: 'alarms',
    component: AlarmTableComponent,
    data: {
      auth: [Authority.CUSTOMER_USER],
      title: 'alarm.alarms',
      breadcrumb: {
        label: 'alarm.alarms',
        icon: 'notifications'
      },
      isPage: true,
      alarmsMode: AlarmsMode.ALL
    }
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
