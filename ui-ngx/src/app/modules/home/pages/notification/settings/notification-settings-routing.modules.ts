///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Resolve, RouterModule, Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { Injectable, NgModule } from '@angular/core';
import { NotificationSettingsComponent } from '@home/pages/notification/settings/notification-settings.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { NotificationService } from '@core/http/notification.service';

@Injectable()
export class NotificationUserSettingsResolver implements Resolve<any> {

  constructor(private store: Store<AppState>,
              private notificationService: NotificationService) {
  }

  resolve(): Observable<any> {
    return this.notificationService.getNotificationUserSettings();
  }
}

const routes: Routes = [
  {
    path: 'notificationSettings',
    component: NotificationSettingsComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'account.notification-settings',
      breadcrumb: {
        label: 'account.notification-settings',
        icon: 'settings'
      }
    },
    resolve: {
      userSettings: NotificationUserSettingsResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [NotificationUserSettingsResolver]
})
export class NotificationSettingsRoutingModules { }
