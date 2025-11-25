///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, DestroyRef, Input, OnInit, output } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { charsWithNumRegex } from '@shared/models/regex.constants';
import {
  AggFunction,
  AggFunctionTranslations,
  AggInputType,
  AggInputTypeTranslations,
  ArgumentType,
  CalculatedFieldAggMetricValue,
  CalculatedFieldArgument,
  CalculatedFieldEventArguments,
  FORBIDDEN_NAMES,
  forbiddenNamesValidator,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  uniqueNameValidator
} from '@shared/models/calculated-field.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';
import { MatDialog } from "@angular/material/dialog";
import { Observable } from "rxjs";
import { isObject } from "@core/utils";
import {
  CalculatedFieldScriptTestDialogComponent,
  CalculatedFieldTestScriptDialogData
} from "@home/components/calculated-fields/components/test-dialog/calculated-field-script-test-dialog.component";
import { filter, switchMap, tap } from "rxjs/operators";
import { CalculatedFieldsService } from "@core/http/calculated-fields.service";

interface CalculatedFieldAggMetricValuePanel extends CalculatedFieldAggMetricValue {
  allowFilter: boolean;
}

@Component({
  selector: 'tb-calculated-field-metrics-panel',
  templateUrl: './calculated-field-metrics-panel.component.html',
  styleUrl: '../common/calculated-field-panel.scss',
})
export class CalculatedFieldMetricsPanelComponent implements OnInit {

  @Input() buttonTitle: string;
  @Input() metric: CalculatedFieldAggMetricValue;
  @Input() usedNames: string[];
  @Input() arguments: Record<string, CalculatedFieldArgument>;
  @Input() simpleMode: boolean;
  @Input() editorCompleter: TbEditorCompleter;
  @Input() highlightRules: AceHighlightRules;
  @Input() calculatedFieldId: string;

  metricDataApplied = output<CalculatedFieldAggMetricValue>();
  filterExpanded = false;
  argumentsList: Array<string>
  functionArgs: Array<string>

  metricForm = this.fb.group({
    name: ['', [Validators.required, forbiddenNamesValidator(FORBIDDEN_NAMES), Validators.pattern(charsWithNumRegex), Validators.maxLength(255)]],
    function: [AggFunction.AVG],
    allowFilter: [false],
    filter: ['', Validators.required],
    input: this.fb.group({
      type: [AggInputType.key],
      key: ['', Validators.required],
      function: ['', Validators.required],
    }),
    defaultValue: [null]
  });

  entityFilter: EntityFilter;

  readonly AggFunctions = Object.values(AggFunction) as AggFunction[];
  readonly AggFunctionTranslations = AggFunctionTranslations;
  readonly ScriptLanguage = ScriptLanguage;
  readonly AggInputType = AggInputType;
  readonly AggInputTypes = Object.values(AggInputType) as AggInputType[];
  readonly AggInputTypeTranslations = AggInputTypeTranslations;

  constructor(
    private fb: FormBuilder,
    private popover: TbPopoverComponent<CalculatedFieldMetricsPanelComponent>,
    private dialog: MatDialog,
    private calculatedFieldsService: CalculatedFieldsService,
    private destroyRef: DestroyRef
  ) {
    this.observeFilterAllowChange();
    this.observeInputTypeChange();
  }

  ngOnInit(): void {
    this.updatedForm();

    const data: CalculatedFieldAggMetricValuePanel = {
      ...this.metric,
      allowFilter: !!this.metric.filter,
    }
    this.metricForm.patchValue(data, {emitEvent: false});

    this.validateFilter(data.allowFilter);
    this.validateInputTypeFilter(data.input?.type ?? AggInputType.key);
    this.validateInputKey();

    this.argumentsList = Object.keys(this.arguments);
    this.functionArgs = ['ctx', ...this.argumentsList];
  }

  saveMetric(): void {
    const value = this.metricForm.value as CalculatedFieldAggMetricValuePanel;
    if (!value.allowFilter) {
      delete value.filter;
    }
    delete value.allowFilter;
    this.metricDataApplied.emit(value);
  }

  cancel(): void {
    this.popover.hide();
  }

  private updatedForm(): void {
    this.metricForm.get('name').addValidators(uniqueNameValidator(this.usedNames));
    this.metricForm.get('name').updateValueAndValidity({emitEvent: false});

    if (!this.simpleMode) {
      this.metricForm.removeControl('defaultValue', {emitEvent: false});
    }
  }

  private observeFilterAllowChange(): void {
    this.metricForm.get('allowFilter').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.validateFilter(value));
  }

  private observeInputTypeChange(): void {
    this.metricForm.get('input.type').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.validateInputTypeFilter(value));
  }

  private validateFilter(allowFilter = false): void {
    if (allowFilter) {
      this.metricForm.get('filter').enable({emitEvent: false});
    } else {
      this.metricForm.get('filter').disable({emitEvent: false});
    }
    this.filterExpanded = allowFilter;
  }

  private validateInputTypeFilter(value: AggInputType): void {
    const inputForm = this.metricForm.get('input');
    if (value === AggInputType.key) {
      inputForm.get('key').enable({emitEvent: false});
      inputForm.get('function').disable({emitEvent: false});
    } else {
      inputForm.get('key').disable({emitEvent: false});
      inputForm.get('function').enable({emitEvent: false});
    }
  }

  private validateInputKey() {
    if (this.metric.input?.type === AggInputType.key && !Object.keys(this.arguments).includes(this.metric.input.key)) {
      this.metricForm.get('input.key').setValue(null);
      this.metricForm.get('input.key').markAsTouched();
    }
  }

  onTestScript(scriptFunc: 'filter' | 'input.function') {
    this.testScript(scriptFunc).subscribe(expression => {
      this.metricForm.get(scriptFunc).setValue(expression);
      this.metricForm.get(scriptFunc).markAsDirty();
    });
  }

  testScript(scriptFunc: 'filter' | 'input.function'): Observable<string> {
    if (this.calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(this.calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return this.getTestScriptDialog(this.arguments, this.metricForm.get(scriptFunc).value, args);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
    }
    return this.getTestScriptDialog(this.arguments, this.metricForm.get(scriptFunc).value, null);
  }

  getTestScriptDialog(argumentsList: Record<string, CalculatedFieldArgument>, expression: string, argumentsObj?: CalculatedFieldEventArguments): Observable<string> {
    const resultArguments = Object.keys(argumentsList).reduce((acc, key) => {
      const type = argumentsList[key].refEntityKey.type;
      acc[key] = isObject(argumentsObj) && argumentsObj.hasOwnProperty(key)
        ? {...argumentsObj[key], type}
        : type === ArgumentType.Rolling ? {values: [], type} : {value: '', type, ts: new Date().getTime()};
      return acc;
    }, {});
    return this.dialog.open<CalculatedFieldScriptTestDialogComponent, CalculatedFieldTestScriptDialogData, string>(CalculatedFieldScriptTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          arguments: resultArguments,
          expression: expression,
          argumentsEditorCompleter: getCalculatedFieldArgumentsEditorCompleter(argumentsList),
          argumentsHighlightRules: getCalculatedFieldArgumentsHighlights(argumentsList)
        }
      }).afterClosed()
      .pipe(
        filter(Boolean),
        tap(expression => expression)
      );
  }
}
