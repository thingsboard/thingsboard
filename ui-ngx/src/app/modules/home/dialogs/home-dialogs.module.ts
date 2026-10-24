// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { HomeDialogsService } from './home-dialogs.service';
import { AddEntitiesToEdgeDialogComponent } from '@home/dialogs/add-entities-to-edge-dialog.component';
import { SelectOwnerDialogComponent } from '@home/dialogs/select-owner-dialog.component';
import { SelectEntityGroupDialogComponent } from '@home/dialogs/select-entity-group-dialog.component';
import { ShareEntityGroupDialogComponent } from '@home/dialogs/share-entity-group-dialog.component';
import { AddEntityGroupsToEdgeDialogComponent } from '@home/dialogs/add-entity-groups-to-edge-dialog.component';

@NgModule({
  declarations:
  [
    SelectOwnerDialogComponent,
    SelectEntityGroupDialogComponent,
    ShareEntityGroupDialogComponent,
    AddEntityGroupsToEdgeDialogComponent,
    AddEntitiesToEdgeDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    SelectOwnerDialogComponent,
    SelectEntityGroupDialogComponent,
    ShareEntityGroupDialogComponent,
    AddEntityGroupsToEdgeDialogComponent,
    AddEntitiesToEdgeDialogComponent
  ],
  providers: [
    HomeDialogsService
  ]
})
export class HomeDialogsModule { }
