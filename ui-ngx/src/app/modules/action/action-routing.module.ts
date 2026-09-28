// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { RouterModule, Routes } from '@angular/router';
import { ActionGuard } from '@modules/action/action.guard';
import { NgModule } from '@angular/core';
import { StoreModule } from '@ngrx/store';
import { of } from 'rxjs';

const routes: Routes = [
  {
    path: 'action/entitiesLimitIncreaseRequest',
    loadComponent: () => of(null),
    data: {},
    canActivate: [ActionGuard],
  },
  {
    path: 'action/addonAccessRequest',
    loadComponent: () => of(null),
    data: {},
    canActivate: [ActionGuard],
  },
  {
    path: 'action/addonAccessError',
    loadComponent: () => of(null),
    data: {},
    canActivate: [ActionGuard],
  }
];

@NgModule({
  imports: [
    StoreModule,
    RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    ActionGuard
  ]
})
export class ActionRoutingModule { }
