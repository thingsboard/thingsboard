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

import { Component, Input } from '@angular/core';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType
} from '@shared/models/query/query.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  alarmRuleBooleanOperationTranslationMap,
  AlarmRuleExpression,
  AlarmRuleExpressionType,
  AlarmRuleFilter,
  AlarmRuleFilterPredicate,
  AlarmRuleFilterPredicateType,
  alarmRuleNumericOperationTranslationMap,
  AlarmRuleStringOperation,
  alarmRuleStringOperationTranslationMap,
  ComplexAlarmRuleFilterPredicate
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { coerceBoolean } from "@shared/decorators/coercion";
import { timeUnitTranslationMap } from "@shared/models/time/time.models";

@Component({
    selector: 'tb-alarm-rule-filter-text',
    templateUrl: './alarm-rule-filter-text.component.html',
    styleUrls: ['./alarm-rule-filter-text.component.scss'],
    providers: [],
    standalone: false
})
export class AlarmRuleFilterTextComponent {

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  noFilterText = this.translate.instant('filter.no-filter-text');

  @Input()
  addFilterPrompt = this.translate.instant('filter.add-filter-prompt');

  @Input()
  @coerceBoolean()
  nowrap = false;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  @coerceBoolean()
  disabled = false;

  private alarmRuleExpressionValue: AlarmRuleExpression;
  get alarmRuleExpression(): AlarmRuleExpression {
    return this.alarmRuleExpressionValue;
  }

  @Input()
  set alarmRuleExpression(value: AlarmRuleExpression) {
    if (value !== this.alarmRuleExpressionValue) {
      this.alarmRuleExpressionValue = value;
      this.updateFilterText(value);
    }
  };

  private specTextValue: string;
  get specText(): string {
    return this.specTextValue;
  }
  @Input()
  set specText(value: string) {
    if (value !== this.specTextValue) {
      this.specTextValue = value;
      this.updateFilterText(this.alarmRuleExpression);
    }
  }

  isRequired = false;

  public filterText: string;

  constructor(private translate: TranslateService,
              private datePipe: DatePipe) {
  }

  private updateFilterText(value: AlarmRuleExpression) {
    this.isRequired = false;
    if (value && (value.expression || value.filters?.length)) {
      if (value.type === AlarmRuleExpressionType.SIMPLE) {
        this.filterText = this.keyFiltersToText(this.translate, this.datePipe, value.filters, value.operation);
      } else {
        this.filterText = 'function expression(ctx, ' + (this.arguments ? Object.keys(this.arguments).join(', ') : '' ) + ')';
      }
      if (this.specText?.length) {
        this.filterText = this.specText + ': ' + this.filterText;
      }
    } else {
      if (this.required) {
        this.filterText = this.addFilterPrompt;
        this.isRequired = true;
      } else {
        this.filterText = this.noFilterText;
      }
    }
  }

  private keyFiltersToText(translate: TranslateService, datePipe: DatePipe, keyFilters: Array<AlarmRuleFilter>, operation: ComplexOperation): string {
    const filtersText = keyFilters.map(keyFilter =>
      this.filterPredicateToText(translate, datePipe, keyFilter, keyFilter.predicates));
    let result: string;
    if (filtersText.length > 1) {
      const operationText = translate.instant(complexOperationTranslationMap.get(operation));
      result = filtersText.join(' <span class="tb-filter-complex-operation">' + operationText + '</span> ');
    } else {
      result = filtersText[0];
    }
    return result;
  }

  private filterPredicateToText(translate: TranslateService,
                                datePipe: DatePipe,
                                keyFilter: AlarmRuleFilter,
                                keyFilterPredicates: AlarmRuleFilterPredicate[],
                                complexOperation?: ComplexOperation): string {
    const key = keyFilter.argument;
    const filterOperation: ComplexOperation = complexOperation ? complexOperation : (keyFilter.operation ?? ComplexOperation.AND);

    const predicates = keyFilterPredicates.map((keyFilterPredicate: AlarmRuleFilterPredicate) => {
      if (keyFilterPredicate.type === AlarmRuleFilterPredicateType.COMPLEX) {
        const complexPredicate = keyFilterPredicate as ComplexAlarmRuleFilterPredicate;
        const complexOperation = complexPredicate.operation ?? ComplexOperation.AND;
        return this.filterPredicateToText(translate, datePipe, keyFilter, complexPredicate.predicates, complexOperation);
      } else {
        let operation: string;
        let value: string;
        const val = keyFilterPredicate.type === AlarmRuleFilterPredicateType.NO_DATA ? keyFilterPredicate.duration : keyFilterPredicate.value;
        const dynamicValue = val?.dynamicValueArgument?.length;
        if (dynamicValue) {
          value = '<span class="tb-filter-dynamic-value"><span class="tb-filter-value">' + val?.dynamicValueArgument + '</span></span>';
        }
        switch (keyFilterPredicate.type) {
          case AlarmRuleFilterPredicateType.STRING:
            operation = translate.instant(alarmRuleStringOperationTranslationMap.get(keyFilterPredicate.operation));
            if (keyFilterPredicate.ignoreCase) {
              operation += ' ' + translate.instant('filter.ignore-case');
            }
            if (!dynamicValue) {
              value = `'${keyFilterPredicate.value.staticValue}'`;
            }
            break;
          case AlarmRuleFilterPredicateType.NUMERIC:
            operation = translate.instant(alarmRuleNumericOperationTranslationMap.get(keyFilterPredicate.operation));
            if (!dynamicValue) {
              if (keyFilter.valueType === EntityKeyValueType.DATE_TIME) {
                value = datePipe.transform(keyFilterPredicate.value.staticValue, 'yyyy-MM-dd HH:mm');
              } else {
                value = keyFilterPredicate.value.staticValue + '';
              }
            }
            break;
          case AlarmRuleFilterPredicateType.BOOLEAN:
            operation = translate.instant(alarmRuleBooleanOperationTranslationMap.get(keyFilterPredicate.operation));
            if (!dynamicValue) {
              value = translate.instant(keyFilterPredicate.value.staticValue ? 'value.true' : 'value.false');
            }
            break;
          case AlarmRuleFilterPredicateType.NO_DATA:
            operation = translate.instant(alarmRuleStringOperationTranslationMap.get(AlarmRuleStringOperation.NO_DATA));
            if (!dynamicValue) {
              value = keyFilterPredicate.duration.staticValue + ' ' + translate.instant(timeUnitTranslationMap.get(keyFilterPredicate.unit)).toLowerCase();
            }
            break;
        }
        if (!dynamicValue) {
          value = `<span class="tb-filter-value">${value}</span>`;
        }
        return `<span class="tb-filter-predicate"><span class="tb-filter-entity-key">${key}</span> <span class="tb-filter-simple-operation">${operation}</span> ${value}</span>`
      }
    });
    if (predicates.length > 1) {
      return '(' + predicates.join(` ${translate.instant(complexOperationTranslationMap.get(filterOperation))} `)+ ')';
    } else {
      return predicates.toString();
    }
  }

}
