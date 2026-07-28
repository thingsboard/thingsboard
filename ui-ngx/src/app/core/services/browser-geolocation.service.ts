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
import { Observable } from 'rxjs';

export enum BrowserGeolocationErrorType {
  insecureContext = 'insecureContext',
  permissionDenied = 'permissionDenied',
  positionUnavailable = 'positionUnavailable',
  timeout = 'timeout'
}

export class BrowserGeolocationError extends Error {
  constructor(public readonly errorType: BrowserGeolocationErrorType) {
    super(errorType);
  }
}

const browserGeolocationErrorMessageKeys: {[key in BrowserGeolocationErrorType]: string} = {
  [BrowserGeolocationErrorType.insecureContext]: 'widget-action.browser-location.error-insecure-context',
  [BrowserGeolocationErrorType.permissionDenied]: 'widget-action.browser-location.error-permission-denied',
  [BrowserGeolocationErrorType.positionUnavailable]: 'widget-action.browser-location.error-position-unavailable',
  [BrowserGeolocationErrorType.timeout]: 'widget-action.browser-location.error-timeout'
};

export function browserGeolocationErrorMessageKey(error: BrowserGeolocationError): string {
  return browserGeolocationErrorMessageKeys[error.errorType];
}

function toBrowserGeolocationError(error: GeolocationPositionError): BrowserGeolocationError {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return new BrowserGeolocationError(BrowserGeolocationErrorType.permissionDenied);
    case error.TIMEOUT:
      return new BrowserGeolocationError(BrowserGeolocationErrorType.timeout);
    default:
      return new BrowserGeolocationError(BrowserGeolocationErrorType.positionUnavailable);
  }
}

@Injectable({
  providedIn: 'root'
})
export class BrowserGeolocationService {

  getCurrentPosition(): Observable<GeolocationPosition> {
    return new Observable<GeolocationPosition>((subscriber) => {
      if (!window.isSecureContext || !navigator.geolocation) {
        subscriber.error(new BrowserGeolocationError(BrowserGeolocationErrorType.insecureContext));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          subscriber.next(position);
          subscriber.complete();
        },
        (error) => subscriber.error(toBrowserGeolocationError(error)),
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
      );
    });
  }
}
