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

import { Directive, inject, OnDestroy } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable, Subscription } from 'rxjs';
import { selectIsLoading } from '@core/interceptors/load.selectors';
import { delay, share } from 'rxjs/operators';
import { AbstractControl } from '@angular/forms';

@Directive()
export abstract class PageComponent implements OnDestroy {

  protected store: Store<AppState> = inject(Store<AppState>);

  isLoading$: Observable<boolean>;
  loadingSubscription: Subscription;
  disabledOnLoadFormControls: Array<AbstractControl> = [];

  showMainLoadingBar = true;

  protected constructor(...args: unknown[]) {
    this.isLoading$ = this.store.pipe(delay(0), select(selectIsLoading), share());
  }

  protected registerDisableOnLoadFormControl(control: AbstractControl) {
    this.disabledOnLoadFormControls.push(control);
    if (!this.loadingSubscription) {
      this.loadingSubscription = this.isLoading$.subscribe((isLoading) => {
        for (const formControl of this.disabledOnLoadFormControls) {
          if (isLoading) {
            formControl.disable({emitEvent: false});
          } else {
            formControl.enable({emitEvent: false});
          }
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.loadingSubscription) {
      this.loadingSubscription.unsubscribe();
    }
  }

}
