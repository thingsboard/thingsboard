// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { MenuId } from '@core/services/menu.models';

export const vcRoutes: Routes = [
  {
    path: 'vc',
    component: VersionControlComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'version-control.version-control',
      breadcrumb: {
        menuId: MenuId.version_control
      }
    }
  }
];

const routes: Routes = [
  {
    path: 'vc',
    redirectTo: '/features/vc'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class VcRoutingModule { }
