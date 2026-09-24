// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialogRef } from '@angular/material/dialog';
import { STEPPER_GLOBAL_OPTIONS } from '@angular/cdk/stepper';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { ClipboardService } from 'ngx-clipboard';
import { TranslateService } from '@ngx-translate/core';
import { Subscription, interval, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { CommunityGrantService } from '@core/http/community-grant.service';
import { RequestConfig } from '@core/http/http-utils';
import {
  CommunityGrantBackendMode,
  CommunityGrantBackendState,
  CommunityGrantBundleErrorCode,
  CommunityGrantBundleParseError,
  CommunityGrantParkReason,
  CommunityGrantRegistration,
  CommunityGrantState,
  CommunityGrantStateConfig,
  communityGrantOnlinePollableStates,
  communityGrantPollInterval,
  communityGrantPortalUrl,
  communityGrantRailLabels,
  communityGrantStallTimeout,
  communityGrantStateConfigs,
  communityGrantStateTitles,
  peekCommunityGrantBundle,
  resolveCommunityGrantErrorMessage
} from '@shared/models/ce-grant/community-grant.models';

// The calling button shows both the error and the wait, so no toast and no global loading bar.
const startRequestConfig: RequestConfig = {ignoreErrors: true, ignoreLoading: true};

@Component({
    selector: 'tb-community-grant-dialog',
    templateUrl: './community-grant-dialog.component.html',
    styleUrls: ['./ce-grant-tokens.scss', './community-grant-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    // Lets `[state]` pick the node icons instead of Material's default indicator logic.
    providers: [{provide: STEPPER_GLOBAL_OPTIONS, useValue: {displayDefaultIndicatorType: false}}],
    standalone: false
})
export class CommunityGrantDialogComponent extends DialogComponent<CommunityGrantDialogComponent>
  implements OnInit, OnDestroy {

  communityGrantState = CommunityGrantState;
  communityGrantBundleErrorCode = CommunityGrantBundleErrorCode;

  get portalUrl(): string {
    return this.registration.portalUrl ?? communityGrantPortalUrl;
  }

  /** `intro` is only the default, so no state screen renders until the first `GET /state` answers. */
  stateKnown = false;

  initialLoadFailed = false;

  state = CommunityGrantState.intro;

  registration: CommunityGrantRegistration = {state: CommunityGrantState.intro};

  registerPending = false;
  retryPending = false;
  runCheckPending = false;
  startAgainPending = false;
  requestAccessPending = false;

  registerErrorMessage: string = null;

  stalled = false;

  accessRequested = false;
  requestAccessErrorMessage: string = null;

  bundleFile: File = null;
  noBundleSelected = false;
  bundleError: CommunityGrantBundleErrorCode = null;
  bundleExpiredAt: number = null;
  checkFailed = false;
  checkFailedMessage: string = null;

  copiedValue: string = null;

  reportPending = false;
  reportLoadError: string = null;

  private verifyEntryState = CommunityGrantState.offline;

  private pollSubscription: Subscription;
  private stallSubscription: Subscription;
  private initialLoadSubscription: Subscription;
  private handoffSubscription: Subscription;
  private copiedTimer: Subscription;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<CommunityGrantDialogComponent>,
              private communityGrantService: CommunityGrantService,
              private dialogService: DialogService,
              private clipboardService: ClipboardService,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
    super(store, router, dialogRef);
    dialogRef.addPanelClass('tb-ce-grant-dialog-panel');
    this.updatePanelWidth();
  }

  ngOnInit(): void {
    this.loadInitialState();
  }

  private loadInitialState(): void {
    this.initialLoadFailed = false;
    this.initialLoadSubscription?.unsubscribe();
    this.initialLoadSubscription = this.communityGrantService.pollRegistration().subscribe({
      next: (registration) => {
        this.stateKnown = true;
        this.applyRegistration(registration);
      },
      error: () => {
        this.initialLoadFailed = true;
        this.cd.markForCheck();
      }
    });
  }

  retryInitialLoad(): void {
    this.loadInitialState();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.initialLoadSubscription?.unsubscribe();
    this.handoffSubscription?.unsubscribe();
    this.copiedTimer?.unsubscribe();
    super.ngOnDestroy();
  }

  get config(): CommunityGrantStateConfig {
    return communityGrantStateConfigs.get(this.state);
  }

  get checkFailedPark(): boolean {
    return this.isPark(CommunityGrantParkReason.CHECK_FAILED);
  }

  get portalUnreachablePark(): boolean {
    return this.isPark(CommunityGrantParkReason.PORTAL_UNREACHABLE);
  }

  private isPark(reason: CommunityGrantParkReason): boolean {
    return this.state === CommunityGrantState.expired && this.registration.parkReason === reason;
  }

  get title(): string {
    if (this.checkFailedPark) {
      return 'ce-grant.check-failed-title';
    }
    if (this.portalUnreachablePark) {
      return 'ce-grant.portal-unreachable-title';
    }
    return communityGrantStateTitles.get(this.state);
  }

  get railLabels(): string[] {
    return communityGrantRailLabels.get(this.config.rail) ?? [];
  }

  get railSelectedIndex(): number {
    return Math.min(this.config.railStep, this.railLabels.length - 1);
  }

  railStepState(index: number): string {
    return index < this.config.railStep ? 'done' : 'number';
  }

  get registrationLink(): string {
    return this.registration.registrationLink;
  }

  isCopied(value: string): boolean {
    return this.copiedValue === value;
  }

  copy(value: string): void {
    this.clipboardService.copy(value);
    this.copiedValue = value;
    this.copiedTimer?.unsubscribe();
    this.copiedTimer = timer(1600).subscribe(() => {
      this.copiedValue = null;
      this.cd.markForCheck();
    });
  }

  private openLink(url: string): void {
    window.open(url, '_blank');
  }

  loadReport(): void {
    this.reportPending = true;
    this.reportLoadError = null;
    this.communityGrantService.downloadOfflineReport({ignoreErrors: true}).subscribe({
      next: (report) => {
        this.reportPending = false;
        if (!report) {
          this.reportLoadError = this.translate.instant('ce-grant.result-error-empty');
          this.cd.markForCheck();
          return;
        }
        this.registration = {...this.registration, report};
        this.cd.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.reportPending = false;
        this.reportLoadError = this.extractErrorMessage(err, 'text');
        this.cd.markForCheck();
      }
    });
  }

  register(open = true): void {
    this.registerPending = true;
    this.registerErrorMessage = null;
    this.communityGrantService.register(startRequestConfig).subscribe({
      next: (registration) => {
        this.registerPending = false;
        this.applyRegistration(registration);
        if (open && this.registrationLink && this.state === CommunityGrantState.awaiting) {
          this.openLink(this.registrationLink);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.registerPending = false;
        this.registerErrorMessage = this.extractErrorMessage(err);
        this.cd.markForCheck();
      }
    });
  }

  /** Re-mints over the live link this screen shows, which stops working. */
  retry(): void {
    this.retryPending = true;
    this.registerErrorMessage = null;
    this.communityGrantService.register(startRequestConfig).subscribe({
      next: (registration) => {
        this.retryPending = false;
        this.applyRegistration(registration);
      },
      error: (err: HttpErrorResponse) => {
        this.retryPending = false;
        this.registerErrorMessage = this.extractErrorMessage(err);
        this.cd.markForCheck();
      }
    });
  }

  startAgain(): void {
    this.startAgainPending = true;
    this.registerErrorMessage = null;
    this.communityGrantService.register(startRequestConfig).subscribe({
      next: (registration) => {
        this.startAgainPending = false;
        this.applyRegistration(registration);
      },
      error: (err: HttpErrorResponse) => {
        this.startAgainPending = false;
        this.registerErrorMessage = this.extractErrorMessage(err);
        this.cd.markForCheck();
      }
    });
  }

  requestAccess(): void {
    this.requestAccessPending = true;
    this.requestAccessErrorMessage = null;
    this.communityGrantService.requestAccess({ignoreErrors: true}).subscribe({
      next: () => {
        this.requestAccessPending = false;
        this.accessRequested = true;
        this.cd.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.requestAccessPending = false;
        this.requestAccessErrorMessage = this.extractErrorMessage(err);
        this.cd.markForCheck();
      }
    });
  }

  continueToCheck(): void {
    this.verifyEntryState = CommunityGrantState.offline;
    this.goToState(CommunityGrantState.verify);
  }

  runCheckByHand(): void {
    this.verifyEntryState = CommunityGrantState.verifying;
    this.goToState(CommunityGrantState.verify);
  }

  backFromVerify(): void {
    this.goToState(this.verifyEntryState);
  }

  bundleFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.bundleFile = input.files?.length ? input.files[0] : null;
    this.noBundleSelected = false;
    this.bundleError = null;
    this.bundleExpiredAt = null;
    this.checkFailed = false;
    this.checkFailedMessage = null;
    const chosen = this.bundleFile;
    if (!chosen) {
      this.cd.markForCheck();
      return;
    }
    peekCommunityGrantBundle(chosen).then(preflight => {
      if (this.bundleFile !== chosen) {
        return;
      }
      this.bundleExpiredAt = preflight.challengeExpired ? preflight.manifest.challengeExpiresAt : null;
      this.cd.markForCheck();
    }).catch((e: unknown) => {
      if (this.bundleFile !== chosen) {
        return;
      }
      this.bundleError = e instanceof CommunityGrantBundleParseError
        ? e.code : CommunityGrantBundleErrorCode.MALFORMED;
      this.cd.markForCheck();
    });
    this.cd.markForCheck();
  }

  clearBundleFile(bundleInput: HTMLInputElement): void {
    bundleInput.value = '';
    this.bundleFile = null;
    this.noBundleSelected = false;
    this.bundleError = null;
    this.bundleExpiredAt = null;
    this.checkFailed = false;
    this.checkFailedMessage = null;
    this.cd.markForCheck();
  }

  runCheck(): void {
    this.noBundleSelected = !this.bundleFile;
    this.checkFailed = false;
    this.checkFailedMessage = null;
    if (this.noBundleSelected || this.bundleError) {
      this.cd.markForCheck();
      return;
    }
    this.runCheckPending = true;
    this.communityGrantService.runOfflineScaleCheck(this.bundleFile).subscribe({
      next: (registration) => this.applyRegistration(registration),
      error: (err: HttpErrorResponse) => {
        this.runCheckPending = false;
        this.checkFailed = true;
        this.checkFailedMessage = this.extractErrorMessage(err);
        this.cd.markForCheck();
      }
    });
  }

  private extractErrorMessage(error: HttpErrorResponse, responseType?: string): string {
    return resolveCommunityGrantErrorMessage(error, this.translate, responseType);
  }

  /** A refusal usually means the poller moved the state on first, so the state is re-read instead. */
  confirmHandoff(): void {
    this.handoffSubscription?.unsubscribe();
    this.handoffSubscription = this.communityGrantService.confirmOfflineHandoff({ignoreErrors: true}).subscribe({
      next: () => this.close(),
      error: () => {
        this.handoffSubscription = this.communityGrantService.pollRegistration({ignoreErrors: true})
          .subscribe(registration => this.applyRegistration(registration));
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  /** A by-hand run in progress holds the `verify` screen, whatever state the flow is in. */
  private applyRegistration(registration: CommunityGrantRegistration): void {
    const awaitedRun = this.runCheckPending;
    this.registration = registration;
    this.runCheckPending = !!registration.offlineRunInProgress;
    const failed = awaitedRun && !this.runCheckPending && !!registration.offlineRunError;
    if (!this.runCheckPending && !failed) {
      this.goToState(registration.state);
      return;
    }
    this.showVerify();
    if (failed) {
      this.checkFailed = true;
      this.checkFailedMessage = registration.offlineRunError;
    }
    this.cd.markForCheck();
  }

  /** Stays on `verify` without {@link goToState}, which would clear the bundle's pre-flight warning. */
  private showVerify(): void {
    if (this.state !== CommunityGrantState.verify) {
      this.verifyEntryState = this.registration.state;
      this.goToState(CommunityGrantState.verify);
      return;
    }
    if (this.runCheckPending && !this.pollSubscription) {
      this.startPolling();
    } else if (!this.runCheckPending) {
      this.stopPolling();
    }
  }

  private goToState(state: CommunityGrantState): void {
    // `VALIDATING` + `ONLINE` with a report is a by-hand run's result, not the automatic wait.
    if (state === CommunityGrantState.verifying && this.registration.report
        && this.registration.backendState === CommunityGrantBackendState.VALIDATING
        && this.registration.mode === CommunityGrantBackendMode.ONLINE) {
      state = CommunityGrantState.result;
    }
    this.state = state;
    this.stalled = false;
    this.registerErrorMessage = null;
    this.noBundleSelected = false;
    this.bundleError = null;
    this.bundleExpiredAt = null;
    this.checkFailed = false;
    this.checkFailedMessage = null;
    this.reportPending = false;
    this.reportLoadError = null;
    this.requestAccessErrorMessage = null;
    this.stopPolling();
    if (communityGrantOnlinePollableStates.has(state) || this.runCheckPending) {
      this.startPolling();
    }
    if ((state === CommunityGrantState.result || state === CommunityGrantState.handedOver)
        && !this.registration.report) {
      // The state omits the report when the server could not read it, so it is fetched on its own.
      this.loadReport();
    }
    this.updatePanelWidth();
    this.cd.markForCheck();
  }

  private updatePanelWidth(): void {
    if (this.stateKnown && this.config.hero) {
      this.dialogRef.addPanelClass('tb-ce-grant-dialog-panel-wide');
    } else {
      this.dialogRef.removePanelClass('tb-ce-grant-dialog-panel-wide');
    }
  }

  /** Compares more than the UI state: the mapping is many-to-one, and a re-mint changes only the link. */
  private startPolling(): void {
    this.pollSubscription = interval(communityGrantPollInterval).pipe(
      switchMap(() => this.communityGrantService.pollRegistration({ignoreLoading: true, ignoreErrors: true}))
    ).subscribe((registration) => {
      // A run in progress holds `verify`, so a differing mapped state alone is not a change.
      const holdsVerify = this.runCheckPending && registration.offlineRunInProgress;
      if ((!holdsVerify && registration.state !== this.state)
          || registration.backendState !== this.registration.backendState
          || registration.mode !== this.registration.mode
          || registration.report !== this.registration.report
          || registration.registrationLink !== this.registration.registrationLink
          || registration.offlineRunInProgress !== this.registration.offlineRunInProgress
          || registration.offlineRunError !== this.registration.offlineRunError) {
        this.applyRegistration(registration);
      }
    });
    this.stallSubscription = timer(communityGrantStallTimeout).subscribe(() => {
      this.stalled = true;
      this.cd.markForCheck();
    });
  }

  private stopPolling(): void {
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
    this.stallSubscription?.unsubscribe();
    this.stallSubscription = null;
  }
}
