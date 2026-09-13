// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Notification } from '@shared/models/notification.models';

export interface InboxNotificationDialogData {
  notification: Notification;
}

@Component({
    selector: 'tb-inbox-notification-dialog',
    templateUrl: './inbox-notification-dialog.component.html',
    styleUrls: ['inbox-notification-dialog.component.scss'],
    standalone: false
})
export class InboxNotificationDialogComponent extends DialogComponent<InboxNotificationDialogComponent, string> {

  notification: Notification;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<InboxNotificationDialogComponent, string>,
              @Inject(MAT_DIALOG_DATA) public data: InboxNotificationDialogData) {
    super(store, router, dialogRef);

    this.notification = data.notification;
  }

  markAsRead(id: string) {
    this.dialogRef.close(id);
  }
}
