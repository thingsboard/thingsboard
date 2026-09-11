// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
