// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import {
  EntityDebugSettingsButtonComponent
} from '@home/components/entity/debug/entity-debug-settings-button.component';
import { RuleNodeConfigModule } from '@home/components/rule-node/rule-node-config.module';
import {
  AddRuleNodeDialogComponent,
  AddRuleNodeLinkDialogComponent,
  CreateNestedRuleChainDialogComponent,
  RuleChainPageComponent
} from '@home/pages/rulechain/rulechain-page.component';
import { RuleNodeDetailsComponent } from '@home/pages/rulechain/rule-node-details.component';
import { RuleNodeConfigComponent } from '@home/pages/rulechain/rule-node-config.component';
import { LinkLabelsComponent } from '@home/pages/rulechain/link-labels.component';
import { RuleNodeLinkComponent } from '@home/pages/rulechain/rule-node-link.component';

@NgModule({
  declarations: [
    RuleChainPageComponent,
    RuleNodeDetailsComponent,
    LinkLabelsComponent,
    RuleNodeLinkComponent,
    RuleNodeConfigComponent,
    AddRuleNodeLinkDialogComponent,
    AddRuleNodeDialogComponent,
    CreateNestedRuleChainDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    DurationLeftPipe,
    EntityDebugSettingsButtonComponent,
    RuleNodeConfigModule,
    HomeComponentsModule,
  ]
})
export class RuleChainPageModule {}
