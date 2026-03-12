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
