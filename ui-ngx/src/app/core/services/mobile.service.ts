///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Inject, Injectable } from '@angular/core';
import { WINDOW } from '@core/services/window.service';
import { isDefined } from '@core/utils';
import { MobileActionResult, WidgetMobileActionResult, WidgetMobileActionType } from '@shared/models/widget.models';
import { from, of } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';
import { catchError } from 'rxjs/operators';

const dashboardStateNameHandler = 'tbMobileDashboardStateNameHandler';
const mobileHandler = 'tbMobileHandler';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class MobileService {

  private readonly mobileApp;
  private readonly mobileChannel;

  constructor(@Inject(WINDOW) private window: Window) {
    const w = (this.window as any);
    this.mobileChannel = w.flutter_inappwebview;
    this.mobileApp = isDefined(this.mobileChannel);
  }

  public isMobileApp(): boolean {
    return this.mobileApp;
  }

  public handleDashboardStateName(name: string) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(dashboardStateNameHandler, name);
    }
  }

  public handleWidgetMobileAction<T extends MobileActionResult>(type: WidgetMobileActionType, ...args: any[]):
    Observable<WidgetMobileActionResult<T>> {
    if (this.mobileApp) {
      return from(
        this.mobileChannel.callHandler(mobileHandler, type, ...args) as Promise<WidgetMobileActionResult<T>>).pipe(
        catchError((err: Error) => {
          return of({
            hasError: true,
            error: err?.message ? err.message : `Failed to execute mobile action ${type}`
          } as WidgetMobileActionResult<any>);
        })
      );
    } else {
      return of(null);
    }
  }

}
