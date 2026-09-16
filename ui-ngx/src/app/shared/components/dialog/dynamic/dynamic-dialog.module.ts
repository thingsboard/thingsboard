// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Overlay, OverlayContainer, OverlayModule } from '@angular/cdk/overlay';
import { inject, Injector, NgModule } from '@angular/core';
import { DEFAULT_DIALOG_CONFIG, Dialog, DialogConfig, DialogModule } from '@angular/cdk/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { DynamicDialog, DynamicMatDialog } from './dynamic-dialog';
import { DynamicOverlay } from './dynamic-overlay';
import { DynamicOverlayContainer, PARENT_OVERLAY_CONTAINER } from './dynamic-overlay-container';

export const DYNAMIC_MAT_DIALOG_PROVIDERS = [
  {
    provide: DynamicMatDialog,
    useFactory: () => {
      const parentInjector = inject(Injector);
      const parentOverlayContainer = parentInjector.get(OverlayContainer);

      const customInjector = Injector.create({
        providers: [
          { provide: PARENT_OVERLAY_CONTAINER, useValue: parentOverlayContainer },
          DynamicOverlayContainer,
          { provide: OverlayContainer, useExisting: DynamicOverlayContainer },
          DynamicOverlay,
          { provide: Overlay, useExisting: DynamicOverlay },
          DynamicDialog,
          { provide: Dialog, useExisting: DynamicDialog },
          DynamicMatDialog,
          { provide: DEFAULT_DIALOG_CONFIG, useValue: new DialogConfig() }
        ],
        parent: parentInjector
      });

      return customInjector.get(DynamicMatDialog);
    }
  }
];

@NgModule({
  imports: [
    OverlayModule,
    DialogModule,
    MatDialogModule
  ],
  providers: DYNAMIC_MAT_DIALOG_PROVIDERS
})
export class DynamicMatDialogModule {
}
