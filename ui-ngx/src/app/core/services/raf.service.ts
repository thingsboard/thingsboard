// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
