///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Location } from "@angular/common";
import { Inject, Injectable, Injector, Optional, SkipSelf, TemplateRef } from '@angular/core';
import {
  MAT_DIALOG_DEFAULT_OPTIONS,
  MAT_DIALOG_SCROLL_STRATEGY,
  MatDialog,
  MatDialogConfig, MatDialogRef
} from '@angular/material/dialog';
import { DynamicOverlay } from "./dynamic-overlay";
import { DynamicOverlayContainer } from '@shared/components/dialog/dynamic/dynamic-overlay-container';
import { ComponentType, ScrollStrategy } from '@angular/cdk/overlay';
import { DEFAULT_DIALOG_CONFIG, Dialog, DialogConfig } from '@angular/cdk/dialog';

export interface DynamicMatDialogConfig<D> extends MatDialogConfig<D> {
  containerElement?: HTMLElement;
}

@Injectable()
export class DynamicMatDialog extends MatDialog {

  private _customOverlay: DynamicOverlay;

  constructor( _overlay: DynamicOverlay,
               _injector: Injector,
               @Optional() location: Location,
               @Inject( MAT_DIALOG_DEFAULT_OPTIONS ) _defaultOptions: MatDialogConfig,
               @Inject( MAT_DIALOG_SCROLL_STRATEGY ) _scrollStrategy: ScrollStrategy,
               @Optional() @SkipSelf() _parentDialog:DynamicMatDialog,
               _overlayContainer: DynamicOverlayContainer) {

    super( _overlay, _injector, location, _defaultOptions, _scrollStrategy, _parentDialog, _overlayContainer );
    this._dialog = _injector.get(DynamicDialog);
    this._customOverlay = _overlay;
  }

  public open<T, D = any, R = any>(component: ComponentType<T> | TemplateRef<T>, config?: DynamicMatDialogConfig<D>): MatDialogRef<T, R> {
    if (config?.containerElement) {
      config.containerElement.style.transform = 'translateZ(0)';
      this._customOverlay.setContainerElement( config.containerElement );
    }
    const ref = super.open(component, config);
    if (config?.containerElement) {
      ref.afterClosed().subscribe(
        {
          next: () => {
            this._customOverlay.setContainerElement(null);
          },
          error: () => {
            this._customOverlay.setContainerElement(null);
          }
        }
      );
    }
    return ref;
  }
}

@Injectable()
export class DynamicDialog extends Dialog {
  constructor( _overlay: DynamicOverlay,
               _injector: Injector,
               @Inject( DEFAULT_DIALOG_CONFIG ) _defaultOptions: DialogConfig,
               @Inject( MAT_DIALOG_SCROLL_STRATEGY ) _scrollStrategy: ScrollStrategy,
               @Optional() @SkipSelf() _parentDialog: DynamicDialog,
               _overlayContainer: DynamicOverlayContainer) {

    super( _overlay, _injector, _defaultOptions, _parentDialog, _overlayContainer, _scrollStrategy  );
  }
}
