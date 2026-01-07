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

import { booleanAttribute, Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable, of } from 'rxjs';
import {
  CalculatedFieldOutput,
  CalculatedFieldRelatedAggregationConfiguration,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  notEmptyObjectValidator,
  OutputType,
  PropagationDirectionTranslations
} from '@shared/models/calculated-field.models';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@app/shared/models/rule-node.models';
import { EntitySearchDirection } from '@shared/models/relation.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-related-entities-aggregation-component',
  templateUrl: './related-entities-aggregation-component.component.html',
  styleUrl: './related-entities-aggregation-component.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RelatedEntitiesAggregationComponentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RelatedEntitiesAggregationComponentComponent),
      multi: true
    }
  ],
})
export class RelatedEntitiesAggregationComponentComponent implements ControlValueAccessor, Validator {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  testScript: (expression?: string) => Observable<string>;

  @Input({transform: booleanAttribute}) isEditValue = true;

  readonly ScriptLanguage = ScriptLanguage;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly OutputType = OutputType;
  readonly Directions = Object.values(EntitySearchDirection) as Array<EntitySearchDirection>;
  readonly PropagationDirectionTranslations = PropagationDirectionTranslations;
  readonly minAllowedDeduplicationIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedDeduplicationIntervalInSecForCF;

  relatedAggregationConfiguration = this.fb.group({
    relation: this.fb.group({
      direction: [EntitySearchDirection.FROM, Validators.required],
      relationType: ['Contains', Validators.required],
    }),
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    metrics: this.fb.control({}, notEmptyObjectValidator()),
    deduplicationIntervalInSec: [this.minAllowedDeduplicationIntervalInSecForCF],
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput),
    useLatestTs: [false]
  });

  arguments$ = this.relatedAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => Object.keys(argumentsObj))
  );

  argumentsEditorCompleter$ = this.relatedAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj ?? {}))
  );

  argumentsHighlightRules$ = this.relatedAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
  );

  private readonly minAllowedScheduledUpdateIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedScheduledUpdateIntervalInSecForCF;
  private propagateChange: (config: CalculatedFieldRelatedAggregationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {

    this.relatedAggregationConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldRelatedAggregationConfiguration) => {
      this.updatedModel(value);
    })
  }

  validate(): ValidationErrors | null {
    return this.relatedAggregationConfiguration.valid || this.relatedAggregationConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: CalculatedFieldRelatedAggregationConfiguration): void {
    this.relatedAggregationConfiguration.patchValue(value, {emitEvent: false});
    setTimeout(() => {
      this.relatedAggregationConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: CalculatedFieldRelatedAggregationConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.relatedAggregationConfiguration.disable({emitEvent: false});
    } else {
      this.relatedAggregationConfiguration.enable({emitEvent: false});
    }
  }

  fetchOptions(searchText: string): Observable<Array<string>> {
    const search = searchText ? searchText?.toLowerCase() : '';
    return of(['Contains', 'Manages']).pipe(map(name => name?.filter(option => option.toLowerCase().includes(search))));
  }

  private updatedModel(value: CalculatedFieldRelatedAggregationConfiguration): void {
    value.type = CalculatedFieldType.RELATED_ENTITIES_AGGREGATION;
    value.scheduledUpdateInterval = this.minAllowedScheduledUpdateIntervalInSecForCF;
    this.propagateChange(value);
  }
}
