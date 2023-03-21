///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  NotificationTarget,
  NotificationTargetConfigType,
  NotificationTargetConfigTypeInfoMap,
  NotificationTargetType,
  NotificationTargetTypeTranslationMap,
  SlackChanelType,
  SlackChanelTypesTranslateMap
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
import { deepTrim, isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';

export interface RecipientNotificationDialogData {
  target?: NotificationTarget;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-target-notification-dialog',
  templateUrl: './recipient-notification-dialog.component.html',
  styleUrls: ['recipient-notification-dialog.componet.scss']
})
export class RecipientNotificationDialogComponent extends
  DialogComponent<RecipientNotificationDialogComponent, NotificationTarget> implements OnDestroy {

  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser: AuthUser = this.authState.authUser;

  targetNotificationForm: FormGroup;
  notificationTargetType = NotificationTargetType;
  notificationTargetTypes: NotificationTargetType[] = Object.values(NotificationTargetType);
  notificationTargetTypeTranslationMap = NotificationTargetTypeTranslationMap;
  notificationTargetConfigType = NotificationTargetConfigType;
  notificationTargetConfigTypes: NotificationTargetConfigType[] = this.allowNotificationTargetConfigTypes();
  notificationTargetConfigTypeInfoMap = NotificationTargetConfigTypeInfoMap;
  slackChanelTypes = Object.keys(SlackChanelType) as SlackChanelType[];
  slackChanelTypesTranslateMap = SlackChanelTypesTranslateMap;

  entityType = EntityType;
  isAdd = true;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RecipientNotificationDialogComponent, NotificationTarget>,
              @Inject(MAT_DIALOG_DATA) public data: RecipientNotificationDialogData,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (isDefinedAndNotNull(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.targetNotificationForm = this.fb.group({
      name: [null, Validators.required],
      configuration: this.fb.group({
        type: [NotificationTargetType.PLATFORM_USERS],
        usersFilter: this.fb.group({
          type: [NotificationTargetConfigType.ALL_USERS],
          filterByTenants: [{value: true, disabled: true}],
          tenantsIds: [{value: null, disabled: true}],
          tenantProfilesIds: [{value: null, disabled: true}],
          customerId: [{value: null, disabled: true}, Validators.required],
          usersIds: [{value: null, disabled: true}, Validators.required],
        }),
        conversationType: [{value: SlackChanelType.PUBLIC_CHANNEL, disabled: true}],
        conversation: [{value: '', disabled: true}, Validators.required],
        description: [null]
      })
    });

    this.targetNotificationForm.get('configuration.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: NotificationTargetType) => {
      this.targetNotificationForm.get('configuration').disable({emitEvent: false});
      switch (type) {
        case NotificationTargetType.PLATFORM_USERS:
          this.targetNotificationForm.get('configuration.usersFilter').enable({emitEvent: false});
          this.targetNotificationForm.get('configuration.usersFilter.type').updateValueAndValidity({onlySelf: true});
          break;
        case NotificationTargetType.SLACK:
          this.targetNotificationForm.get('configuration.conversationType').enable({emitEvent: false});
          this.targetNotificationForm.get('configuration.conversation').enable({emitEvent: false});
          break;
      }
      this.targetNotificationForm.get('configuration.type').enable({emitEvent: false});
      this.targetNotificationForm.get('configuration.description').enable({emitEvent: false});
    });

    this.targetNotificationForm.get('configuration.usersFilter.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: NotificationTargetConfigType) => {
      this.targetNotificationForm.get('configuration.usersFilter').disable({emitEvent: false});
      switch (type) {
        case NotificationTargetConfigType.TENANT_ADMINISTRATORS:
          if (this.isSysAdmin()) {
            this.targetNotificationForm.get('configuration.usersFilter.filterByTenants').enable({onlySelf: true});
          }
          break;
        case NotificationTargetConfigType.USER_LIST:
          this.targetNotificationForm.get('configuration.usersFilter.usersIds').enable({emitEvent: false});
          break;
        case NotificationTargetConfigType.CUSTOMER_USERS:
          this.targetNotificationForm.get('configuration.usersFilter.customerId').enable({emitEvent: false});
          break;
      }
      this.targetNotificationForm.get('configuration.usersFilter.type').enable({emitEvent: false});
    });

    this.targetNotificationForm.get('configuration.usersFilter.filterByTenants').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: boolean) => {
      if (value) {
        this.targetNotificationForm.get('configuration.usersFilter.tenantsIds').enable({emitEvent: false});
        this.targetNotificationForm.get('configuration.usersFilter.tenantProfilesIds').disable({emitEvent: false});
      } else {
        this.targetNotificationForm.get('configuration.usersFilter.tenantsIds').disable({emitEvent: false});
        this.targetNotificationForm.get('configuration.usersFilter.tenantProfilesIds').enable({emitEvent: false});
      }
    });

    if (isDefinedAndNotNull(data.target)) {
      this.targetNotificationForm.patchValue(data.target, {emitEvent: false});
      this.targetNotificationForm.get('configuration.type').updateValueAndValidity({onlySelf: true});
      if (this.isSysAdmin() && data.target.configuration.usersFilter.type === NotificationTargetConfigType.TENANT_ADMINISTRATORS) {
        this.targetNotificationForm.get('configuration.usersFilter.filterByTenants')
          .patchValue(!Array.isArray(this.data.target.configuration.usersFilter.tenantProfilesIds), {onlySelf: true});
      }
    }
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
    let formValue = deepTrim(this.targetNotificationForm.value);
    if (isDefinedAndNotNull(this.data.target)) {
      formValue = Object.assign({}, this.data.target, formValue);
    }
    if (this.isSysAdmin() && formValue.configuration.type === NotificationTargetType.PLATFORM_USERS &&
      formValue.configuration.usersFilter.type === NotificationTargetConfigType.TENANT_ADMINISTRATORS) {
      delete formValue.configuration.usersFilter.filterByTenants;
    }
    this.notificationService.saveNotificationTarget(formValue).subscribe(
      (target) => this.dialogRef.close(target)
    );
  }

  isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  private allowNotificationTargetConfigTypes(): NotificationTargetConfigType[] {
    if (this.isSysAdmin()) {
      return [NotificationTargetConfigType.ALL_USERS, NotificationTargetConfigType.TENANT_ADMINISTRATORS];
    }
    return Object.values(NotificationTargetConfigType);
  }
}
