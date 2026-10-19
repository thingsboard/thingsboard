// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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