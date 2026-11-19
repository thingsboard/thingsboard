// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ExtensionComponent } from '@home/pages/extension/extension.component';
import { ExtensionApiDocsDialogComponent } from '@home/pages/extension/extension-api-docs-dialog.component';
import { ExtensionLogsDialogComponent } from '@home/pages/extension/extension-logs-dialog.component';
import { ExtensionQuotaPanelComponent } from '@home/pages/extension/extension-quota-panel.component';
import { ExtensionRoutingModule } from '@home/pages/extension/extension-routing.module';

@NgModule({
  declarations: [
    ExtensionComponent,
    ExtensionApiDocsDialogComponent,
    ExtensionLogsDialogComponent,
    ExtensionQuotaPanelComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    ExtensionRoutingModule
  ]
})
export class ExtensionModule { }
