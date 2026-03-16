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

import { Inject, Injectable, NgZone } from '@angular/core';
import { WINDOW } from '@core/services/window.service';

export type CancelAnimationFrame = () => void;

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class RafService {

  private readonly rafFunction: (frameCallback: () => void) => CancelAnimationFrame;
  private readonly rafSupported: boolean;

  constructor(
    @Inject(WINDOW) private window: Window,
    private ngZone: NgZone
  ) {
    const requestAnimationFrame: (frameCallback: () => void) => number = window.requestAnimationFrame ||
      (window as any).webkitRequestAnimationFrame;
    const cancelAnimationFrame = window.cancelAnimationFrame ||
      (window as any).webkitCancelAnimationFrame ||
      // @ts-ignore
      window.webkitCancelRequestAnimationFrame;

    this.rafSupported = !!requestAnimationFrame;

    if (this.rafSupported) {
      this.rafFunction = (frameCallback: () => void) => {
        const id = requestAnimationFrame(frameCallback);
        return () => {
          cancelAnimationFrame(id);
        };
      };
    } else {
      this.rafFunction = (frameCallback: () => void) => {
        const timeoutId = setTimeout(frameCallback, 16.66);
        return () => {
          clearTimeout(timeoutId);
        };
      };
    }
  }

  public raf(frameCallback: () => void, runInZone = false): CancelAnimationFrame {
    if (runInZone) {
      return this.rafFunction(frameCallback);
    } else {
      return this.ngZone.runOutsideAngular(() => this.rafFunction(frameCallback));
    }
  }
}
