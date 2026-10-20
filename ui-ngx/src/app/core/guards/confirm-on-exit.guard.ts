// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { UntypedFormGroup } from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthState } from '@core/auth/auth.models';
import { selectAuth } from '@core/auth/auth.selectors';
import { map, mergeMap, take } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { isDefined } from '../utils';
import { Observable, of } from 'rxjs';

export interface HasConfirmForm {
  confirmForm(): UntypedFormGroup;
  onExit?(): Observable<any>;
  confirmOnExitMessage?: string;
}

export interface HasDirtyFlag {
  isDirty: boolean;
  confirmOnExitMessage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmOnExitGuard  {

  constructor(private store: Store<AppState>,
              private dialogService: DialogService,
              private router: Router,
              private translate: TranslateService) { }

  canDeactivate(component: HasConfirmForm & HasDirtyFlag,
                route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot) {


    if (this.router.getCurrentNavigation()?.extras?.state?.skipConfirmOnExit) {
      return true;
    }

    let auth: AuthState = null;
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        auth = authState;
      }
    );

    if (auth && auth.isAuthenticated) {
      let isDirty = false;
      if (component.confirmForm) {
        const confirmForm = component.confirmForm();
        if (confirmForm) {
          isDirty = confirmForm.dirty;
        }
      } else if (isDefined(component.isDirty)) {
        isDirty = component.isDirty;
      }
      if (isDirty) {
        const message = this.getMessage(component);
        return this.dialogService.confirm(
          this.translate.instant('confirm-on-exit.title'),
          message
        ).pipe(
          mergeMap(result => {
            if (result && component.onExit) {
              return component.onExit().pipe(map(() => result));
            } else {
              return of(result);
            }
          }),
          map((dialogResult) => {
            if (dialogResult) {
              if (component.confirmForm && component.confirmForm()) {
                component.confirmForm().markAsPristine();
              } else {
                component.isDirty = false;
              }
            }
            return dialogResult;
          })
        );
      }
    }
    if (component.onExit) {
      return component.onExit().pipe(map(() => true));
    }
    return true;
  }

  private getMessage(component: HasConfirmForm & HasDirtyFlag): string {
    return component.confirmOnExitMessage
      ? component.confirmOnExitMessage
      : this.translate.instant('confirm-on-exit.html-message');
  }
}
