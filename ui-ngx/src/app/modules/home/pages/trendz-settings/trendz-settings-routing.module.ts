// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, NgModule } from "@angular/core";
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from "@angular/router";
import { TrendzSettingsComponent } from "@home/pages/trendz-settings/trendz-settings.component";
import { Authority } from "@app/shared/models/authority.enum";
import { MenuId } from "@app/core/services/menu.models";
import {
  TrendzStatus,
  TrendzSynchronizationResultType,
  TrendzSynchronizationStatus
} from "@app/shared/models/trendz-analytics.models";
import { TrendzService } from "@app/core/http/trendz.service";
import { subscriptionInfoResolver } from '@home/pages/admin/admin-routing.module';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { map, of, switchMap } from "rxjs";

export const TrendzSyncResolver: ResolveFn<TrendzStatus> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  store: Store<AppState> = inject(Store<AppState>),
  trendzService = inject(TrendzService)) => {
    const authState = getCurrentAuthState(store);
    if (authState.licenseVersion < 2 || authState.trendzEnabled) {
      return trendzService.getTrendzSyncResult().pipe(
        switchMap(result => {
          const trendzStatus: TrendzStatus = {
            type: result.type,
            syncStatus: result.status,
            healthcheckStatus: result.status,
          }
          if (result.status === TrendzSynchronizationStatus.SYNCED) {
            return trendzService.performTrendzHealthcheck().pipe(
              map(healthcheckResult => {
                trendzStatus.healthcheckStatus = healthcheckResult.status;
                trendzStatus.type = healthcheckResult.type;
                return trendzStatus;
              })
            );
          }
          return of(trendzStatus);
        }),
      )
    } else {
      return of({
        type: TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED,
        syncStatus: TrendzSynchronizationStatus.NOT_AVAILABLE,
        healthcheckStatus: TrendzSynchronizationStatus.NOT_AVAILABLE
      });
    }
}

const routes: Routes = [
  {
    path: 'trendzSettings',
    component: TrendzSettingsComponent,
    data: {
      auth: [Authority.SYS_ADMIN],
      title: 'trendz-analytics.trendz-settings',
      breadcrumb: {
        menuId: MenuId.trendz_settings
      }
    },
    resolve: {
      trendzSyncInfo: TrendzSyncResolver,
      subscriptionInfo: subscriptionInfoResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TrendzSettingsRoutingModule { }
