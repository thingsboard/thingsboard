// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { RuleChainRoutingModule } from '@modules/home/pages/rulechain/rulechain-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { RuleNodeComponent } from '@home/pages/rulechain/rulenode.component';
import { FC_NODE_COMPONENT_CONFIG } from 'ngx-flowchart';

@NgModule({
  declarations: [
    RuleChainComponent,
    RuleChainTabsComponent,
    RuleNodeComponent,
  ],
  providers: [
    {
      provide: FC_NODE_COMPONENT_CONFIG,
      useValue: {
        nodeComponentType: RuleNodeComponent
      }
    },
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    RuleChainRoutingModule,
  ]
})
export class RuleChainModule { }
