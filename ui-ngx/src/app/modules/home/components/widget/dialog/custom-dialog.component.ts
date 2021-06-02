///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { Directive, InjectionToken } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { PageComponent } from '@shared/components/page.component';
import { CustomDialogContainerComponent } from './custom-dialog-container.component';
import { FormBuilder, Validators } from '@angular/forms';
import { TbInject } from '@shared/decorators/tb-inject';

export const CUSTOM_DIALOG_DATA = new InjectionToken<any>('ConfigDialogData');

export interface CustomDialogData {
  controller: (instance: CustomDialogComponent) => void;
  [key: string]: any;
}

@Directive()
// tslint:disable-next-line:directive-class-suffix
export class CustomDialogComponent extends PageComponent {

  [key: string]: any;

  constructor(@TbInject(Store) protected store: Store<AppState>,
              @TbInject(Router) protected router: Router,
              @TbInject(MatDialogRef) public dialogRef: MatDialogRef<CustomDialogContainerComponent>,
              @TbInject(FormBuilder) public fb: FormBuilder,
              @TbInject(CUSTOM_DIALOG_DATA) public data: CustomDialogData) {
    super(store);
    // @ts-ignore
    this.validators = Validators;
    this.data.controller(this);
  }
}
