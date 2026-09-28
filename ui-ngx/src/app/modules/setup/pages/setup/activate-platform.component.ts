// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { environment } from '@env/environment';
import {
  LicenseClaimMode,
  LicenseClaimStatus,
  SystemSetupRequest,
  SystemSetupState
} from '@shared/models/system-setup.models';
import { SystemSetupService } from '@core/http/system-setup.service';
import {
  catchError, EMPTY, filter, interval, map, Observable, Subject, Subscription, switchMap, take, takeUntil, of,
  from
} from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '@core/auth/auth.service';
import { passwordsMatchValidator } from '@shared/models/password.models';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { select } from '@ngrx/store';
import {
  getCurrentAuthState,
  selectAuthUser,
  selectIsAuthenticated,
  selectIsUserLoaded
} from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { baseUrl, resolveSendErrorMessage, unwrapModule } from '@core/utils';

enum ActivationState {
  INITIAL = 'INITIAL',
  WAITING = 'WAITING',
  OFFLINE = 'OFFLINE',
  HAVE_KEY = 'HAVE_KEY',
  SETUP_ACCOUNT = 'SETUP_ACCOUNT',
  DEMO_TENANT_OFFER = 'DEMO_TENANT_OFFER',
  NON_PRODUCTION_CONFIRM = 'NON_PRODUCTION_CONFIRM'
}

interface StoredActivationClaim {
  activationLink: string;
  mode: LicenseClaimMode;
  /** Optional: entries written before the poll became token-aware carry none, and restore without one. */
  claimToken?: string;
}

const LICENSE_PORTAL_HOST_NAME = 'license.thingsboard.io';
const ACTIVATION_CLAIM_STORAGE_KEY = 'activationClaim';
const KEY_MIN_LEN = 8;
// The GET /noauth/setup/claim endpoint is throttled server-side (at most one
// outbound portal call per interval) so a snappy client cadence is safe.
const ACTIVATION_POLL_INTERVAL_MS = 3000;

const DEMO_TENANT_EMAIL = 'tenant@thingsboard.org';
const DEMO_TENANT_PASSWORD = 'tenant';

const DEMO_TENANT_CREDS_CONTENT = `ThingsBoard demo tenant — tenant administrator
Email: ${DEMO_TENANT_EMAIL}
Password: ${DEMO_TENANT_PASSWORD}`;

@Component({
  selector: 'tb-activate-platform',
  templateUrl: './activate-platform.component.html',
  styleUrls: ['./setup-page.scss'],
  standalone: false
})
export class ActivatePlatformComponent extends PageComponent implements OnInit, OnDestroy {

  get stateClass(): string {
    switch (this.activationState) {
      case ActivationState.INITIAL:
        return 'tb-initial';
      case ActivationState.WAITING:
        return 'tb-waiting';
      case ActivationState.OFFLINE:
        return 'tb-offline';
      case ActivationState.HAVE_KEY:
        return 'tb-key';
      case ActivationState.SETUP_ACCOUNT:
        return 'tb-setup-account';
      case ActivationState.DEMO_TENANT_OFFER:
        return 'tb-demo-tenant';
      case ActivationState.NON_PRODUCTION_CONFIRM:
        return 'tb-non-production';
      default:
        return '';
    }
  }

  ActivationState = ActivationState;

  licensePortalHostName = LICENSE_PORTAL_HOST_NAME;
  licensePortalActivationLink = '';
  licensePortalActivationLinkQrCodeSVG = '';
  copyingActivationLink = false;

  demoTenantEmail = DEMO_TENANT_EMAIL;
  demoTenantPassword = DEMO_TENANT_PASSWORD;
  demoTenantCredsContent = DEMO_TENANT_CREDS_CONTENT;
  copyingDemoTenantCreds = false;

  /** The mode the claim was minted with, as the server's own reachability probe read it. */
  claimMode: LicenseClaimMode;

  /**
   * The token this session's own claim was minted with. Named on every poll so that a second session starting
   * its own activation - which replaces this token server-side - leaves this one watching a dead link and told
   * so, rather than polling the other session's live claim for ever.
   */
  private claimToken: string;

  /** Only a signed-in sysadmin gets the Confirm control; everyone else gets the sign-in affordance. */
  isSysAdminViewer$: Observable<boolean> = this.store.pipe(
    select(selectAuthUser),
    map((authUser) => authUser?.authority === Authority.SYS_ADMIN)
  );

  activationState: ActivationState;

  env = environment;

