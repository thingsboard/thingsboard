// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { EditorPanelComponent } from '@home/pages/mobile/common/editor-panel.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';

@NgModule({
  declarations: [
    EditorPanelComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    EditorPanelComponent
  ]
})
export class CommonMobileModule {}
