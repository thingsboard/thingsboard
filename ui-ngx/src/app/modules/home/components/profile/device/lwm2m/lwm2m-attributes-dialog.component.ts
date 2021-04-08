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

import {Component, Inject, OnInit, SkipSelf} from "@angular/core";
import {ErrorStateMatcher} from "@angular/material/core";
import {DialogComponent} from "@shared/components/dialog.component";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {Router} from "@angular/router";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {isDefinedAndNotNull} from "@core/utils";
import {JsonObject} from "@angular/compiler-cli/ngcc/src/packages/entry_point";

export interface Lwm2mAttributesDialogData {
  readonly: boolean;
  attributeLwm2m: JsonObject;
  destName: string
}

@Component({
  selector: 'tb-lwm2m-attributes-dialog',
  templateUrl: './lwm2m-attributes-dialog.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [{provide: ErrorStateMatcher, useExisting: Lwm2mAttributesDialogComponent}],
})
export class Lwm2mAttributesDialogComponent extends DialogComponent<Lwm2mAttributesDialogComponent, Object> implements OnInit, ErrorStateMatcher {

  readonly = this.data.readonly;

  attributeLwm2m = this.data.attributeLwm2m;

  submitted = false;

  dirtyValue = false;

  attributeLwm2mDialogFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: Lwm2mAttributesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<Lwm2mAttributesDialogComponent, object>,
              private fb: FormBuilder,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.attributeLwm2mDialogFormGroup = this.fb.group({
      keyFilters: [{}, []]
    });
    this.attributeLwm2mDialogFormGroup.patchValue({keyFilters: this.attributeLwm2m});
    this.attributeLwm2mDialogFormGroup.get('keyFilters').valueChanges.subscribe((attributes) => {
      debugger
      this.attributeLwm2m = attributes;
    });
    if (this.readonly) {
      this.attributeLwm2mDialogFormGroup.disable({emitEvent: false});
    }
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.attributeLwm2m);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  attributeLwm2mToString = (keyFilters: {}): string => {
    return isDefinedAndNotNull(keyFilters) ? JSON.stringify(keyFilters) : "---";
  }
}
