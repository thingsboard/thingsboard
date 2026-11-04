// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DOCUMENT, EventEmitter, Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { defaultHttpOptionsFromConfig } from '@core/http/http-utils';
import {
  LicenseChangeResult,
  LicenseClaimInfo,
  LicenseClaimResult,
  LicenseKeyRequest,
  SetupInfo,
  SystemSetupRequest,
  SystemSetupState
} from '@shared/models/system-setup.models';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { updateUserLang } from '@core/settings/settings.utils';
import { TranslateService, TranslateStore } from '@ngx-translate/core';
import { TranslateDefaultLoader } from '@core/translate/translate-default-loader';
import { UtilsService } from '@core/services/utils.service';
import { SubscriptionInfo } from '@shared/models/subscription.models';

@Injectable({
  providedIn: 'root'
})
export class SystemSetupService {

  private setupComplete = new EventEmitter<boolean>();

  private state: SystemSetupState;

  /** One-time by construction: app.component subscribes once, and re-running it re-authenticates a live session. */
  private startUpHandOffMade = false;

  constructor(private store: Store<AppState>,
              private http: HttpClient,
              private translate: TranslateService,
              private translateStore: TranslateStore,
              @Inject(DOCUMENT) private document: Document,
              private router: Router,
              private utils: UtilsService) {}

  /**
   * The whole application waits on this emission - nothing else installs the auth wiring or loads the user, so a
   * subscriber that never emits leaves the browser on the spinner. An unreadable state therefore proceeds as
   * READY; if the instance really is locked, its first API call answers 423 and the interceptor takes over.
   */
  public checkSetupState(): Observable<boolean> {
    this.getSetupState().subscribe({
      next: (setupInfo) => this.handleSetupState(this.readableState(setupInfo), false),
      error: () => this.handleSetupState(SystemSetupState.READY, false)
    });
    return this.setupComplete.asObservable();
  }

  private readableState(setupInfo: SetupInfo): SystemSetupState {
    const status = setupInfo?.status;
    return !!status && Object.values(SystemSetupState).includes(status) ? status : SystemSetupState.READY;
  }

  public handleSetupState(state: SystemSetupState, gotoDefaultPlace = true): void {
    this.state = state;
    if (state === SystemSetupState.READY) {
      this.makeStartUpHandOff(gotoDefaultPlace);
    } else {
      this.initTranslate(false).subscribe(() => {
        $('link[rel="icon"]').attr('href', 'thingsboard.ico');
        const haveKey = this.utils.getQueryParam('haveKey') === 'true';
        this.router.navigate(['activation'], {queryParams: haveKey ? {haveKey} : {}}).then(() => {});
      });
    }
  }

  /**
   * Called by the setup screen, which has already established the session and decides where the user lands - so
   * it does not route, and does not re-run the start-up hand-off if that already happened. Leaves one thing to
   * put back by hand: the translations handleSetupState() swapped for the setup screen's bundled ones.
   */
  public finishSetup(): void {
    this.state = SystemSetupState.READY;
    if (this.startUpHandOffMade) {
      this.initTranslate(true).subscribe();
      return;
    }
    this.makeStartUpHandOff(false);
  }

  private makeStartUpHandOff(gotoDefaultPlace: boolean): void {
    if (this.startUpHandOffMade) {
      return;
    }
    this.startUpHandOffMade = true;
    this.setupComplete.emit(gotoDefaultPlace);
  }

  public getState(): SystemSetupState {
    return this.state;
  }

  // Every caller of these endpoints renders its own errors, so the interceptor must not toast them as well.
  public getSetupState(): Observable<SetupInfo> {
    return this.http.get<SetupInfo>('/api/noauth/setup/state',
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  /**
   * The claim token names which claim the caller is watching: only one is outstanding per installation, so a
   * second session starting its own activation replaces this one's, and naming it is what gets the superseded
   * session an EXPIRED instead of a PENDING on a link nobody can activate. Omitted when none is held, which
   * reports on whatever claim is stored.
   */
  public getClaimInfo(claimToken?: string): Observable<LicenseClaimInfo> {
    return this.http.get<LicenseClaimInfo>('/api/noauth/setup/claim',
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true, queryParams: {claimToken}}));
  }

  /** Bodyless: the claim is minted from the instance's own identity, so there is nothing for a caller to send. */
  public requestClaim(): Observable<LicenseClaimResult> {
    return this.http.post<LicenseClaimResult>('/api/noauth/setup/claim', null,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  public applyLicenseKey(secret: string): Observable<SetupInfo> {
    const body: LicenseKeyRequest = { secret };
    return this.http.post<SetupInfo>('/api/noauth/setup/license', body,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}))
      .pipe(tap((info) => this.state = info?.status));
  }

  public changeLicenseKey(secret: string): Observable<LicenseChangeResult> {
    const body: LicenseKeyRequest = { secret };
    return this.http.post<LicenseChangeResult>('/api/admin/license/key', body,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}))
    .pipe(tap((info) => this.state = info?.status));
  }

  public previewLicenseKey(secret: string): Observable<SubscriptionInfo> {
    const body: LicenseKeyRequest = { secret };
    return this.http.post<SubscriptionInfo>('/api/admin/license/preview', body,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  public completeSetup(request: SystemSetupRequest): Observable<void> {
    return this.http.post<void>('/api/noauth/setup/complete', request,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  /** Sysadmin-authenticated, not under /api/noauth/. */
  public confirmNonProduction(): Observable<void> {
    return this.http.post<void>('/api/admin/nonProduction/confirm', null,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  private initTranslate(setupCompleted: boolean): Observable<any> {
    (this.translate.currentLoader as TranslateDefaultLoader).isSetupCompleted = setupCompleted;
    const availableLocales = getCurrentAuthState(this.store)?.availableLocales;
    return updateUserLang(this.translate, this.translateStore, this.document, null, availableLocales, true);
  }
}
