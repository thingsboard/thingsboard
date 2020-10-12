///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { KeyFilter, keyFilterInfosToKeyFilters, keyFiltersToKeyFilterInfos } from '@shared/models/query/query.models';

export interface AlarmRuleKeyFiltersDialogData {
  readonly: boolean;
  keyFilters: Array<KeyFilter>;
}

@Component({
  selector: 'tb-alarm-rule-key-filters-dialog',
  templateUrl: './alarm-rule-key-filters-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AlarmRuleKeyFiltersDialogComponent}],
  styleUrls: []
})
export class AlarmRuleKeyFiltersDialogComponent extends DialogComponent<AlarmRuleKeyFiltersDialogComponent, Array<KeyFilter>>
  implements OnInit, ErrorStateMatcher {

  readonly = this.data.readonly;
  keyFilters = this.data.keyFilters;

  keyFiltersFormGroup: FormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleKeyFiltersDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AlarmRuleKeyFiltersDialogComponent, Array<KeyFilter>>,
              private fb: FormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.keyFiltersFormGroup = this.fb.group({
      keyFilters: [keyFiltersToKeyFilterInfos(this.keyFilters), Validators.required]
    });
    if (this.readonly) {
      this.keyFiltersFormGroup.disable({emitEvent: false});
    }
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.keyFilters = keyFilterInfosToKeyFilters(this.keyFiltersFormGroup.get('keyFilters').value);
    this.dialogRef.close(this.keyFilters);
  }
}
