// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AlarmDetailsDialogComponent } from '@home/components/alarm/alarm-details-dialog.component';
import { SchedulerEventModule } from '@home/components/scheduler/scheduler-event.module';
import { BlobEntitiesComponent } from '@home/components/blob-entity/blob-entities.component';
import { SHARED_HOME_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { AlarmCommentComponent } from '@home/components/alarm/alarm-comment.component';
import { AlarmCommentDialogComponent } from '@home/components/alarm/alarm-comment-dialog.component';
import { AlarmAssigneeComponent } from '@home/components/alarm/alarm-assignee.component';

@NgModule({
  providers: [
    { provide: SHARED_HOME_COMPONENTS_MODULE_TOKEN, useValue: SharedHomeComponentsModule }
  ],
  declarations:
    [
      AlarmDetailsDialogComponent,
      AlarmCommentComponent,
      AlarmCommentDialogComponent,
      AlarmAssigneeComponent,
      BlobEntitiesComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    SchedulerEventModule
  ],
  exports: [
    AlarmDetailsDialogComponent,
    AlarmCommentComponent,
    AlarmCommentDialogComponent,
    AlarmAssigneeComponent,
    BlobEntitiesComponent,
    SchedulerEventModule
  ]
})
export class SharedHomeComponentsModule { }
