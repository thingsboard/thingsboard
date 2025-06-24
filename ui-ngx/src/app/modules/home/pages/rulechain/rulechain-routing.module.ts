///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Inject, Injectable, NgModule, Optional } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterModule, RouterStateSnapshot, Routes, UrlTree } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { RuleChainsTableConfigResolver } from '@modules/home/pages/rulechain/rulechains-table-config.resolver';
import { from, Observable } from 'rxjs';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import {
  RuleChainMetaData,
  RuleChain, RuleChainType
} from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { RuleNodeComponentDescriptor } from '@shared/models/rule-node.models';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { ItemBufferService, MenuId } from '@core/public-api';
import { MODULES_MAP } from '@shared/public-api';
import { IModulesMap } from '@modules/common/modules-map.models';

@Injectable()
export class RuleChainResolver  {

  constructor(private ruleChainService: RuleChainService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<RuleChain> {
    const ruleChainId = route.params.ruleChainId;
    return this.ruleChainService.getRuleChain(ruleChainId);
  }
}

@Injectable()
export class RuleChainMetaDataResolver  {

  constructor(private ruleChainService: RuleChainService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<RuleChainMetaData> {
    const ruleChainId = route.params.ruleChainId;
    return this.ruleChainService.getRuleChainMetadata(ruleChainId);
  }
}

@Injectable()
export class RuleNodeComponentsResolver  {

  constructor(private ruleChainService: RuleChainService,
              @Optional() @Inject(MODULES_MAP) private modulesMap: IModulesMap) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Array<RuleNodeComponentDescriptor>> {
    return this.ruleChainService.getRuleNodeComponents(this.modulesMap, route.data.ruleChainType);
  }
}

@Injectable()
export class TooltipsterResolver  {

  constructor() {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<any> {
    return from(import('tooltipster'));
  }
}

@Injectable()
export class RuleChainImportGuard  {

  constructor(private itembuffer: ItemBufferService,
              private router: Router) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
    Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    if (this.itembuffer.hasRuleChainImport()) {
      return true;
    } else {
      return this.router.parseUrl('ruleChains');
    }
  }

}

export const ruleChainBreadcumbLabelFunction: BreadCrumbLabelFunction<RuleChainPageComponent>
  = ((route, translate, component) => {
  let label: string = component.ruleChain.name;
  if (component.ruleChain.root) {
    label += ` (${translate.instant('rulechain.root')})`;
  }
  return label;
});

export const importRuleChainBreadcumbLabelFunction: BreadCrumbLabelFunction<RuleChainPageComponent> =
  ((route, translate, component) => {
  return `${translate.instant('rulechain.import')}: ${component.ruleChain.name}`;
});

const routes: Routes = [
  {
    path: 'ruleChains',
    data: {
      breadcrumb: {
        menuId: MenuId.rule_chains
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'rulechain.rulechains',
          ruleChainsType: 'tenant'
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
          title: 'rulechain.rulechain',
          import: false,
          ruleChainType: RuleChainType.CORE
        },
        loadChildren: () => import('./rulechain-page.module').then(m => m.RuleChainPageModule),
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
          title: 'rulechain.rulechain',
          import: true,
          ruleChainType: RuleChainType.CORE
        },
        loadChildren: () => import('./rulechain-page.module').then(m => m.RuleChainPageModule),
        resolve: {
          ruleNodeComponents: RuleNodeComponentsResolver,
          tooltipster: TooltipsterResolver
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
    RuleChainMetaDataResolver,
    RuleNodeComponentsResolver,
    TooltipsterResolver,
    RuleChainImportGuard
  ]
})
export class RuleChainRoutingModule { }
