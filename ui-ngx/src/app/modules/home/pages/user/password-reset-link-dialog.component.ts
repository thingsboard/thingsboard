// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { PasswordResetLinkInfo } from '@shared/models/user.model';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';

export interface PasswordResetLinkDialogData {
  passwordResetLinkInfo: PasswordResetLinkInfo;
}

@Component({
    selector: 'tb-password-reset-link-dialog',
    templateUrl: './password-reset-link-dialog.component.html',
    standalone: false
})
export class PasswordResetLinkDialogComponent extends DialogComponent<PasswordResetLinkDialogComponent, void> {

  passwordResetLink: string;
  passwordResetLinkTtl: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: PasswordResetLinkDialogData,
              public dialogRef: MatDialogRef<PasswordResetLinkDialogComponent, void>,
              private translate: TranslateService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe) {
    super(store, router, dialogRef);
    this.passwordResetLink = this.data.passwordResetLinkInfo.value;
    this.passwordResetLinkTtl = this.millisecondsToTimeStringPipe.transform(this.data.passwordResetLinkInfo.ttlMs);
  }

  close(): void {
    this.dialogRef.close();
  }

  onPasswordResetLinkCopied() {
     this.store.dispatch(new ActionNotificationShow(
       {
         message: this.translate.instant('user.password-reset-link-copied-message'),
         type: 'success',
         target: 'passwordResetLinkDialogContent',
         duration: 1200,
         verticalPosition: 'bottom',
         horizontalPosition: 'left'
       }));
  }

}
