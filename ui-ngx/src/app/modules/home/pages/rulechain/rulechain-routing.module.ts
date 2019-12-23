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
import {
  RuleChain,
  RuleChainMetaData,
  RuleChainImport,
  ResolvedRuleChainMetaData
} from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { DashboardPageComponent } from '@home/pages/dashboard/dashboard-page.component';
import { dashboardBreadcumbLabelFunction, DashboardResolver } from '@home/pages/dashboard/dashboard-routing.module';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { RuleNodeComponentDescriptor } from '@shared/models/rule-node.models';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';

import * as AngularCommon from '@angular/common';
import * as AngularCore from '@angular/core';
import * as AngularForms from '@angular/forms';
import * as AngularCdkCoercion from '@angular/cdk/coercion';
import * as NgrxStore from '@ngrx/store';
import * as TranslateCore from '@ngx-translate/core';
import * as TbCore from '@core/public-api';
import * as TbShared from '@shared/public-api';

declare const SystemJS;

const ruleNodeConfigResourcesModulesMap = {
  '@angular/core': SystemJS.newModule(AngularCore),
  '@angular/common': SystemJS.newModule(AngularCommon),
  '@angular/forms': SystemJS.newModule(AngularForms),
  '@ngrx/store': SystemJS.newModule(NgrxStore),
  '@ngx-translate/core': SystemJS.newModule(TranslateCore),
  '@core/public-api': SystemJS.newModule(TbCore),
  '@shared/public-api': SystemJS.newModule(TbShared)
};

const t = SystemJS.newModule(AngularCore);


@Injectable()
export class RuleChainResolver implements Resolve<RuleChain> {

  constructor(private ruleChainService: RuleChainService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<RuleChain> {
    const ruleChainId = route.params.ruleChainId;
    return this.ruleChainService.getRuleChain(ruleChainId);
  }
}

@Injectable()
export class ResolvedRuleChainMetaDataResolver implements Resolve<ResolvedRuleChainMetaData> {

  constructor(private ruleChainService: RuleChainService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<ResolvedRuleChainMetaData> {
    const ruleChainId = route.params.ruleChainId;
    return this.ruleChainService.getResolvedRuleChainMetadata(ruleChainId);
  }
}

@Injectable()
export class RuleNodeComponentsResolver implements Resolve<Array<RuleNodeComponentDescriptor>> {

  constructor(private ruleChainService: RuleChainService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Array<RuleNodeComponentDescriptor>> {
    return this.ruleChainService.getRuleNodeComponents(ruleNodeConfigResourcesModulesMap);
  }
}

export const ruleChainBreadcumbLabelFunction: BreadCrumbLabelFunction = ((route, translate, component) => {
  let label: string = component.ruleChain.name;
  if (component.ruleChain.root) {
    label += ` (${translate.instant('rulechain.root')})`;
  }
  return label;
});

export const importRuleChainBreadcumbLabelFunction: BreadCrumbLabelFunction = ((route, translate, component) => {
  return `${translate.instant('rulechain.import')}: ${component.ruleChain.name}`;
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
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: ruleChainBreadcumbLabelFunction,
            icon: 'settings_ethernet'
          } as BreadCrumbConfig,
          auth: [Authority.TENANT_ADMIN],
          title: 'rulechain.rulechain',
          import: false
        },
        resolve: {
          ruleChain: RuleChainResolver,
          ruleChainMetaData: ResolvedRuleChainMetaDataResolver,
          ruleNodeComponents: RuleNodeComponentsResolver
        }
      },
      {
        path: 'ruleChain/import',
        component: RuleChainPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: importRuleChainBreadcumbLabelFunction,
            icon: 'settings_ethernet'
          } as BreadCrumbConfig,
          auth: [Authority.TENANT_ADMIN],
          title: 'rulechain.rulechain',
          import: true
        },
        resolve: {
          ruleChain: 'importRuleChain',
          ruleChainMetaData: 'importRuleChainMetadata',
          ruleNodeComponents: RuleNodeComponentsResolver
        }
      }
    ]
  }
];

// @dynamic
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    RuleChainsTableConfigResolver,
    RuleChainResolver,
    ResolvedRuleChainMetaDataResolver,
    RuleNodeComponentsResolver,
    {
      provide: 'importRuleChain',
      useValue: (route: ActivatedRouteSnapshot) => {
        const ruleChainImport: RuleChainImport = route.params.ruleChainImport;
        return ruleChainImport.ruleChain;
      }
    },
    {
      provide: 'importRuleChainMetadata',
      useValue: (route: ActivatedRouteSnapshot) => {
        const ruleChainImport: RuleChainImport = route.params.ruleChainImport;
        return ruleChainImport.metadata;
      }
    }
  ]
})
export class RuleChainRoutingModule { }
