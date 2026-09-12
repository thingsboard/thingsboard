// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    try {
      return super.open(component, config);
    } finally {
      if (config?.containerElement) {
        this._customOverlay.setContainerElement(null);
      }
    }
  }
}

@Injectable()
export class DynamicDialog extends Dialog {
}
