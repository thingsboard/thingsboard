// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectionStrategy, Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { UpdateMessage } from '@shared/models/settings.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { of, Subscription } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import {
  CommunityGrantDialogComponent
} from '@home/components/widget/lib/home-page/community-grant-dialog.component';
import { CommunityGrantService } from '@core/http/community-grant.service';
import {
  CommunityGrantCardState,
  CommunityGrantParkReason,
  CommunityGrantState,
  communityGrantCardConfigs,
  communityGrantPortalUrl,
  communityGrantStateConfigs,
  CommunityGrantCardConfig
} from '@shared/models/ce-grant/community-grant.models';

@Component({
    selector: 'tb-version-info',
    templateUrl: './version-info.component.html',
    styleUrls: ['./home-page-widget.scss', './ce-grant-tokens.scss', './version-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class VersionInfoComponent extends PageComponent implements OnInit, OnDestroy {

  authUser = getCurrentAuthUser(this.store);

  readonly updateMessage = signal<UpdateMessage>(null);

  readonly registrationLink = signal(communityGrantPortalUrl);

  readonly communityGrantCardState = CommunityGrantCardState;

  /** Null is a real answer to the update check, so it cannot double as "not yet". */
  private readonly updateChecked = signal(false);

  private readonly grantState = signal<CommunityGrantState>(null);

  private readonly grantStateKnown = signal(false);

  readonly grantCard = computed(() => communityGrantCardConfigs.get(this.grantState()) ?? null);

  readonly grantIcon = computed(() => {
    const state = communityGrantStateConfigs.get(this.grantState());
    return state?.cardIcon || state?.icon;
  });

  readonly grantCardHasLinks = computed(() =>
    this.hasLinks(communityGrantCardConfigs.get(this.grantState()) ?? null));

  private readonly grantParkReason = signal<CommunityGrantParkReason>(null);

  readonly showExpiredNote = computed(() => this.isPark(null));

  readonly showCheckFailedNote = computed(() => this.isPark(CommunityGrantParkReason.CHECK_FAILED));

  readonly showPortalUnreachableNote =
    computed(() => this.isPark(CommunityGrantParkReason.PORTAL_UNREACHABLE));

  /** A registration outranks the release offer; anything about the release waits for both reads. */
  readonly cardState = computed<CommunityGrantCardState>(() => {
    const card = this.grantCard();
    if (card) {
      return card.body;
    }
    if (!this.grantStateKnown() || !this.updateChecked()) {
      return CommunityGrantCardState.pending;
    }
    return this.authUser.authority === Authority.SYS_ADMIN
      ? CommunityGrantCardState.offer
      : CommunityGrantCardState.upToDate;
  });

  readonly offerVersion = computed(() =>
    this.updateMessage()?.updateAvailable ? this.updateMessage().latestVersion : '4.4');

  private communityGrantStateSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private communityGrantService: CommunityGrantService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit() {
    (this.authUser.authority === Authority.SYS_ADMIN ?
      this.adminService.checkUpdates() : of(null)).subscribe({
      next: (updateMessage) => {
        this.updateMessage.set(updateMessage);
        this.updateChecked.set(true);
      },
      error: () => this.updateChecked.set(true)
    });
    this.loadCommunityGrantState();
  }

  ngOnDestroy(): void {
    this.communityGrantStateSubscription?.unsubscribe();
    super.ngOnDestroy();
  }

  private loadCommunityGrantState(): void {
    if (this.authUser.authority !== Authority.SYS_ADMIN) {
      this.grantStateKnown.set(true);
      return;
    }
    this.communityGrantStateSubscription?.unsubscribe();
    this.communityGrantStateSubscription = this.communityGrantService.pollRegistration({ignoreErrors: true})
      .subscribe({
        next: (registration) => {
          this.registrationLink.set(registration.registrationLink ?? communityGrantPortalUrl);
          this.applyGrantState(registration.state, registration.parkReason);
        },
        error: () => this.applyGrantState(null, null)
      });
  }

  private applyGrantState(state: CommunityGrantState, parkReason: CommunityGrantParkReason): void {
    this.grantState.set(state);
    this.grantParkReason.set(parkReason);
    this.grantStateKnown.set(true);
  }

  /** `null` matches any park reason other than the two retryable ones. */
  private isPark(reason: CommunityGrantParkReason): boolean {
    if (this.grantState() !== CommunityGrantState.expired) {
      return false;
    }
    const parked = this.grantParkReason();
    return reason === null
      ? parked !== CommunityGrantParkReason.CHECK_FAILED
        && parked !== CommunityGrantParkReason.PORTAL_UNREACHABLE
      : parked === reason;
  }

  private hasLinks(config: CommunityGrantCardConfig): boolean {
    return !!config?.registrationLink || !!config?.linkAction;
  }

  getCommunityGrant(): void {
    this.dialog.open<CommunityGrantDialogComponent>(CommunityGrantDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      maxWidth: '100vw',
      maxHeight: '92vh',
      autoFocus: false
    }).afterClosed().subscribe(() => this.loadCommunityGrantState());
  }
}
