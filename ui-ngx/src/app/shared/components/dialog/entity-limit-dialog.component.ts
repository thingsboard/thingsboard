// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { WINDOW } from '@core/services/window.service';
import { SubscriptionEntry, SubscriptionErrorCode, subscriptionErrorsMap } from '@shared/models/subscription.models';
import { TranslateService } from '@ngx-translate/core';

export interface EntityLimitDialogData {
  subscriptionErrorCode: SubscriptionErrorCode;
  subscriptionEntry: SubscriptionEntry;
  value: any;
}

// @dynamic
@Component({
    selector: 'tb-entity-limit-dialog',
    templateUrl: './entity-limit-dialog.component.html',
    styleUrls: ['./entity-limit-dialog.component.scss'],
    standalone: false
})
export class EntityLimitDialogComponent extends DialogComponent<EntityLimitDialogComponent> {

  limitReachedSvg = 'assets/limit-reached.svg';

  errorContent: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: EntityLimitDialogData,
              @Inject(WINDOW) private window: Window,
              public dialogRef: MatDialogRef<EntityLimitDialogComponent>) {
    super(store, router, dialogRef);

    const subscriptionErrorText = subscriptionErrorsMap.get(data.subscriptionErrorCode).get(data.subscriptionEntry);
    this.errorContent = this.translate.instant(subscriptionErrorText, {value: data.value.value});
  }

  public upgrade() {
    this.dialogRef.close();
    this.window.open('https://thingsboard.io/pricing/', '_blank');
  }

  public close() {
    this.dialogRef.close();
  }
}
