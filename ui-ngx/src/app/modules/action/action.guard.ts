// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, NgZone } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthState } from '@core/auth/auth.models';
import { select, Store } from '@ngrx/store';
import { selectAuth } from '@core/auth/auth.selectors';
import { mergeMap, skipWhile, take } from 'rxjs/operators';
import { enterZone } from '@core/operator/enterZone';
import { AppState } from '@core/core.state';
import { ActivatedRoute, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { AuthService } from '@core/auth/auth.service';
import { AddonType, addonTypeTranslationMap } from '@shared/models/subscription.models';

@Injectable()
export class ActionGuard {

  constructor(private store: Store<AppState>,
              private zone: NgZone,
              private authService: AuthService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private route: ActivatedRoute,
              private notificationService: NotificationService) {

  }

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
    return this.getAuthState().pipe(
      mergeMap((authState) => {
        const url: string = state.url;
        if (authState.isAuthenticated) {
          const lastChild = this.getLastChild(state.root);
          const path = this.extractPath(state.root);
          const prevPath = this.extractPath(this.route.snapshot);
          const prevParams = this.getLastChild(this.route.snapshot).params || {};
          let actionObservable: Observable<any> = of(null);
          if (path === 'action.entitiesLimitIncreaseRequest') {
            actionObservable = this.performEntitiesLimitIncreaseRequest(authState, lastChild);
          } else if (path === 'action.addonAccessRequest') {
            actionObservable = this.performAddonAccessRequest(authState, lastChild);
          } else if (path === 'action.addonAccessError') {
            actionObservable = this.performAddonAccessError(authState, lastChild);
          }
          return actionObservable.pipe(
            mergeMap(() => {
              const defaultUrl = this.authService.defaultUrl(true, authState, prevPath, prevParams);
              if (defaultUrl) {
                return of(defaultUrl);
              } else {
                return of(false);
              }
            })
          );
        } else {
          this.authService.redirectUrl = url;
          return of(this.authService.defaultUrl(false));
        }
      })
    );
  }

  performEntitiesLimitIncreaseRequest(authState: AuthState, route: ActivatedRouteSnapshot): Observable<any> {
    if (authState.authUser.authority === Authority.TENANT_ADMIN) {
      const entityType = route.queryParams.entityType;
      const subscriptionViolation = !!route.queryParams.subscriptionViolation;
      if (entityType) {
        return this.notificationService.sendEntitiesLimitIncreaseRequest(entityType, subscriptionViolation).pipe(
          mergeMap(() => this.dialogs.alert(
            this.translate.instant('entity.increase-limit-request-sent-title'),
            this.translate.instant('entity.increase-limit-request-sent-text'),
            this.translate.instant('action.close')
          ))
        );
      }
    }
    return of(null);
  }

  performAddonAccessRequest(authState: AuthState, route: ActivatedRouteSnapshot): Observable<any> {
    if (authState.authUser.authority === Authority.TENANT_ADMIN) {
      const addonType = route.queryParams.addonType;
      if (addonType) {
        return this.notificationService.sendAddonAccessRequest(addonType).pipe(
          mergeMap(() => this.dialogs.alert(
            this.translate.instant('subscription.feature-request-sent-title', { addonName: this.translate.instant('subscription.name-addon', { name: this.translate.instant(addonTypeTranslationMap.get(addonType)) } )}),
            this.translate.instant('subscription.feature-request-sent-text'),
            this.translate.instant('action.close')
          ))
        );
      }
    }
    return of(null);
  }

  performAddonAccessError(authState: AuthState, route: ActivatedRouteSnapshot): Observable<any> {
    if (authState.authUser.authority === Authority.TENANT_ADMIN) {
      const addonType = route.queryParams.addonType;
      if (addonType) {
        return this.notificationService.sendAddonAccessError(addonType).pipe(
          mergeMap(() => this.dialogs.alert(
            this.translate.instant('trendz-analytics.service-unavailable-request-sent-title'),
            this.translate.instant('trendz-analytics.service-unavailable-request-sent-message'),

            this.translate.instant('action.close')
          ))
        );
      }
    }
    return of(null);
  }

  private extractPath(snapshot: ActivatedRouteSnapshot): string {
    snapshot = snapshot.root;
    const urlSegments: string[] = [];
    if (snapshot.url) {
      urlSegments.push(...snapshot.url.map(segment => segment.path));
    }
    while (snapshot.children.length) {
      snapshot = snapshot.children[0];
      if (snapshot.url) {
        urlSegments.push(...snapshot.url.map(segment => segment.path));
      }
    }
    return urlSegments.join('.');
  }

  private getLastChild(snapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    let lastChild = snapshot.root;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    return lastChild;
  }
}
