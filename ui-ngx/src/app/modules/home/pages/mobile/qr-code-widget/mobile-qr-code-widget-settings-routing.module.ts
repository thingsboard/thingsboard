// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import {
  MobileQrCodeWidgetSettingsComponent
} from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { forkJoin, of } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { MobileApplicationService } from '@core/http/mobile-application.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { QrCodeSettings } from '@shared/models/mobile-app.models';
import { LoginWhiteLabelingParams } from '@shared/models/white-labeling.models';

export const qrCodeWidgetSettingsResolver: ResolveFn<{
  mobileAppSettings: QrCodeSettings;
  loginWhiteLabelParams: LoginWhiteLabelingParams;
}> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  store = inject(Store<AppState>),
  mobileAppService = inject(MobileApplicationService),
  wl = inject(WhiteLabelingService)
) => forkJoin({
  mobileAppSettings: mobileAppService.getMobileAppSettings(),
  loginWhiteLabelParams: getCurrentAuthUser(store).authority === Authority.TENANT_ADMIN ?
    wl.getCurrentLoginWhiteLabelParams() : of(null as LoginWhiteLabelingParams)
});

export const qrCodeWidgetRoutes: Routes = [
  {
    path: 'qr-code-widget',
    component: MobileQrCodeWidgetSettingsComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
      title: 'mobile.qr-code-widget',
      breadcrumb: {
        menuId: MenuId.mobile_qr_code_widget
      }
    },
    resolve: {
      qrCodeWidgetSettings: qrCodeWidgetSettingsResolver
    }
  }
];

const routes: Routes = [
  {
    path: 'settings/mobile-app',
    pathMatch: 'full',
    redirectTo: '/mobile-center/qr-code-widget'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MobileQrCodeWidgetSettingsRoutingModule { }
