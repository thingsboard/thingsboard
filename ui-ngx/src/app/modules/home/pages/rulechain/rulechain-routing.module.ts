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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';

import {EntitiesTableComponent} from '../../components/entity/entities-table.component';
import {Authority} from '@shared/models/authority.enum';
import {RuleChainsTableConfigResolver} from '@modules/home/pages/rulechain/rulechains-table-config.resolver';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { RuleChain } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { DashboardPageComponent } from '@home/pages/dashboard/dashboard-page.component';
import { dashboardBreadcumbLabelFunction, DashboardResolver } from '@home/pages/dashboard/dashboard-routing.module';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';


@Injectable()
export class RuleChainResolver implements Resolve<RuleChain> {

  constructor(private ruleChainService: RuleChainService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<RuleChain> {
    const ruleChainId = route.params.ruleChainId;
    return this.ruleChainService.getRuleChain(ruleChainId);
  }
}

export const ruleChainBreadcumbLabelFunction: BreadCrumbLabelFunction = ((route, translate, component) => {
  let label: string = component.ruleChain.name;
  if (component.ruleChain.root) {
    label += ` (${translate.instant('rulechain.root')})`;
  }
  return label;
});

const routes: Routes = [
  {
    path: 'ruleChains',
    data: {
      breadcrumb: {
        label: 'rulechain.rulechains',
        icon: 'settings_ethernet'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'rulechain.rulechains'
        },
        resolve: {
          entitiesTableConfig: RuleChainsTableConfigResolver
        }
      },
      {
        path: ':ruleChainId',
        component: RuleChainPageComponent,
        data: {
          breadcrumb: {
            labelFunction: ruleChainBreadcumbLabelFunction,
            icon: 'settings_ethernet'
          } as BreadCrumbConfig,
          auth: [Authority.TENANT_ADMIN],
          title: 'rulechain.rulechain',
          widgetEditMode: false
        },
        resolve: {
          ruleChain: RuleChainResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    RuleChainsTableConfigResolver,
    RuleChainResolver
  ]
})
export class RuleChainRoutingModule { }
