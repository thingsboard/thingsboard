// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Overlay } from '@angular/cdk/overlay';
import { inject, Injectable } from '@angular/core';
import { DynamicOverlayContainer } from './dynamic-overlay-container';

@Injectable()
export class DynamicOverlay extends Overlay {

  private _dynamicOverlayContainer = inject(DynamicOverlayContainer);

  public setContainerElement(containerElement: HTMLElement): void {
    this._dynamicOverlayContainer.setContainerElement(containerElement);
  }
}
