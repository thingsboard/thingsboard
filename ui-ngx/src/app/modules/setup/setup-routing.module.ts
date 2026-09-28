// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ActivatePlatformComponent } from '@modules/setup/pages/setup/activate-platform.component';
import { SetupGuard } from '@core/guards/setup.guard';

const routes: Routes = [
  {
    path: 'activation',
    component: ActivatePlatformComponent,
    data: {
      title: 'setup.activate-platform'
    },
    canActivate: [ SetupGuard ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SetupRoutingModule { }
