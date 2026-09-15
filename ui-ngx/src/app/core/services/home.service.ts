// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
