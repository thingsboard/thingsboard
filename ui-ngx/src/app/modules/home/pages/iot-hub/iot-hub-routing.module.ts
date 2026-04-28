///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { TbIotHubHomeComponent } from './iot-hub-home.component';
import { TbIotHubItemsPageComponent } from './iot-hub-items-page.component';
import { TbIotHubCreatorProfileComponent } from './iot-hub-creator-profile.component';
import { TbIotHubInstalledItemsComponent } from './iot-hub-installed-items.component';
import { TbIotHubSearchPageComponent } from './iot-hub-search-page.component';
import { TbIotHubItemResolverComponent } from './iot-hub-item-resolver.component';

const routes: Routes = [
  {
    path: 'iot-hub',
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'iot-hub.iot-hub',
        icon: 'store'
      }
    },
    children: [
      {
        path: '',
        component: TbIotHubHomeComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.iot-hub'
        }
      },
      {
        path: 'widgets',
        component: TbIotHubItemsPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'item.type-widget-plural',
          itemType: 'WIDGET',
          breadcrumb: { label: 'item.type-widget-plural', icon: 'widgets' }
        }
      },
      {
        path: 'dashboards',
        component: TbIotHubItemsPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'item.type-dashboard-plural',
          itemType: 'DASHBOARD',
          breadcrumb: { label: 'item.type-dashboard-plural', icon: 'dashboard' }
        }
      },
      {
        path: 'solution-templates',
        component: TbIotHubItemsPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'item.type-solution-template-plural',
          itemType: 'SOLUTION_TEMPLATE',
          breadcrumb: { label: 'item.type-solution-template-plural', icon: 'integration_instructions' }
        }
      },
      {
        path: 'calculated-fields',
        component: TbIotHubItemsPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'item.type-calculated-field-plural',
          itemType: 'CALCULATED_FIELD',
          breadcrumb: { label: 'item.type-calculated-field-plural', icon: 'functions' }
        }
      },
      {
        path: 'rule-chains',
        component: TbIotHubItemsPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'item.type-rule-chain-plural',
          itemType: 'RULE_CHAIN',
          breadcrumb: { label: 'item.type-rule-chain-plural', icon: 'account_tree' }
        }
      },
      {
        path: 'devices',
        component: TbIotHubItemsPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.device-library',
          itemType: 'DEVICE',
          breadcrumb: { label: 'iot-hub.device-library', icon: 'memory' }
        }
      },
      {
        path: 'search',
        component: TbIotHubSearchPageComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.search-results',
          breadcrumb: {
            label: 'iot-hub.search-results',
            icon: 'search'
          }
        }
      },
      {
        path: 'installed',
        component: TbIotHubInstalledItemsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.installed-items',
          breadcrumb: {
            label: 'iot-hub.installed-items',
            icon: 'inventory_2'
          }
        }
      },
      {
        path: 'creator/:creatorId',
        component: TbIotHubCreatorProfileComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.creator-profile',
          breadcrumb: {
            label: 'iot-hub.creator-profile',
            icon: 'person'
          }
        }
      },
      {
        path: 'version/:itemVersionId',
        component: TbIotHubItemResolverComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.item-detail'
        }
      },
      {
        path: ':itemId',
        component: TbIotHubItemResolverComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.item-detail'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IotHubRoutingModule { }
