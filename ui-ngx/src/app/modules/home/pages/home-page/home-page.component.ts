// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectionStrategy, Component, inject, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { toSignal } from '@angular/core/rxjs-interop';
import { Authority } from '@shared/models/authority.enum';
import { HomeDashboard } from '@shared/models/dashboard.models';
import { TourService } from '@core/services/tour.service';

const DEFAULT_PRIMARY = '#00695c';

@Component({
  selector: 'tb-home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss', './components/home-toast.scss', './components/home-menu.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
  host: {
    '[class.tb-home-dashboard]': '!!homeDashboard',
    '[class.tenant-brand]': '!homeDashboard && tenantBrand'
  }
})
export class HomePageComponent {

  readonly Authority = Authority;

  readonly authenticated = toSignal(inject(Store<AppState>).select(selectIsAuthenticated), {initialValue: true});

  readonly homeDashboard: HomeDashboard = inject(ActivatedRoute).snapshot.data.homeDashboard;

  readonly authority = getCurrentAuthUser(inject(Store<AppState>))?.authority;

  readonly aiSky = !this.homeDashboard && inject(TourService).aiAvailable();

  readonly tenantBrand = getComputedStyle(document.body).getPropertyValue('--tb-primary-500')
    .trim().toLowerCase() !== DEFAULT_PRIMARY;
}
