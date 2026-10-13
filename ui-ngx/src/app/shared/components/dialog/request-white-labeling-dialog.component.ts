// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { AddonType } from '@shared/models/subscription.models';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { finalize } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { resolveSendErrorMessage } from '@core/utils';

const WL_REQUESTED_STORAGE_KEY = 'wlRequested';

@Component({
    selector: 'tb-request-white-labeling-dialog',
    templateUrl: './request-white-labeling-dialog.component.html',
    styleUrls: ['./request-white-labeling-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RequestWhiteLabelingDialogComponent extends DialogComponent<RequestWhiteLabelingDialogComponent>{

  authUser = getCurrentAuthUser(this.store);

  isOldLicense = !(getCurrentAuthState(this.store).licenseVersion > 1);
  isSysAdmin = this.authUser.authority === Authority.SYS_ADMIN;
  isTenantAdmin = this.authUser.authority === Authority.TENANT_ADMIN;

  wlRequested = false;

  sendError = false;
  sendNetworkError = false;
  sendErrorMessage: string | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestWhiteLabelingDialogComponent>,
              private translate: TranslateService,
              private notificationService: NotificationService,
              private wl: WhiteLabelingService,
              private cd: ChangeDetectorRef) {
    super(store,  router, dialogRef);
    this.wlRequested = this.restoreWlRequested();
  }

  cancel(): void {
    this.dialogRef.close();
  }

  requestAccess($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.clearSendErrors();
    this.notificationService.sendAddonAccessRequest(AddonType.WHITE_LABELING, {ignoreLoading: true, ignoreErrors: true}).subscribe(
      {
        next: () => {
          this.wlRequested = true;
          this.storeWlRequested();
          this.cd.markForCheck();
        },
        error: err => {
          void this.onSendError(err);
          this.cd.markForCheck();
        }
      }
    );
  }

  upgradePlan($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isOldLicense) {
      window.open('https://thingsboard.io/pricing/', '_blank');
    } else {
      // Dropping the preview first is housekeeping, not a precondition - navigate either way rather than leaving
      // the button doing nothing.
      this.wl.cancelWhiteLabelPreview().pipe(
        finalize(() => this.router.navigateByUrl('license', {state: {skipConfirmOnExit: true}}))
      ).subscribe();
    }
  }

  private storeWlRequested(): void {
    try {
      localStorage.setItem(WL_REQUESTED_STORAGE_KEY, JSON.stringify(true));
    } catch {}
  }

  private restoreWlRequested(): boolean {
    try {
      const stored = localStorage.getItem(WL_REQUESTED_STORAGE_KEY);
      return stored ? (JSON.parse(stored) === true) : false;
    } catch {
      return false;
    }
  }

  private clearSendErrors(): void {
    this.sendError = false;
    this.sendNetworkError = false;
    this.sendErrorMessage = null;
  }

  private async onSendError(err: any): Promise<void> {
    this.sendError = true;
    // Asked of the failure itself, not of whether a message could be got out of it: status 0 means the browser
    // never reached a server, which is what all five readers of this flag are actually asking.
    this.sendNetworkError = err?.status === 0;
    this.sendErrorMessage = this.sendNetworkError ? null : await resolveSendErrorMessage(err, this.translate);
  }

}
