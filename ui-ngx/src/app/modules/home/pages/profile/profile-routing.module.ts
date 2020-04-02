///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';

import { ProfileComponent } from './profile.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { User } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserService } from '@core/http/user.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Observable } from 'rxjs';

@Injectable()
export class UserProfileResolver implements Resolve<User> {

  constructor(private store: Store<AppState>,
              private userService: UserService) {
  }

  resolve(): Observable<User> {
    const userId = getCurrentAuthUser(this.store).userId;
    return this.userService.getUser(userId);
  }
}

const routes: Routes = [
  {
    path: 'profile',
    component: ProfileComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'profile.profile',
      breadcrumb: {
        label: 'profile.profile',
        icon: 'account_circle'
      }
    },
    resolve: {
      user: UserProfileResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UserProfileResolver
  ]
})
export class ProfileRoutingModule { }
