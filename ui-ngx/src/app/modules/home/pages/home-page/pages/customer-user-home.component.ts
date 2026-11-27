// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHomeCards } from '@core/auth/auth.selectors';
import { HomeCardId } from '@shared/models/user-settings.models';
import { isHomeCardShown } from '@home/pages/home-page/home-cards.models';
import { HomePermissionsService } from '@home/pages/home-page/home-permissions.service';

const MAIN_CARDS = [HomeCardId.ALARMS, HomeCardId.DASHBOARDS, HomeCardId.DEVICES];
const SIDE_CARDS = [HomeCardId.NOTIFICATIONS, HomeCardId.QUICK_LINKS];

@Component({
  selector: 'tb-customer-user-home',
  template: `
    <ng-template #homeCard let-id>
      @switch (id) {
        @case (HomeCardId.ALARMS) {
          <tb-alarms-home></tb-alarms-home>
        }
        @case (HomeCardId.DASHBOARDS) {
          <tb-dashboards-home></tb-dashboards-home>
        }
        @case (HomeCardId.DEVICES) {
          <tb-devices-home></tb-devices-home>
        }
        @case (HomeCardId.NOTIFICATIONS) {
          <tb-notifications-home></tb-notifications-home>
        }
        @case (HomeCardId.QUICK_LINKS) {
          <tb-quick-links-home></tb-quick-links-home>
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
      <tb-home-cards-footer [cards]="cards"></tb-home-cards-footer>
    </div>
  `,
  styleUrls: ['./tenant-admin-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerUserHomeComponent {

  readonly HomeCardId = HomeCardId;

  private readonly permissions = inject(HomePermissionsService);

  private readonly mainCardIds = MAIN_CARDS.filter(id => this.permissions.cardAvailable(id));

  private readonly sideCardIds = SIDE_CARDS.filter(id => this.permissions.cardAvailable(id));

  readonly cards = [...this.mainCardIds, ...this.sideCardIds];

  private settings = toSignal(inject(Store<AppState>).select(selectHomeCards), {initialValue: {}});

  private readonly shownMainCards = computed(() => this.mainCardIds.filter(id => isHomeCardShown(this.settings(), id)));

  private readonly shownSideCards = computed(() => this.sideCardIds.filter(id => isHomeCardShown(this.settings(), id)));

  readonly mainCards = computed(() => this.shownMainCards().length ? this.shownMainCards() : this.shownSideCards());

  readonly sideCards = computed(() => this.shownMainCards().length ? this.shownSideCards() : []);
}
