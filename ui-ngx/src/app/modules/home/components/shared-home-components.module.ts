///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
