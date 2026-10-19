// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ActiveComponentService {

  private activeComponent: any;
  private activeComponentChangedSubject: Subject<any> = new Subject<any>();

  public getCurrentActiveComponent(): any {
    return this.activeComponent;
  }

  public setCurrentActiveComponent(component: any): void {
    this.activeComponent = component;
    this.activeComponentChangedSubject.next(component);
  }

  public onActiveComponentChanged(): Observable<any> {
    return this.activeComponentChangedSubject.asObservable();
  }

}
