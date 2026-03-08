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

import { OverlayContainer } from "@angular/cdk/overlay";
import { inject, Injectable, InjectionToken } from "@angular/core";

export const PARENT_OVERLAY_CONTAINER = new InjectionToken<OverlayContainer>('PARENT_OVERLAY_CONTAINER');

@Injectable()
export class DynamicOverlayContainer extends OverlayContainer {

  private _globalContainer = inject(PARENT_OVERLAY_CONTAINER);
  private _customElement: HTMLElement | null = null;

  public override getContainerElement(): HTMLElement {
    return this._customElement || this._globalContainer.getContainerElement();
  }

  setContainerElement(element: HTMLElement | null): void {
    this._customElement = element;
  }
}