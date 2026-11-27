// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHomeCards, selectMobileQrEnabled } from '@core/auth/auth.selectors';
import { TourService } from '@core/services/tour.service';
import { HomeCardId } from '@shared/models/user-settings.models';
import { isHomeCardShown } from '@home/pages/home-page/home-cards.models';
import { HomePermissionsService } from '@home/pages/home-page/home-permissions.service';

const MAIN_CARDS = [HomeCardId.IOT_HUB, HomeCardId.ALARMS, HomeCardId.DASHBOARDS, HomeCardId.DEVICES, HomeCardId.ACTIVITY];
const SIDE_CARDS = [HomeCardId.GET_STARTED, HomeCardId.NOTIFICATIONS, HomeCardId.QUICK_LINKS, HomeCardId.USAGE, HomeCardId.MOBILE_APP];

@Component({
  selector: 'tb-tenant-admin-home',
  templateUrl: './tenant-admin-home.component.html',
  styleUrls: ['./tenant-admin-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TenantAdminHomeComponent {

  readonly HomeCardId = HomeCardId;

  private readonly store = inject(Store<AppState>);

  private readonly permissions = inject(HomePermissionsService);

  private readonly mobileQrEnabled = toSignal(this.store.select(selectMobileQrEnabled), {initialValue: false});

  private readonly mainCardIds = MAIN_CARDS.filter(id => this.permissions.cardAvailable(id));

  private readonly sideCardIds = computed(() => SIDE_CARDS.filter(id =>
    this.permissions.cardAvailable(id) && (id !== HomeCardId.MOBILE_APP || this.mobileQrEnabled())));

  readonly cards = computed(() => [...this.mainCardIds, ...this.sideCardIds()]);

  readonly aiAvailable = inject(TourService).aiAvailable();

  private settings = toSignal(this.store.select(selectHomeCards), {initialValue: {}});

  private readonly shownMainCards = computed(() => this.mainCardIds.filter(id => isHomeCardShown(this.settings(), id)));

  private readonly shownSideCards = computed(() => this.sideCardIds().filter(id => isHomeCardShown(this.settings(), id)));

  readonly mainCards = computed(() => this.shownMainCards().length ? this.shownMainCards() : this.shownSideCards());

  readonly sideCards = computed(() => this.shownMainCards().length ? this.shownSideCards() : []);
}
