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

import {
  Overlay,
  ScrollStrategyOptions,
  OverlayKeyboardDispatcher, OverlayOutsideClickDispatcher, OverlayPositionBuilder
} from '@angular/cdk/overlay';
import { ComponentFactoryResolver, Inject, Injectable, Injector, NgZone } from '@angular/core';
import { DynamicOverlayContainer } from './dynamic-overlay-container';
import { DOCUMENT, Location } from '@angular/common';
import { Directionality } from '@angular/cdk/bidi';

@Injectable()
export class DynamicOverlay extends Overlay {

  private _dynamicOverlayContainer: DynamicOverlayContainer;

  constructor( scrollStrategies: ScrollStrategyOptions,
               _overlayContainer: DynamicOverlayContainer,
               _componentFactoryResolver: ComponentFactoryResolver,
               _positionBuilder: OverlayPositionBuilder,
               _keyboardDispatcher: OverlayKeyboardDispatcher,
               _injector: Injector,
               _ngZone: NgZone,
               @Inject(DOCUMENT) document: Document,
               _directionality: Directionality,
               _location: Location,
               _outsideClickDispatcher: OverlayOutsideClickDispatcher) {

    super( scrollStrategies,
      _overlayContainer,
      _componentFactoryResolver,
      _positionBuilder,
      _keyboardDispatcher,
      _injector,
      _ngZone,
      document,
      _directionality,
      _location,
      _outsideClickDispatcher);

    this._dynamicOverlayContainer = _overlayContainer;
  }

  public setContainerElement(containerElement:HTMLElement ): void {
    this._dynamicOverlayContainer.setContainerElement( containerElement );
  }
}
