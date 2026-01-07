///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { UntypedFormGroup } from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthState } from '@core/auth/auth.models';
import { selectAuth } from '@core/auth/auth.selectors';
import { map, take } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { isDefined } from '../utils';

export interface HasConfirmForm {
  confirmForm(): UntypedFormGroup;
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
              private translate: TranslateService) { }

  canDeactivate(component: HasConfirmForm & HasDirtyFlag,
                route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot) {


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
    return true;
  }

  private getMessage(component: HasConfirmForm & HasDirtyFlag): string {
    return component.confirmOnExitMessage
      ? component.confirmOnExitMessage
      : this.translate.instant('confirm-on-exit.html-message');
  }
}
