// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHomeCards } from '@core/auth/auth.selectors';
import { TourService } from '@core/services/tour.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { isHomeCardShown } from '@home/pages/home-page/home-cards.models';

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

  private readonly sideCardIds = inject(UserPermissionsService).hasGenericPermission(Resource.ALL, Operation.READ)
    ? SIDE_CARDS : SIDE_CARDS.filter(id => id !== HomeCardId.USAGE);

  readonly cards = [...MAIN_CARDS, ...this.sideCardIds];

  readonly aiAvailable = inject(TourService).aiAvailable();

  private settings = toSignal(inject(Store<AppState>).select(selectHomeCards), {initialValue: {}});

  readonly mainCards = computed(() => MAIN_CARDS.filter(id => isHomeCardShown(this.settings(), id)));

  readonly sideCards = computed(() => this.sideCardIds.filter(id => isHomeCardShown(this.settings(), id)));
}
