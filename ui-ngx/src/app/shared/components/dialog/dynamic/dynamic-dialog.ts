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

import { inject, Injectable, TemplateRef } from '@angular/core';
import { MatDialog, MatDialogConfig, MatDialogRef } from '@angular/material/dialog';
import { DynamicOverlay } from './dynamic-overlay';
import { ComponentType } from '@angular/cdk/overlay';
import { Dialog } from '@angular/cdk/dialog';

export interface DynamicMatDialogConfig<D> extends MatDialogConfig<D> {
  containerElement?: HTMLElement;
}

@Injectable()
export class DynamicMatDialog extends MatDialog {

  private _customOverlay = inject(DynamicOverlay);

  public override open<T, D = any, R = any>(component: ComponentType<T> | TemplateRef<T>, config?: DynamicMatDialogConfig<D>): MatDialogRef<T, R> {
    if (config?.containerElement) {
      config.containerElement.style.transform = 'translateZ(0)';
      this._customOverlay.setContainerElement(config.containerElement);
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
}
