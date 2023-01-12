///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { NotificationTemplate, NotificationType } from '@shared/models/notification.models';
import { Component, Inject, OnDestroy } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepTrim, isDefined } from '@core/utils';
import { Subject } from 'rxjs';

export interface TemplateNotificationDialogData {
  template?: NotificationTemplate;
  isAdd?: boolean;
  isCopy?: boolean;
}

@Component({
  selector: 'tb-template-notification-dialog',
  templateUrl: './template-notification-dialog.component.html',
  styleUrls: ['./template-notification-dialog.component.scss']
})
export class TemplateNotificationDialogComponent
  extends DialogComponent<TemplateNotificationDialogComponent, NotificationTemplate> implements OnDestroy {

  templateNotificationForm: FormGroup;
  dialogTitle = 'notification.edit-notification-template';
  saveButtonLabel = 'action.save';

  NotificationType = NotificationType;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TemplateNotificationDialogComponent, NotificationTemplate>,
              @Inject(MAT_DIALOG_DATA) public data: TemplateNotificationDialogData,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (data.isAdd) {
      this.dialogTitle = 'notification.add-notification-template';
      this.saveButtonLabel = 'action.add';
    }
    if (data.isCopy) {
      this.dialogTitle = 'notification.copy-notification-template';
    }

    this.templateNotificationForm = this.fb.group({
      name: [null, Validators.required],
      type: [NotificationType.GENERIC],
      configuration: this.fb.group({
        defaultTextTemplate: [null, Validators.required]
      })
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    let formValue = deepTrim(this.templateNotificationForm.value);
    if (isDefined(this.data.template)) {
      formValue = Object.assign({}, this.data.template, formValue);
    }
    this.notificationService.saveNotificationTemplate(formValue).subscribe(
      (target) => this.dialogRef.close(target)
    );
  }
}
