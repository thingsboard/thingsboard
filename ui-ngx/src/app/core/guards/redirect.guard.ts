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
export class RedirectGuard implements CanActivate {
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
      this.router.navigateByUrl('/settings/oauth2-settings');
      return false;
    }
    this.router.navigateByUrl('/settings/general');
    return false;
  }

}
