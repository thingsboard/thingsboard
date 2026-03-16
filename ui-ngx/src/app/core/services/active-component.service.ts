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
