// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, ViewEncapsulation } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { resolveSendErrorMessage } from '@core/utils';
import { AddonType } from '@shared/models/subscription.models';
import { finalize } from 'rxjs/operators';

const PE_PACK_REQUESTED_STORAGE_KEY = 'pePackRequested';

@Component({
  selector: 'tb-request-pack-white-labeling-dialog',
  templateUrl: './request-pack-white-labeling-dialog.component.html',
  styleUrls: ['./request-white-labeling-dialog.component.scss', './request-pack-white-labeling-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class RequestPackWhiteLabelingDialogComponent extends DialogComponent<RequestPackWhiteLabelingDialogComponent>{

  authUser = getCurrentAuthUser(this.store);

  isSysAdmin = this.authUser.authority === Authority.SYS_ADMIN;
  isTenantAdmin = this.authUser.authority === Authority.TENANT_ADMIN;
  isCustomerUser = this.authUser.authority === Authority.CUSTOMER_USER;

  pePackRequested = false;

  sendError = false;
  sendNetworkError = false;
  sendErrorMessage: string | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestPackWhiteLabelingDialogComponent>,
              private translate: TranslateService,
              private notificationService: NotificationService,
              private wl: WhiteLabelingService,
              private cd: ChangeDetectorRef) {
    super(store,  router, dialogRef);
    this.pePackRequested = this.restorePePackRequested();
  }

  cancel(): void {
    this.dialogRef.close();
  }

  requestAccess($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.clearSendErrors();
    this.notificationService.sendAddonAccessRequest(AddonType.PROFESSIONAL_UPGRADE, {ignoreLoading: true, ignoreErrors: true}).subscribe(
      {
        next: () => {
          this.pePackRequested = true;
          this.storePePackRequested();
          this.cd.markForCheck();
        },
        error: err => {
          void this.onSendError(err);
          this.cd.markForCheck();
        }
      }
    );
  }

  addToLicense($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // Dropping the preview first is housekeeping, not a precondition - navigate either way rather than leaving
    // the button doing nothing.
    this.wl.cancelWhiteLabelPreview().pipe(
      finalize(() => this.router.navigateByUrl('license', {state: {skipConfirmOnExit: true}}))
    ).subscribe();
  }

  gotoPage(page: string) {
    // The sysadmin case never reaches this handler — the cards emit nothing when disableNavigation is set.
    this.wl.cancelWhiteLabelPreview().pipe(
      finalize(() => this.router.navigateByUrl(page, {state: {skipConfirmOnExit: true}}))
    ).subscribe();
  }

  private storePePackRequested(): void {
    try {
      localStorage.setItem(PE_PACK_REQUESTED_STORAGE_KEY, JSON.stringify(true));
    } catch {}
  }

  private restorePePackRequested(): boolean {
    try {
      const stored = localStorage.getItem(PE_PACK_REQUESTED_STORAGE_KEY);
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
