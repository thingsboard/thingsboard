// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppState } from '@app/core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { NotificationService } from '@core/http/notification.service';
import { AddonType } from '@shared/models/subscription.models';

@Component({
    selector: 'tb-trendz-analytics-unavailable',
    templateUrl: './trendz-analytics-unavailable.component.html',
    styleUrls: ['./trendz-analytics-unavailable.component.scss'],
    standalone: false
})
export class TrendzAnalyticsUnavailableComponent extends DialogComponent<TrendzAnalyticsUnavailableComponent> {

  name = this.translate.instant(this.wl.getTrendzName());

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TrendzAnalyticsUnavailableComponent>,
              private wl: WhiteLabelingService,
              private translate: TranslateService,
              private dialogs: DialogService,
              private notificationService: NotificationService) {
    super(store,  router, dialogRef);
  }

  reportIssue() {
    this.notificationService.sendAddonAccessError(AddonType.TRENDZ).subscribe(() => {
      this.dialogs.alert(
        this.translate.instant('trendz-analytics.service-unavailable-request-sent-title'),
        this.translate.instant('trendz-analytics.service-unavailable-request-sent-message'),
        this.translate.instant('action.close')
      )
    });
  }
}
