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

import { Component, Inject, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { DataKey, DataKeyConfigMode, Widget, widgetType } from '@shared/models/widget.models';
import { DataKeyConfigComponent } from './data-key-config.component';
import { Dashboard } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { TranslateService } from '@ngx-translate/core';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';

export interface DataKeyConfigDialogData {
  dataKey: DataKey;
  dataKeyConfigMode?: DataKeyConfigMode;
  dataKeySettingsForm: FormProperty[];
  dataKeySettingsDirective: string;
  dashboard: Dashboard;
  aliasController: IAliasController;
  widget: Widget;
  widgetType: widgetType;
  deviceId?: string;
  entityAliasId?: string;
  showPostProcessing?: boolean;
  callbacks?: WidgetConfigCallbacks;
  hideDataKeyName?: boolean;
  hideDataKeyLabel?: boolean;
  hideDataKeyColor?: boolean;
  hideDataKeyUnits?: boolean;
  hideDataKeyDecimals?: boolean;
  supportsUnitConversion?: boolean
}

@Component({
    selector: 'tb-data-key-config-dialog',
    templateUrl: './data-key-config-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: DataKeyConfigDialogComponent }],
    styleUrls: ['./data-key-config-dialog.component.scss'],
    standalone: false
})
export class DataKeyConfigDialogComponent extends DialogComponent<DataKeyConfigDialogComponent, DataKey>
  implements OnInit, ErrorStateMatcher {

  @ViewChild('dataKeyConfig', {static: true}) dataKeyConfig: DataKeyConfigComponent;

  hasAdvanced = false;

  dataKeyConfigHeaderOptions: ToggleHeaderOption[];

  dataKeyConfigMode: DataKeyConfigMode = DataKeyConfigMode.general;

  dataKeyFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DataKeyConfigDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DataKeyConfigDialogComponent, DataKey>,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.dataKeyFormGroup = this.fb.group({
      dataKey: [this.data.dataKey, [Validators.required]]
    });
    if (this.data.dataKeySettingsForm?.length ||
      this.data.dataKeySettingsDirective?.length) {
      this.hasAdvanced = true;
      this.dataKeyConfigHeaderOptions = [
        {
          name: this.translate.instant('datakey.general'),
          value: DataKeyConfigMode.general
        },
        {
          name: this.translate.instant('datakey.advanced'),
          value: DataKeyConfigMode.advanced
        }
      ];
      if (this.data.dataKeyConfigMode) {
        this.dataKeyConfigMode = this.data.dataKeyConfigMode;
      }
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.dataKeyConfig.validateOnSubmit().subscribe(() => {
      if (this.dataKeyFormGroup.valid) {
        const dataKey: DataKey = this.dataKeyFormGroup.get('dataKey').value;
        this.dialogRef.close(dataKey);
      }
    });
  }
}
