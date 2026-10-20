// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { EmptyPageComponent } from '@modules/empty-page/empty-page.component';

const routes: Routes = [
  {
    path: 'empty-page',
    component: EmptyPageComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class EmptyPageRotingModule { }
