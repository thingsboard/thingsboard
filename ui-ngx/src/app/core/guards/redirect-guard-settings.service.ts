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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthState } from '@core/auth/auth.models';
import { select, Store } from '@ngrx/store';
import { selectAuth } from '@core/auth/auth.selectors';
import { take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';

@Injectable({
  providedIn: 'root'
})
export class RedirectGuardSettings implements CanActivate {
  constructor(private store: Store<AppState>,
              private router: Router) { }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot) {
    let auth: AuthState = null;
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        auth = authState;
      }
    );

    if (auth?.userDetails?.authority === Authority.TENANT_ADMIN) {
      this.router.navigateByUrl('/settings/oauth2');
      return false;
    }
    this.router.navigateByUrl('/settings/general');
    return false;
  }

}
