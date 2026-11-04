// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AuthService } from '@core/auth/auth.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { AddonType } from '@shared/models/subscription.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
    selector: 'tb-request-trendz-dialog',
    templateUrl: './request-trendz-dialog.component.html',
    styleUrls: ['./request-feature-dialog-styles.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RequestTrendzDialogComponent extends DialogComponent<RequestTrendzDialogComponent>{

  isCustomerUser = getCurrentAuthUser(this.store).authority === Authority.CUSTOMER_USER;

  name = this.translate.instant(this.wl.getTrendzName());

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestTrendzDialogComponent>,
              private authService: AuthService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private notificationService: NotificationService,
              private wl: WhiteLabelingService) {
    super(store,  router, dialogRef);
  }

  requestAccess($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }

    this.notificationService.sendAddonAccessRequest(AddonType.TRENDZ).subscribe(() => {
      this.dialogs.alert(
        this.translate.instant('subscription.feature-request-sent-title', {
          addonName: this.translate.instant('subscription.name-addon', {name: this.name})
        }),
        this.translate.instant('subscription.feature-request-sent-text'),
        this.translate.instant('action.close')
      );
    });
  }

  learnMore($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    window.open(`${this.wl.getHelpLinkBaseUrl()}/docs/trendz/`, '_blank');
  }

  loginAsSysAdmin($event: Event) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    this.authService.redirectUrl = '/trendzSettings';
    this.authService.logout();
  }
}
