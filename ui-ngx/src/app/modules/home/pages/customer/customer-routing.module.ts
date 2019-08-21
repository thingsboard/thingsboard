///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {EntitiesTableComponent} from '../../components/entity/entities-table.component';
import {Authority} from '@shared/models/authority.enum';
import {UsersTableConfigResolver} from '../user/users-table-config.resolver';
import {CustomersTableConfigResolver} from './customers-table-config.resolver';
import {DevicesTableConfigResolver} from '@modules/home/pages/device/devices-table-config.resolver';
import {AssetsTableConfigResolver} from '../asset/assets-table-config.resolver';
import {DashboardsTableConfigResolver} from '@modules/home/pages/dashboard/dashboards-table-config.resolver';

const routes: Routes = [
  {
    path: 'customers',
    data: {
      breadcrumb: {
        label: 'customer.customers',
        icon: 'supervisor_account'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver
        }
      },
      {
        path: ':customerId/users',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'user.customer-users',
          breadcrumb: {
            label: 'user.customer-users',
            icon: 'account_circle'
          }
        },
        resolve: {
          entitiesTableConfig: UsersTableConfigResolver
        }
      },
      {
        path: ':customerId/devices',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.devices',
          devicesType: 'customer',
          breadcrumb: {
            label: 'customer.devices',
            icon: 'devices_other'
          }
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      },
      {
        path: ':customerId/assets',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.assets',
          assetsType: 'customer',
          breadcrumb: {
            label: 'customer.assets',
            icon: 'domain'
          }
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      },
      {
        path: ':customerId/dashboards',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.assets',
          dashboardsType: 'customer',
          breadcrumb: {
            label: 'customer.dashboards',
            icon: 'dashboard'
          }
        },
        resolve: {
          entitiesTableConfig: DashboardsTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    CustomersTableConfigResolver
  ]
})
export class CustomerRoutingModule { }
