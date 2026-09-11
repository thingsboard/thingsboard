// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { AttributesNameValueMap } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';

export interface Lwm2mAttributesDialogData {
  readonly: boolean;
  attributes: AttributesNameValueMap;
  modelName: string;
  isResource: boolean;
}

@Component({
    selector: 'tb-lwm2m-attributes-dialog',
    templateUrl: './lwm2m-attributes-dialog.component.html',
    styleUrls: [],
    providers: [{ provide: ErrorStateMatcher, useExisting: Lwm2mAttributesDialogComponent }],
    standalone: false
})
export class Lwm2mAttributesDialogComponent
  extends DialogComponent<Lwm2mAttributesDialogComponent, AttributesNameValueMap> implements ErrorStateMatcher {

  readonly: boolean;
  name: string;
  isResource: boolean;

  private submitted = false;

  attributeFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: Lwm2mAttributesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<Lwm2mAttributesDialogComponent, AttributesNameValueMap>,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.readonly = data.readonly;
    this.name = data.modelName;
    this.isResource = data.isResource;

    this.attributeFormGroup = this.fb.group({
      attributes: [data.attributes]
    });
    if (this.readonly) {
      this.attributeFormGroup.disable({emitEvent: false});
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.attributeFormGroup.get('attributes').value);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
