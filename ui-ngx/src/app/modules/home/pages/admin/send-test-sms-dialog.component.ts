// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { phoneNumberPattern, SmsProviderConfiguration, TestSmsRequest } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';

export interface SendTestSmsDialogData {
  smsProviderConfiguration: SmsProviderConfiguration;
}

@Component({
    selector: 'tb-send-test-sms-dialog',
    templateUrl: './send-test-sms-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class SendTestSmsDialogComponent extends
  DialogComponent<SendTestSmsDialogComponent> implements OnInit {

  phoneNumberPattern = phoneNumberPattern;

  sendTestSmsFormGroup: UntypedFormGroup;

  smsProviderConfiguration = this.data.smsProviderConfiguration;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SendTestSmsDialogData,
              private adminService: AdminService,
              private translate: TranslateService,
              public dialogRef: MatDialogRef<SendTestSmsDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.sendTestSmsFormGroup = this.fb.group({
      numberTo: [null, [Validators.required, Validators.pattern(phoneNumberPattern)]],
      message: [null, [Validators.required, Validators.maxLength(1600)]]
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  sendTestSms(): void {
    const request: TestSmsRequest =  {
      providerConfiguration: this.smsProviderConfiguration,
      numberTo: this.sendTestSmsFormGroup.value.numberTo,
      message: this.sendTestSmsFormGroup.value.message
    };
    this.adminService.sendTestSms(request).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.translate.instant('admin.test-sms-sent'),
            target: 'sendTestSmsDialogContent',
            verticalPosition: 'bottom',
            horizontalPosition: 'left',
            type: 'success'
          }));
      }
    );
  }
}
