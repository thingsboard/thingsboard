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
import { ActivationLinkInfo } from '@shared/models/user.model';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';

export interface ActivationLinkDialogData {
  activationLinkInfo: ActivationLinkInfo;
}

@Component({
    selector: 'tb-activation-link-dialog',
    templateUrl: './activation-link-dialog.component.html',
    standalone: false
})
export class ActivationLinkDialogComponent extends DialogComponent<ActivationLinkDialogComponent, void> {

  activationLink: string;
  activationLinkTtl: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ActivationLinkDialogData,
              public dialogRef: MatDialogRef<ActivationLinkDialogComponent, void>,
              private translate: TranslateService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe) {
    super(store, router, dialogRef);
    this.activationLink = this.data.activationLinkInfo.value;
    this.activationLinkTtl = this.millisecondsToTimeStringPipe.transform(this.data.activationLinkInfo.ttlMs);
  }

  close(): void {
    this.dialogRef.close();
  }

  onActivationLinkCopied() {
     this.store.dispatch(new ActionNotificationShow(
       {
         message: this.translate.instant('user.activation-link-copied-message'),
         type: 'success',
         target: 'activationLinkDialogContent',
         duration: 1200,
         verticalPosition: 'bottom',
         horizontalPosition: 'left'
       }));
  }

}
