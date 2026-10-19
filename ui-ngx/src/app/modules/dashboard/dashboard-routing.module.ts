// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from '@core/guards/auth.guard';
import { StoreModule } from '@ngrx/store';

const routes: Routes = [
  { path: '',
    data: {
      title: 'dashboard.dashboard',
      breadcrumb: {
        skip: true
      }
    },
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    loadChildren: () => import('./dashboard-pages.module').then(m => m.DashboardPagesModule)
  }
];

@NgModule({
  imports: [
    StoreModule,
    RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DashboardRoutingModule { }
