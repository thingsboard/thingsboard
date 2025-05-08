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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  NotificationDeliveryMethod,
  NotificationDeliveryMethodInfoMap,
  NotificationRequest
} from '@shared/models/notification.models';

export interface NotificationRequestErrorDialogData {
  notificationRequest: NotificationRequest;
}

@Component({
  selector: 'tb-notification-send-error-dialog',
  templateUrl: './sent-error-dialog.component.html',
  styleUrls: ['sent-error-dialog.component.scss']
})
export class SentErrorDialogComponent extends DialogComponent<SentErrorDialogComponent, void> {

  errorStats: { [key in NotificationDeliveryMethod]: {[errorKey in string]: string}};

  NotificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<SentErrorDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: NotificationRequestErrorDialogData) {
    super(store, router, dialogRef);

    this.errorStats = data.notificationRequest.stats.errors;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