  sendError = false;
  sendNetworkError = false;
  sendErrorMessage: string | null = null;

  keyInput = '';
  keyError = false;

  keyErrorStateMatcher: ErrorStateMatcher = {
    isErrorState: (control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean => {
      return !!(control && (this.keyError || this.sendError) && (control.dirty || control.touched));
    }
  };

  activating = false;
  activatingKey = false;
  submittingAccount = false;
  confirmingNonProduction = false;
  sysAdminEmail = '';

  setupAccountForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
    loadDemo: [true]
  }, {
    validators: [
      passwordsMatchValidator('password', 'confirmPassword'),
    ]
  });

  private poll?: Subscription;
  /** Keeps the retried setup-state read one at a time while the poll keeps reporting the same activation. */
  private readingActivatedSetupState = false;
  private readonly destroy$ = new Subject<void>();

  constructor(private systemSetupService: SystemSetupService,
              private fb: FormBuilder,
              private authService: AuthService,
              private translate: TranslateService,
              private route: ActivatedRoute,
              private router: Router) {
    super();
  }

  ngOnInit(): void {
    const state = this.systemSetupService.getState();
    if (state === SystemSetupState.ACCOUNT_REQUIRED) {
      this.activationState = ActivationState.SETUP_ACCOUNT;
    } else if (state === SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED) {
      this.activationState = ActivationState.NON_PRODUCTION_CONFIRM;
      this.ensureAuthInitialized();
    } else {
      this.restoreActivationClaim().subscribe((restored) => {
        if (restored) {
          this.activationState = this.claimWaitingState();
          this.startPolling();
        } else {
          if (this.route.snapshot.queryParams.haveKey) {
            void this.router.navigate([], {
              relativeTo: this.route,
              queryParams: {},
              queryParamsHandling: '',
              replaceUrl: true
            });
            this.activationState = ActivationState.HAVE_KEY;
          } else {
            this.activationState = ActivationState.INITIAL;
          }
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopPolling();
    super.ngOnDestroy();
  }

  gotoLicensePortal(): void {
    if (this.activating) {
      // The request mints a claim token, and a second one would retire the link the first just handed out.
      return;
    }
    this.clearSendErrors();
    this.activating = true;
    this.systemSetupService.requestClaim().subscribe({
      next: (result) => {
        this.activating = false;
        if (!result?.signUpUrl) {
          // Nothing to open, copy or encode, so there is no automatic path to show: stay put, say so, and leave
          // the key form as the way in.
          this.sendError = true;
          this.sendNetworkError = false;
          this.sendErrorMessage = this.translate.instant('setup.errors.no-sign-up-url');
          return;
        }
        this.claimMode = result.mode;
        this.claimToken = result.claimToken;
        from(this.computeLink(result.signUpUrl)).subscribe(() => {
          this.storeActivationClaim();
          this.activationState = this.claimWaitingState();
          this.startPolling();
          if (this.claimMode !== LicenseClaimMode.OFFLINE) {
            this.openActivationLink();
          }
        });
      },
      error: (err) => {
        this.activating = false;
        void this.onSendError(err);
      }
    });
  }

  /**
   * OFFLINE says the server could not reach the portal - it says nothing about the machine the operator is
   * sitting at, so the link is rendered in full either way. The offline screen only adds the step of bringing
   * the key back by hand, and watching keeps running in case the server does reach the portal after all.
   */
  private claimWaitingState(): ActivationState {
    return this.claimMode === LicenseClaimMode.OFFLINE ? ActivationState.OFFLINE : ActivationState.WAITING;
  }

  private async computeLink(signUpUrl: string): Promise<void> {
    const url = new URL(signUpUrl);
    url.searchParams.set('platformUrl', baseUrl());
    this.licensePortalActivationLink = url.toString();
    await this.renderActivationLinkQrCode();
  }

  private async renderActivationLinkQrCode(): Promise<void> {
    const qrCode = await import('qrcode');
    unwrapModule(qrCode).toString(this.licensePortalActivationLink, {margin: 0}, (_err, svgElement) => {
      this.licensePortalActivationLinkQrCodeSVG = svgElement;
    });
  }

  copyActivationLink(): void {
    this.copyingActivationLink = true;
    setTimeout(() => {
      this.copyingActivationLink = false;
    }, 1600);
  }

  openActivationLink(): void {
    window.open(this.licensePortalActivationLink, '_blank');
  }

  gotoHaveKey(): void {
    this.keyInput = '';
    this.stopPolling();
    this.clearKeyErrors();
    this.activationState = ActivationState.HAVE_KEY;
  }

  goBack(): void {
    this.restoreActivationClaim().subscribe((restored) => {
      if (restored) {
        this.backToWaiting();
      } else {
        this.gotoInitial();
      }
    });
  }

  copyDemoTenantCreds(): void {
    this.copyingDemoTenantCreds = true;
    setTimeout(() => {
      this.copyingDemoTenantCreds = false;
    }, 1600);
  }

  private gotoInitial(): void {
    this.stopPolling();
    // Starting over abandons the previous claim, so leave nothing to restore.
    this.clearStoredActivationClaim();
    this.licensePortalActivationLink = '';
    this.licensePortalActivationLinkQrCodeSVG = '';
    this.claimMode = null;
    this.claimToken = null;
    this.clearSendErrors();
    this.activationState = ActivationState.INITIAL;
  }

  private backToWaiting(): void {
    this.clearKeyErrors();
    this.activationState = this.claimWaitingState();
    this.startPolling();
  }

  async pasteKey() {
    try {
      this.keyInput = await navigator.clipboard.readText();
    } catch (_err) { /* empty */ }
  }

  onKeyChange(): void {
    this.clearKeyErrors();
  }

  activateKey(control: FormControl): void {
    this.clearKeyErrors();
    const secret = (this.keyInput || '').trim();
    if (secret.length < KEY_MIN_LEN) {
      this.keyError = true;
      return;
    }
    this.activatingKey = true;
    this.systemSetupService.applyLicenseKey(secret).subscribe({
      next: (info) => {
        this.stopPolling();
        if (!!info?.status && info.status !== SystemSetupState.LICENSE_REQUIRED) {
          this.handleSetupStatusUpdate(info.status);
        } else {
          this.activatingKey = false;
          this.keyError = true;
        }
      },
      error: (err) => {
        control.markAsTouched();
        this.activatingKey = false;
        void this.onSendError(err);
      }
    });
  }

  setupAccount(): void {
    if (this.submittingAccount) {
      return;
    }
    this.clearSendErrors();
    if (this.setupAccountForm.invalid) {
      this.setupAccountForm.markAllAsTouched();
      return;
    }
    const request = this.setupAccountForm.getRawValue();
    const systemSetupRequest: SystemSetupRequest = {
      email: request.email,
      password: request.password,
      loadDemo: request.loadDemo
    };
    this.submittingAccount = true;
    this.systemSetupService.completeSetup(systemSetupRequest).subscribe({
      next: () => {
        this.systemSetupService.getSetupState().subscribe({
          next: (info) => {
            if (info?.status === SystemSetupState.READY) {
              this.authService.login({ username: request.email, password: request.password })
              .subscribe({
                next: () => {
                  // The sign-in is still loading the user, so complete from the moment that load lands: a second,
                  // competing load started here would sign out the session the first one just established.
                  this.whenUserLoaded().subscribe(() => {
                    if (!request.loadDemo) {
                      this.handleSetupStatusUpdate(info.status);
                      return;
                    }
                    this.sysAdminEmail = request.email;
                    this.submittingAccount = false;
                    this.activationState = ActivationState.DEMO_TENANT_OFFER;
                  });
                },
                error: () => {
                  this.submittingAccount = false;
                  this.sendError = true;
                  this.sendErrorMessage = this.translate.instant('setup.errors.signin-failed');
                }
              });
            } else {
              this.submittingAccount = false;
              this.sendError = true;
              this.sendErrorMessage = this.translate.instant('setup.errors.state-not-updated');
            }
          },
          error: () => {
            this.submittingAccount = false;
            this.sendError = true;
            this.sendErrorMessage = this.translate.instant('setup.errors.state-verify-failed');
          }
        });
      },
      error: (err) => {
        this.submittingAccount = false;
        void this.onSendError(err);
      }
    });
  }

  exploreDemoTenant(): void {
    this.clearSendErrors();
    this.submittingAccount = true;
    this.authService.login({ username: DEMO_TENANT_EMAIL, password: DEMO_TENANT_PASSWORD }).subscribe({
      next: () => {
        this.whenUserLoaded().subscribe(() => {
          this.leaveSetupScreen();
        });
      },
      error: (err) => {
        this.submittingAccount = false;
        void this.onSendError(err);
      }
    });
  }

  /** Both answers leave the tenant exactly as it is: it exists by the time this step renders. */
  skipDemoTenant(): void {
    this.leaveSetupScreen();
  }

  /**
   * Watching stops only once the setup-state read comes back usable; a failed read leaves the poll running, so
   * the next tick retries instead of stranding the screen on the waiting copy.
   */
  private continueActivated(): void {
    if (this.readingActivatedSetupState) {
      return;
    }
    this.readingActivatedSetupState = true;
    this.systemSetupService.getSetupState().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (setupInfo) => {
        this.readingActivatedSetupState = false;
        if (setupInfo?.status) {
          this.stopPolling();
          this.handleSetupStatusUpdate(setupInfo.status);
        } else {
          this.onActivatedStateUnreadable();
        }
      },
      error: () => {
        this.readingActivatedSetupState = false;
        this.onActivatedStateUnreadable();
      }
    });
  }

  private onActivatedStateUnreadable(): void {
    this.sendError = true;
    this.sendNetworkError = false;
    this.sendErrorMessage = this.translate.instant('setup.errors.activated-state-verify-failed');
  }

  private handleSetupStatusUpdate(status: SystemSetupState): void {
    if (!status) {
      return;
    }
    // Any state reported from here means the license step is behind us, so there is no claim left to resume.
    this.clearStoredActivationClaim();
    if (status === SystemSetupState.ACCOUNT_REQUIRED) {
      this.clearPendingSubmissions();
      this.activationState = ActivationState.SETUP_ACCOUNT;
    } else if (status === SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED) {
      this.clearPendingSubmissions();
      this.activationState = ActivationState.NON_PRODUCTION_CONFIRM;
      this.ensureAuthInitialized();
    } else if (status === SystemSetupState.READY) {
      this.leaveSetupScreen();
    } else {
      // Still locked - e.g. LICENSE_REQUIRED from a node that has not converged. Handed back unchanged: nothing
      // here may record an incomplete setup as complete.
      this.systemSetupService.handleSetupState(status);
    }
  }

  /**
   * Establishes the session first, then records the setup complete, then routes. What must not happen is the
   * start-up hand-off reloading the user from scratch: a failure inside that reload signs out a working session,
   * which is how completing an activation used to end on the login form.
   */
  private leaveSetupScreen(): void {
    this.ensureAuthInitialized();
    this.whenUserLoaded().subscribe(() => {
      this.systemSetupService.finishSetup();
      this.authService.gotoDefaultPlace(getCurrentAuthState(this.store).isAuthenticated);
    });
  }

  /** Loaded, not signed in: a load that finds no usable session still counts and answers with nobody authenticated. */
  private whenUserLoaded(): Observable<boolean> {
    return this.store.pipe(
      select(selectIsUserLoaded),
      filter((isUserLoaded) => isUserLoaded),
      take(1),
      takeUntil(this.destroy$)
    );
  }

  /** Otherwise a newly rendered screen shows a permanently disabled, permanently spinning button. */
  private clearPendingSubmissions(): void {
    this.activating = false;
    this.activatingKey = false;
    this.submittingAccount = false;
    this.confirmingNonProduction = false;
  }

  confirmNonProduction(): void {
    if (this.confirmingNonProduction) {
      return;
    }
    this.clearSendErrors();
    this.confirmingNonProduction = true;
    this.systemSetupService.confirmNonProduction().subscribe({
      next: () => {
        this.systemSetupService.getSetupState().subscribe({
          // A read still reporting the confirmation as outstanding looks identical to an empty one from here:
          // nothing on screen would change and the same button would be clicked again. Say which it was.
          next: (info) => {
            this.confirmingNonProduction = false;
            const status = info?.status;
            if (!!status && status !== SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED) {
              this.handleSetupStatusUpdate(status);
            } else {
              this.sendError = true;
              this.sendErrorMessage = this.translate.instant('setup.non-production.state-not-updated');
            }
          },
          error: () => {
            this.confirmingNonProduction = false;
            this.sendError = true;
            this.sendErrorMessage = this.translate.instant('setup.non-production.state-verify-failed');
          }
        });
      },
      error: (err) => {
        this.confirmingNonProduction = false;
        void this.onSendError(err);
      }
    });
  }

  /**
   * Signs the current viewer out and waits for it to land before navigating - going immediately races /login's
   * guard, which would still see the old state. Captures this URL so a successful sign-in returns here.
   */
  goToSysAdminLogin(): void {
    this.authService.logout(true);
    this.store.pipe(
      select(selectIsAuthenticated),
      filter((isAuthenticated) => !isAuthenticated),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  /**
   * Unlike LICENSE_REQUIRED/ACCOUNT_REQUIRED, this state presupposes a sysadmin account and its only remedy is
   * sysadmin-authenticated. The bootstrap installs no auth wiring while setup is incomplete, so without this
   * `isUserLoaded` never becomes true, AuthGuard never resolves, and the lock has no way out.
   */
  private ensureAuthInitialized(): void {
    if (!getCurrentAuthState(this.store).isUserLoaded) {
      this.authService.reloadUser();
    }
  }

  private startPolling(): void {
    this.stopPolling();
    // Poll GET /api/noauth/setup/claim. Contract per backend javadoc:
    //   ACTIVATED       -> license applied; read the setup state and move on. Polling is stopped by that
    //                      read succeeding, not by this branch, so a read that fails is retried on the
    //                      next tick rather than stranding the screen.
    //   PENDING         -> keep polling.
    //   EXPIRED         -> this session's claim is dead: the portal refused it, or another session's
    //                      claim request superseded the claimToken named below. Terminal either way,
    //                      and the link is spent with it.
    //   NOT_REQUESTED   -> no outstanding claim. Polling only ever runs after a claim request, so this
    //                      means the claim was retired — the same dead end as EXPIRED, which after a
    //                      refusal is delivered on exactly one poll (the reject clears the token) and
    //                      would otherwise be answered NOT_REQUESTED from the next poll on.
    //   HTTP 400        -> terminal, fall back to the manual key form.
    //   HTTP 429 / 5xx  -> retryable, keep polling (backoff is server-side).
    this.poll = interval(ACTIVATION_POLL_INTERVAL_MS)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.systemSetupService.getClaimInfo(this.claimToken).pipe(
          catchError((err: HttpErrorResponse) => {
            if (err?.status === 400) {
              this.stopPolling();
              // Watching is refused, so the link plus the key form is all that is left to offer.
              this.activationState = ActivationState.OFFLINE;
            }
            return EMPTY;
          })
        ))
      )
      .subscribe((info) => {
        if (info?.status === LicenseClaimStatus.ACTIVATED) {
          this.continueActivated();
        } else if (info?.status === LicenseClaimStatus.EXPIRED || info?.status === LicenseClaimStatus.NOT_REQUESTED) {
          this.stopPolling();
          // The link is spent, so neither screen that shows it is any use: the only remedy is a fresh claim,
          // which is what the first screen asks for.
          this.gotoInitial();
          this.sendError = true;
          this.sendNetworkError = false;
          this.sendErrorMessage = this.translate.instant('setup.errors.link-expired');
        }
      });
  }

  private stopPolling(): void {
    if (this.poll) {
      this.poll.unsubscribe();
      this.poll = undefined;
    }
  }

  /**
   * The claim lives on the server, but the link built from its sign-up URL lives only here - so a reload would
   * leave the screen with no link to show, and re-requesting one would mint a fresh token and invalidate the
   * link the operator may already have open. Session storage: survives F5, dies with the tab.
   */
  private storeActivationClaim(): void {
    try {
      const claim: StoredActivationClaim = {
        activationLink: this.licensePortalActivationLink, mode: this.claimMode, claimToken: this.claimToken
      };
      window.sessionStorage.setItem(ACTIVATION_CLAIM_STORAGE_KEY, JSON.stringify(claim));
    } catch {
      // Session storage unavailable: the URL just will not survive a reload, which is where this started.
    }
  }

  private restoreActivationClaim(): Observable<boolean> {
    let claim: StoredActivationClaim;
    try {
      const stored = window.sessionStorage.getItem(ACTIVATION_CLAIM_STORAGE_KEY);
      claim = stored ? JSON.parse(stored) : null;
    } catch {
      claim = null;
    }
    if (!claim?.activationLink) {
      return of(false);
    }
    // An unrecognised mode costs only the extra key-entry step, so it is dropped rather than read as OFFLINE.
    this.claimMode = Object.values(LicenseClaimMode).includes(claim.mode) ? claim.mode : null;
    this.licensePortalActivationLink = claim.activationLink;
    // Absent in an entry written before the poll became token-aware: polling without one is what that build
    // already did, so the claim is restored rather than discarded.
    this.claimToken = claim.claimToken;
    return from(this.renderActivationLinkQrCode()).pipe(
      map(() => true)
    );
  }

  private clearStoredActivationClaim(): void {
    try {
      window.sessionStorage.removeItem(ACTIVATION_CLAIM_STORAGE_KEY);
    } catch {
      // Nothing stored, nothing to clear.
    }
  }

  private clearKeyErrors(): void {
    this.keyError = false;
    this.clearSendErrors();
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
