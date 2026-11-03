// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { HomeComponent } from './home.component';
import { AuthGuard } from '@core/guards/auth.guard';
import { StoreModule } from '@ngrx/store';

const routes: Routes = [
  { path: '',
    component: HomeComponent,
    data: {
      title: 'home.home',
      breadcrumb: {
        skip: true
      }
    },
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    loadChildren: () => import('./pages/home-pages.module').then(m => m.HomePagesModule)
  }
];

@NgModule({
  imports: [
    StoreModule,
    RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HomeRoutingModule { }
