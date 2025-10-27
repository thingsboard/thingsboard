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

import { Component, Input, OnInit, output } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, FormControl, ValidatorFn, Validators } from '@angular/forms';
import { charsWithNumRegex } from '@shared/models/regex.constants';
import {
  AggFunction,
  AggFunctionTranslations,
  AggInputType,
  AggInputTypeTranslations,
  CalculatedFieldAggMetricValue
} from '@shared/models/calculated-field.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';

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
  @Input() arguments: Array<string>;
  @Input() editorCompleter: TbEditorCompleter;
  @Input() highlightRules: AceHighlightRules;

  metricDataApplied = output<CalculatedFieldAggMetricValue>();
  filterExpanded = false;
  functionArgs: Array<string>

  metricForm = this.fb.group({
    name: ['', [Validators.required, this.uniqNameRequired(), this.forbiddenNameValidator(), Validators.pattern(charsWithNumRegex), Validators.maxLength(255)]],
    function: [AggFunction.AVG],
    allowFilter: [false],
    filter: ['', Validators.required],
    input: this.fb.group({
      type: [AggInputType.key],
      key: ['', Validators.required],
      function: ['', Validators.required],
    })
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
    private popover: TbPopoverComponent<CalculatedFieldMetricsPanelComponent>
  ) {
    this.observeFilterAllowChange();
    this.observeInputTypeChange();
  }

  ngOnInit(): void {
    const data: CalculatedFieldAggMetricValuePanel = {
      ...this.metric,
      allowFilter: !!this.metric.filter,
    }
    this.metricForm.patchValue(data, {emitEvent: false});

    this.validateFilter(data.allowFilter);
    this.validateInputTypeFilter(data.input?.type ?? AggInputType.key);
    this.validateInputKey();

    this.functionArgs = ['ctx', ...this.arguments];
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
    if (this.metric.input?.type === AggInputType.key && !this.arguments.includes(this.metric.input.key)) {
      this.metricForm.get('input.key').setValue(null);
      this.metricForm.get('input.key').markAsTouched();
    }
  }

  private uniqNameRequired(): ValidatorFn {
    return (control: FormControl) => {
      const newName = control.value.trim().toLowerCase();
      const isDuplicate = this.usedNames?.some(name => name.toLowerCase() === newName);

      return isDuplicate ? { duplicateName: true } : null;
    };
  }

  private forbiddenNameValidator(): ValidatorFn {
    return (control: FormControl) => {
      const trimmedValue = control.value.trim().toLowerCase();
      const forbiddenNames = ['ctx', 'e', 'pi'];
      return forbiddenNames.includes(trimmedValue) ? { forbiddenName: true } : null;
    };
  }
}
