// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHomeCards, selectMobileQrEnabled } from '@core/auth/auth.selectors';
import { HomeCardId } from '@shared/models/user-settings.models';
import { isHomeCardShown } from '@home/pages/home-page/home-cards.models';
import { SysAdminHomeService } from '@home/pages/home-page/sys-admin-home.service';

const MAIN_CARDS = [HomeCardId.GET_STARTED_SYS_ADMIN, HomeCardId.SYSTEM_STATUS, HomeCardId.LICENSE, HomeCardId.ACTIVITY];
const SIDE_CARDS = [HomeCardId.NOTIFICATIONS, HomeCardId.INSTALL_ITEMS, HomeCardId.QUICK_LINKS, HomeCardId.MOBILE_APP];

@Component({
  selector: 'tb-sys-admin-home',
  template: `
    <ng-template #homeCard let-id>
      @switch (id) {
        @case (HomeCardId.GET_STARTED_SYS_ADMIN) {
          <tb-get-started-sys-admin-home></tb-get-started-sys-admin-home>
        }
        @case (HomeCardId.SYSTEM_STATUS) {
          <tb-system-status-home></tb-system-status-home>
        }
        @case (HomeCardId.LICENSE) {
          <tb-license-home></tb-license-home>
        }
        @case (HomeCardId.ACTIVITY) {
          <tb-devices-activity-home></tb-devices-activity-home>
        }
        @case (HomeCardId.NOTIFICATIONS) {
          <tb-notifications-home></tb-notifications-home>
        }
        @case (HomeCardId.INSTALL_ITEMS) {
          <tb-install-items-home [withFeatures]="!getStartedShown()"></tb-install-items-home>
        }
        @case (HomeCardId.QUICK_LINKS) {
          <tb-quick-links-home></tb-quick-links-home>
        }
        @case (HomeCardId.MOBILE_APP) {
          <tb-mobile-app-home></tb-mobile-app-home>
        }
      }
    </ng-template>
    <div class="tb-home">
      <div class="tb-home-head">
        <tb-welcome-home></tb-welcome-home>
        <tb-status-line-home></tb-status-line-home>
      </div>
      <div class="tb-home-grid">
        <div class="tb-col tb-col-main">
          <tb-product-release-home></tb-product-release-home>
          @for (id of mainCards(); track id) {
            <ng-container [ngTemplateOutlet]="homeCard" [ngTemplateOutletContext]="{$implicit: id}"></ng-container>
          }
        </div>
        <div class="tb-col tb-col-side">
          @for (id of sideCards(); track id) {
            <ng-container [ngTemplateOutlet]="homeCard" [ngTemplateOutletContext]="{$implicit: id}"></ng-container>
          }
        </div>
      </div>
      <tb-home-cards-footer [cards]="cards()"></tb-home-cards-footer>
    </div>
  `,
  styleUrls: ['./tenant-admin-home.component.scss'],
  providers: [SysAdminHomeService],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SysAdminHomeComponent {

  readonly HomeCardId = HomeCardId;

  private readonly store = inject(Store<AppState>);

  private readonly settings = toSignal(this.store.select(selectHomeCards), {initialValue: {}});

  private readonly mobileQrEnabled = toSignal(this.store.select(selectMobileQrEnabled), {initialValue: false});

  readonly getStartedShown = computed(() => isHomeCardShown(this.settings(), HomeCardId.GET_STARTED_SYS_ADMIN));

  readonly cards = computed(() => [...MAIN_CARDS, ...SIDE_CARDS].filter(id => this.available(id)));

  readonly mainCards = computed(() => MAIN_CARDS.filter(id => this.shown(id)));

  readonly sideCards = computed(() => SIDE_CARDS.filter(id => this.shown(id)));

  constructor() {
    inject(SysAdminHomeService).featuresInfo$.subscribe();
  }

  private available(id: HomeCardId): boolean {
    return id !== HomeCardId.MOBILE_APP || this.mobileQrEnabled();
  }

  private shown(id: HomeCardId): boolean {
    return this.available(id) && isHomeCardShown(this.settings(), id);
  }
}
