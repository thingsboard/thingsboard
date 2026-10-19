// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { ChangeOriginatorConfigComponent } from './change-originator-config.component';
import { CommonRuleNodeConfigModule } from '../common/common-rule-node-config.module';
import { TransformScriptConfigComponent } from './script-config.component';
import { ToEmailConfigComponent } from './to-email-config.component';
import { CopyKeysConfigComponent } from './copy-keys-config.component';
import { RenameKeysConfigComponent } from './rename-keys-config.component';
import { NodeJsonPathConfigComponent } from './node-json-path-config.component';
import { DeleteKeysConfigComponent } from './delete-keys-config.component';
import { DeduplicationConfigComponent } from './deduplication-config.component';
import { DuplicateToGroupConfigComponent } from './duplicate-to-group-config.component';
import { DuplicateToGroupByNameConfigComponent } from './duplicate-to-group-by-name-config.component';
import { DuplicateToRelatedConfigComponent } from './duplicate-to-related-config.component';

@NgModule({
  declarations: [
    ChangeOriginatorConfigComponent,
    TransformScriptConfigComponent,
    ToEmailConfigComponent,
    CopyKeysConfigComponent,
    RenameKeysConfigComponent,
    NodeJsonPathConfigComponent,
    DeleteKeysConfigComponent,
    DeduplicationConfigComponent,
    DuplicateToGroupConfigComponent,
    DuplicateToRelatedConfigComponent,
    DuplicateToGroupByNameConfigComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    ChangeOriginatorConfigComponent,
    TransformScriptConfigComponent,
    ToEmailConfigComponent,
    CopyKeysConfigComponent,
    RenameKeysConfigComponent,
    NodeJsonPathConfigComponent,
    DeleteKeysConfigComponent,
    DeduplicationConfigComponent,
    DuplicateToGroupConfigComponent,
    DuplicateToRelatedConfigComponent,
    DuplicateToGroupByNameConfigComponent,
  ]
})
export class TransformationRuleNodeConfigModule {
}
