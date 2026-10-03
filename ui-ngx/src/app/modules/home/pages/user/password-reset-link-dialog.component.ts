///
/// Copyright © 2016-2026 The Thingsboard Authors
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
