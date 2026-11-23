// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { TourService } from '@core/services/tour.service';

const DEFAULT_PRIMARY = '#00695c';

@Component({
  selector: 'tb-home-page',
  template: `
    @if (aiSky) {
      <div class="tb-home-sky" aria-hidden="true"></div>
    }
    @switch (authority) {
      @case (Authority.SYS_ADMIN) {
        <tb-sys-admin-home></tb-sys-admin-home>
      }
      @case (Authority.TENANT_ADMIN) {
        <tb-tenant-admin-home></tb-tenant-admin-home>
      }
      @case (Authority.CUSTOMER_USER) {
        <tb-customer-user-home></tb-customer-user-home>
      }
    }
  `,
  styleUrls: ['./home-page.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.tenant-brand]': 'tenantBrand'
  }
})
export class HomePageComponent {

  readonly Authority = Authority;

  readonly authority = getCurrentAuthUser(inject(Store<AppState>))?.authority;

  readonly aiSky = inject(TourService).aiAvailable();

  readonly tenantBrand = getComputedStyle(document.body).getPropertyValue('--tb-primary-500')
    .trim().toLowerCase() !== DEFAULT_PRIMARY;
}
