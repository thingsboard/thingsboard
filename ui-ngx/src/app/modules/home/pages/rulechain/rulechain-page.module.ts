// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
  AddNoteDialogComponent,
  AddRuleNodeDialogComponent,
  AddRuleNodeLinkDialogComponent,
  CreateNestedRuleChainDialogComponent,
  RuleChainPageComponent
} from '@home/pages/rulechain/rulechain-page.component';
import { RuleNodeDetailsComponent } from '@home/pages/rulechain/rule-node-details.component';
import { RuleNodeConfigComponent } from '@home/pages/rulechain/rule-node-config.component';
import { LinkLabelsComponent } from '@home/pages/rulechain/link-labels.component';
import { RuleNodeLinkComponent } from '@home/pages/rulechain/rule-node-link.component';
import { RuleNoteEditorComponent } from '@home/pages/rulechain/rule-note-editor.component';

@NgModule({
  declarations: [
    RuleChainPageComponent,
    RuleNodeDetailsComponent,
    RuleNoteEditorComponent,
    LinkLabelsComponent,
    RuleNodeLinkComponent,
    RuleNodeConfigComponent,
    AddRuleNodeLinkDialogComponent,
    AddRuleNodeDialogComponent,
    CreateNestedRuleChainDialogComponent,
    AddNoteDialogComponent
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
