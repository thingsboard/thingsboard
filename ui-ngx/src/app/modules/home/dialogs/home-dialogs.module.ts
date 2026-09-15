// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AssignToCustomerDialogComponent } from '@modules/home/dialogs/assign-to-customer-dialog.component';
import { AddEntitiesToCustomerDialogComponent } from '@modules/home/dialogs/add-entities-to-customer-dialog.component';
import { HomeDialogsService } from './home-dialogs.service';
import { AddEntitiesToEdgeDialogComponent } from '@home/dialogs/add-entities-to-edge-dialog.component';

@NgModule({
  declarations:
  [
    AssignToCustomerDialogComponent,
    AddEntitiesToCustomerDialogComponent,
    AddEntitiesToEdgeDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    AssignToCustomerDialogComponent,
    AddEntitiesToCustomerDialogComponent,
    AddEntitiesToEdgeDialogComponent
  ],
  providers: [
    HomeDialogsService
  ]
})
export class HomeDialogsModule { }
