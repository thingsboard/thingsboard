import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import {
  MobileQrCodeWidgetSettingsComponent
} from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';

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
