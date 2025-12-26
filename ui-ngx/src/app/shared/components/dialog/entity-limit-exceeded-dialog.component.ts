///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Component, Inject, ViewEncapsulation } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { AuthService } from '@core/auth/auth.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { NotificationService } from '@core/http/notification.service';

export interface EntityLimitExceededDialogData {
  entityType: EntityType;
  limit: number;
}

// @dynamic
@Component({
  selector: 'tb-entity-limit-exceeded-dialog',
  templateUrl: './entity-limit-exceeded-dialog.component.html',
  styleUrls: ['./entity-limit-exceeded-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class EntityLimitExceededDialogComponent extends DialogComponent<EntityLimitExceededDialogComponent> {

  limitReachedText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityLimitExceededDialogData,
              public dialogRef: MatDialogRef<EntityLimitExceededDialogComponent>,
              private authService: AuthService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    let entitiesText: string;
    if (data.limit > 1) {
      entitiesText = data.limit + ' ' + (this.translate.instant(entityTypeTranslations.get(data.entityType).typePlural) as string).toLowerCase();
    } else {
      entitiesText = '1 ' + (this.translate.instant(entityTypeTranslations.get(data.entityType).type) as string).toLowerCase();
    }
    this.limitReachedText = this.translate.instant('entity.limit-reached-text', { entities: entitiesText, entity: (this.translate.instant(entityTypeTranslations.get(data.entityType).type) as string).toLowerCase() });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  requestLimitIncrease($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.sendEntitiesLimitIncreaseRequest(this.data.entityType).subscribe(
      () => {
        this.dialogRef.close();
        this.dialogs.alert(
          this.translate.instant('entity.increase-limit-request-sent-title'),
          this.translate.instant('entity.increase-limit-request-sent-text'),
          this.translate.instant('action.close')
        );
      }
    );
  }

  loginAsSysAdmin($event: Event) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    this.authService.redirectUrl = `/tenants/${getCurrentAuthUser(this.store).tenantId}`;
    this.authService.logout();
  }

}
