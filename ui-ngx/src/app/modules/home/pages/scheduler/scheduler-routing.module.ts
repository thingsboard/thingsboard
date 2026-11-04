// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { SchedulerEventsComponent } from '@home/components/scheduler/scheduler-events.component';
import { MenuId } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestPePackComponent } from '@home/components/pe-pack/request-pe-pack.component';
import { PlatformFeature } from '@shared/models/subscription.models';

const disabledSchedulerReplaceComponentFunction = (store: Store<AppState>) => {
  const authState = getCurrentAuthState(store);
  if (!authState.schedulerEnabled) {
    return RequestPePackComponent;
  } else {
    return null;
  }
}

export const schedulerRoutes: Routes = [
  {
    path: 'scheduler',
    component: SchedulerEventsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'scheduler.scheduler',
      platformFeature: PlatformFeature.SCHEDULER,
      replaceComponent: disabledSchedulerReplaceComponentFunction,
      breadcrumb: {
        menuId: MenuId.scheduler
      }
    }
  }
];

const routes: Routes = [
  {
    path: 'scheduler',
    redirectTo: '/features/scheduler'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SchedulerRoutingModule { }
