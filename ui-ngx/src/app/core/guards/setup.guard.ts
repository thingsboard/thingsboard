// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, NgZone } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { SystemSetupService } from '@core/http/system-setup.service';
import { Observable, of } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { AuthState } from '@core/auth/auth.models';
import { select, Store } from '@ngrx/store';
import { selectAuth } from '@core/auth/auth.selectors';
import { mergeMap, skipWhile, take } from 'rxjs/operators';
import { enterZone } from '@core/operator/enterZone';
import { AppState } from '@core/core.state';
import { SystemSetupState } from '@shared/models/system-setup.models';

@Injectable({
  providedIn: 'root'
})
export class SetupGuard  {

  constructor(private store: Store<AppState>,
              private systemSetupService: SystemSetupService,
              private authService: AuthService,
              private zone: NgZone) {}

  getAuthState(): Observable<AuthState> {
    return this.store.pipe(
      select(selectAuth),
      skipWhile((authState) => !authState || !authState.isUserLoaded),
      take(1),
      enterZone(this.zone)
    );
  }

  canActivate(next: ActivatedRouteSnapshot,
              state: RouterStateSnapshot) {
    if (!!this.systemSetupService.getState() && this.systemSetupService.getState() !== SystemSetupState.READY) {
      return of(true);
    } else {
      return this.getAuthState().pipe(
        mergeMap((authState) => {
          return of(this.authService.defaultUrl(authState.isAuthenticated, authState));
        }
      ));
    }
  }
}
