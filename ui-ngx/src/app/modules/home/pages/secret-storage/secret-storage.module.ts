// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { SecretStorageTableHeaderComponent } from '@home/pages/secret-storage/secret-storage-table-header.component';
import { SecretStorageTableComponent } from '@home/pages/secret-storage/secret-storage-table.component';
import {
  EditSecretDescriptionPanelComponent
} from '@home/pages/secret-storage/edit-secret-description-panel.component';
import { EditSecretValueDialogComponent } from '@home/pages/secret-storage/edit-secret-value-dialog.component';

@NgModule({
  declarations: [
    SecretStorageTableComponent,
    SecretStorageTableHeaderComponent,
    EditSecretDescriptionPanelComponent,
    EditSecretValueDialogComponent
  ],
  exports: [
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule
  ]
})
export class SecretStorageModule { }
