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

import { EventEmitter, Injectable } from '@angular/core';
import { ActiveComponentService } from '@core/services/active-component.service';
import { BehaviorSubject, Observable, shareReplay, Subject } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { isDefinedAndNotNull } from '@core/utils';
import { PageComponent } from '@shared/components/page.component';

@Injectable({
  providedIn: 'root'
})
export class HomeService {

  private hideMainToolbarSubject: Subject<boolean> = new BehaviorSubject<boolean>(false);
  private hideLoadingBarSubject: Subject<boolean> = new BehaviorSubject<boolean>(false);

  get hideMainToolbar$() {
    return this.hideMainToolbarSubject.asObservable().pipe(distinctUntilChanged(), shareReplay(1));
  }

  get hideLoadingBar$() {
    return this.hideLoadingBarSubject.asObservable().pipe(distinctUntilChanged(), shareReplay(1));
  }

  toggleSideBar = new EventEmitter<void>();

  constructor(private activeComponentService: ActiveComponentService) {
    this.activeComponentService.onActiveComponentChanged().subscribe(activeComponent => {
      Promise.resolve().then(() => {
        this.activeComponentChanged(activeComponent);
      });
    });
  }

  public setHideMainToolbar(hide: boolean): void {
    Promise.resolve().then(() => {
      this.hideMainToolbarSubject.next(hide);
    });
  }

  private activeComponentChanged(activeComponent: any) {
    this.hideMainToolbarSubject.next(false);
    let hideLoadingBar = false;
    if (activeComponent && activeComponent instanceof RouterTabsComponent
      && isDefinedAndNotNull(activeComponent.activatedRoute?.snapshot?.data?.showMainLoadingBar)) {
      hideLoadingBar = !activeComponent.activatedRoute.snapshot.data.showMainLoadingBar;
    } else if (activeComponent && activeComponent instanceof PageComponent
      && isDefinedAndNotNull(activeComponent?.showMainLoadingBar)) {
      hideLoadingBar = !activeComponent.showMainLoadingBar;
    }
    this.hideLoadingBarSubject.next(hideLoadingBar);
  }

}
