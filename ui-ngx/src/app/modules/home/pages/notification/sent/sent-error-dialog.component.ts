// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: ['sent-error-dialog.component.scss'],
    standalone: false
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
