// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AlarmDetailsDialogComponent } from '@home/components/alarm/alarm-details-dialog.component';
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
      AlarmAssigneeComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    AlarmDetailsDialogComponent,
    AlarmCommentComponent,
    AlarmCommentDialogComponent,
    AlarmAssigneeComponent
  ]
})
export class SharedHomeComponentsModule { }
