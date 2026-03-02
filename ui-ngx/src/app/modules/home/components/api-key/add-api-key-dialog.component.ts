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
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, Validators } from '@angular/forms';
import { deepTrim } from '@core/utils';
import { ApiKeyService } from '@core/http/api-key.service';
import { ApiKeyInfo } from '@shared/models/api-key.models';
import { ApiKeysTableDialogData } from '@home/components/api-key/api-keys-table-dialog.component';
import { DAY } from '@shared/models/time/time.models';

@Component({
    selector: 'tb-add-api-key-dialog',
    templateUrl: './add-api-key-dialog.component.html',
    styleUrls: ['./add-api-key-dialog.component.scss'],
    standalone: false
})
export class AddApiKeyDialogComponent extends DialogComponent<AddApiKeyDialogComponent, ApiKeyInfo | string> {

  readonly startDate = new Date();
  readonly expirationTimeOptions: Array<number> = [7, 30, 60, 90].map(days => days * DAY);
  readonly apiKeyForm = this.fb.group({
    description: [{value: null, disabled: false}, [Validators.required]],
    enabled: [{value: true, disabled: false}, []],
    expirationTime: [{value: this.expirationTimeOptions[1] as string | number, disabled: false}, [Validators.required]],
    customExpirationTime: [{value: null, disabled: true}, []],
  });

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    public dialogRef: MatDialogRef<AddApiKeyDialogComponent, ApiKeyInfo | string>,
    private fb: FormBuilder,
    private apiKeyService: ApiKeyService,
    @Inject(MAT_DIALOG_DATA) public data: ApiKeysTableDialogData,
  ) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    const formValue = this.apiKeyForm.value;
    const userId = this.data.userId;
    const expirationTime = this.calcExpirationTime();
    const apiKey = {
      ...deepTrim(formValue),
      expirationTime,
      userId,
    } as ApiKeyInfo;
    this.apiKeyService.saveApiKey(apiKey).subscribe(
      (res) => {
        this.dialogRef.close(res);
      }
    );
  }

  isCustomExpirationTime() {
    return this.apiKeyForm.value?.expirationTime === 'custom';
  }

  onExpirationDateChange() {
    const customExpirationTimeControl = this.apiKeyForm.get('customExpirationTime');
    if (this.isCustomExpirationTime()) {
      customExpirationTimeControl.enable({emitEvent: false});
    } else {
      customExpirationTimeControl.disable({emitEvent: false});
    }
  }

  private calcExpirationTime(): number {
    const expirationTimeValue = this.apiKeyForm.get('expirationTime').value;
    let value: number;
    if (this.isCustomExpirationTime()) {
      value = this.apiKeyForm.get('customExpirationTime').value.getTime();
    } else if (expirationTimeValue === 'never') {
      value = 0;
    } else {
      value = expirationTimeValue as number + Date.now();
    }
    return value;
  }
}
