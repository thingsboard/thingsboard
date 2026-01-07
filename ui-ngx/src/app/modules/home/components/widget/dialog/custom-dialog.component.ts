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

import { MatDialogRef } from '@angular/material/dialog';
import { Directive, inject, InjectionToken } from '@angular/core';
import { Router } from '@angular/router';
import { PageComponent } from '@shared/components/page.component';
import { CustomDialogContainerComponent } from './custom-dialog-container.component';
import { UntypedFormBuilder, Validators } from '@angular/forms';

export const CUSTOM_DIALOG_DATA = new InjectionToken<CustomDialogData>('ConfigDialogData');

export interface CustomDialogData {
  controller: (instance: CustomDialogComponent) => void;
  [key: string]: any;
}

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class CustomDialogComponent extends PageComponent {

  [key: string]: any;

  protected router = inject(Router);
  public dialogRef = inject(MatDialogRef<CustomDialogContainerComponent>);
  public data = inject(CUSTOM_DIALOG_DATA);
  public fb = inject(UntypedFormBuilder);

  constructor() {
    super();
    // @ts-ignore
    this.validators = Validators;
    this.data.controller(this);
  }
}
