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

import {
  NotificationRule,
  NotificationTarget,
  NotificationTargetConfigType,
  NotificationTargetConfigTypeTranslateMap
} from '@shared/models/notification.models';
import { Component, Inject, OnDestroy } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { EntityType } from '@shared/models/entity-type.models';
import { deepTrim, isDefined } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface RuleNotificationDialogData {
  rule?: NotificationRule;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-rule-notification-dialog',
  templateUrl: './rule-notification-dialog.component.html',
  styleUrls: ['rule-notification-dialog.component.scss']
})
export class RuleNotificationDialogComponent extends
  DialogComponent<RuleNotificationDialogComponent, NotificationRule> implements OnDestroy {

  ruleNotificationForm: FormGroup;
  // notificationTargetConfigType = NotificationTargetConfigType;
  // notificationTargetConfigTypes: NotificationTargetConfigType[] = Object.values(NotificationTargetConfigType);
  // notificationTargetConfigTypeTranslateMap = NotificationTargetConfigTypeTranslateMap;
  entityType = EntityType;
  isAdd = true;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RuleNotificationDialogComponent, NotificationRule>,
              @Inject(MAT_DIALOG_DATA) public data: RuleNotificationDialogData,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (isDefined(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.ruleNotificationForm = this.fb.group({
      name: [null, Validators.required],
      templateId: [null, Validators.required],
      configuration: this.fb.group({
        ids: [],
        interval: [],
        // type: [NotificationTargetConfigType.ALL_USERS],
        description: [null],
        // usersIds: [{value: null, disabled: true}, Validators.required],
        // customerId: [{value: null, disabled: true}, Validators.required],
        // getCustomerIdFromOriginatorEntity: [{value: false, disabled: true}],
      })
    });

    // if (isDefined(data.target)) {
    //   this.targetNotificationForm.patchValue(data.target, {emitEvent: false});
    //   this.targetNotificationForm.get('configuration.type').updateValueAndValidity({onlySelf: true});
    // }
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
    let formValue = deepTrim(this.ruleNotificationForm.value);
    if (isDefined(this.data.rule)) {
      formValue = Object.assign({}, this.data.rule, formValue);
    }
    this.notificationService.saveNotificationRule(formValue).subscribe(
      (rule) => this.dialogRef.close(rule)
    );
  }
}
