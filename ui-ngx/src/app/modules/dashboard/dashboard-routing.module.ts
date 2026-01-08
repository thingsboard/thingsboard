///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
