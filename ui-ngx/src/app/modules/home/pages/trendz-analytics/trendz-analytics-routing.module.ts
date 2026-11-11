// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { TrendzAnalyticsComponent } from '@home/pages/trendz-analytics/trendz-analytics.component';
import { MenuId } from '@core/services/menu.models';
import { map, of } from 'rxjs';
import { TrendzSynchronizationStatus } from '@app/shared/models/trendz-analytics.models';
import { TrendzService } from '@core/http/trendz.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';

export const TrendzSyncInfoResolver: ResolveFn<boolean> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  store: Store<AppState> = inject(Store<AppState>),
  trendzService = inject(TrendzService)) => {
    const authState = getCurrentAuthState(store);
    if (authState.licenseVersion < 2 || authState.trendzEnabled) {
      return trendzService.performTrendzHealthcheck()
      .pipe(map(result => result.status === TrendzSynchronizationStatus.SYNCED));
    } else {
      return of(false);
    }
  }

const routes: Routes = [
  {
    path: 'analytics',
    component: TrendzAnalyticsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'trendz-analytics.trendz-analytics',
      breadcrumb: {
        menuId: MenuId.trendz_analytics
      }
    },
    resolve: {
      trendzSynced: TrendzSyncInfoResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TrendzAnalyticsRoutingModule { }
