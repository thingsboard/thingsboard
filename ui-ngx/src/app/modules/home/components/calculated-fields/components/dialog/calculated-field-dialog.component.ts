///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroup, UntypedFormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { helpBaseUrl } from '@shared/models/constants';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  CalculatedFieldDialogData,
  CalculatedFieldType,
  CalculatedFieldTypeTranslations,
  OutputType,
  OutputTypeTranslations
} from '@shared/models/calculated-field.models';
import { noLeadTrailSpacesRegex } from '@shared/models/regex.constants';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EntityType } from '@shared/models/entity-type.models';
import { map, startWith } from 'rxjs/operators';
import { isObject } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-calculated-field-dialog',
  templateUrl: './calculated-field-dialog.component.html',
})
export class CalculatedFieldDialogComponent extends DialogComponent<CalculatedFieldDialogComponent, CalculatedField> {

  fieldFormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex), Validators.maxLength(255)]],
    type: [CalculatedFieldType.SIMPLE],
    debugSettings: [],
    configuration: this.fb.group({
      arguments: [{}],
      expressionSIMPLE: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex), Validators.maxLength(255)]],
      expressionSCRIPT: [],
      output: this.fb.group({
        name: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex), Validators.maxLength(255)]],
        scope: [{ value: AttributeScope.SERVER_SCOPE, disabled: true }],
        type: [OutputType.Timeseries]
      }),
    }),
  });

  functionArgs$ = this.configFormGroup.valueChanges
    .pipe(
      startWith(this.data.value?.configuration ?? {}),
      map(configuration => isObject(configuration?.arguments) ? Object.keys(configuration.arguments) : [])
    );

  readonly OutputTypeTranslations = OutputTypeTranslations;
  readonly OutputType = OutputType;
  readonly AttributeScope = AttributeScope;
  readonly EntityType = EntityType;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly ScriptLanguage = ScriptLanguage;
  readonly helpLink = `${helpBaseUrl}/[TODO: [Calculated Fields] add valid link]`;
  readonly fieldTypes = Object.values(CalculatedFieldType) as CalculatedFieldType[];
  readonly outputTypes = Object.values(OutputType) as OutputType[];
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDialogData,
              public dialogRef: MatDialogRef<CalculatedFieldDialogComponent, CalculatedField>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.applyDialogData();
    this.observeTypeChanges();
  }

  get configFormGroup(): FormGroup {
    return this.fieldFormGroup.get('configuration') as FormGroup;
  }

  get outputFormGroup(): FormGroup {
    return this.fieldFormGroup.get('configuration').get('output') as FormGroup;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      const { configuration, type, ...rest } = this.fieldFormGroup.value;
      const { expressionSIMPLE, expressionSCRIPT, ...restConfig } = configuration;
      this.dialogRef.close({ configuration: { ...restConfig, type, expression: configuration['expression'+type] }, ...rest, type });
    }
  }

  private applyDialogData(): void {
    const { configuration = {}, type = CalculatedFieldType.SIMPLE, ...value } = this.data.value;
    const { expression, ...restConfig } = configuration as CalculatedFieldConfiguration;
    this.fieldFormGroup.patchValue({ configuration: { ...restConfig, ['expression'+type]: expression }, type, ...value }, {emitEvent: false});
  }

  private observeTypeChanges(): void {
    this.toggleKeyByCalculatedFieldType(this.fieldFormGroup.get('type').value);
    this.toggleScopeByOutputType(this.outputFormGroup.get('type').value);

    this.outputFormGroup.get('type').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(type => this.toggleScopeByOutputType(type));
    this.fieldFormGroup.get('type').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(type => this.toggleKeyByCalculatedFieldType(type));
  }

  private toggleScopeByOutputType(type: OutputType): void {
    this.outputFormGroup.get('scope')[type === OutputType.Attribute? 'enable' : 'disable']({emitEvent: false});
  }

  private toggleKeyByCalculatedFieldType(type: CalculatedFieldType): void {
    this.outputFormGroup.get('name')[type === CalculatedFieldType.SIMPLE? 'enable' : 'disable']({emitEvent: false});
    this.configFormGroup.get('expression'+CalculatedFieldType.SIMPLE)[type === CalculatedFieldType.SIMPLE? 'enable' : 'disable']({emitEvent: false});
    this.configFormGroup.get('expression'+CalculatedFieldType.SCRIPT)[type === CalculatedFieldType.SCRIPT? 'enable' : 'disable']({emitEvent: false});
  }
}
