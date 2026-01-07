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

import { OverlayModule } from '@angular/cdk/overlay';
import { NgModule } from '@angular/core';
import { DEFAULT_DIALOG_CONFIG, DialogConfig, DialogModule } from '@angular/cdk/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { DynamicDialog, DynamicMatDialog } from './dynamic-dialog';
import { DynamicOverlay } from './dynamic-overlay';
import { DynamicOverlayContainer } from './dynamic-overlay-container';

export const DYNAMIC_MAT_DIALOG_PROVIDERS = [
  DynamicOverlayContainer,
  DynamicOverlay,
  DynamicDialog,
  DynamicMatDialog,
  {
    provide: DEFAULT_DIALOG_CONFIG,
    useValue: {
      ...new DialogConfig()
    }
  }
];

@NgModule( {
  imports: [
    OverlayModule,
    DialogModule,
    MatDialogModule
  ],
  providers: DYNAMIC_MAT_DIALOG_PROVIDERS
} )
export class DynamicMatDialogModule {
}
